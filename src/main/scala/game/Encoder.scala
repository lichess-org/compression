package org.lichess.compression.game

import org.lichess.compression.BitOps

object Encoder:

  private val moveList = new ThreadLocal[MoveList]:
    override def initialValue(): MoveList = MoveList()

  private val SAN_RE =
    "([NBKRQ])?([a-h])?([1-8])?x?([a-h][1-8])(?:=([NBRQK]))?[\\+#]?".r

  private def charToRole(c: Char): Role = c match
    case 'N' => Role.KNIGHT
    case 'B' => Role.BISHOP
    case 'R' => Role.ROOK
    case 'Q' => Role.QUEEN
    case 'K' => Role.KING
    case _   => throw IllegalArgumentException()

  def encode(pgnMoves: Array[String]): Array[Byte] =
    val writer = BitOps.Writer()
    val board  = Board()
    val legals = moveList.get()

    for pgnMove <- pgnMoves do
      var role: Role      = null
      var promotion: Role = null
      var from: Long      = Bitboard.ALL
      var to: Int         = 0

      if pgnMove.startsWith("O-O-O") then
        role = Role.KING
        from = board.kings
        to = Bitboard.lsb(board.rooks & Bitboard.RANKS(if (board.turn) 0 else 7))
      else if pgnMove.startsWith("O-O") then
        role = Role.KING
        from = board.kings
        to = Bitboard.msb(board.rooks & Bitboard.RANKS(if (board.turn) 0 else 7))
      else
        SAN_RE.findFirstMatchIn(pgnMove) match
          case None =>
            return null
          case Some(matcher) =>
            val roleStr = matcher.group(1)
            role = if (roleStr == null) Role.PAWN else charToRole(roleStr.charAt(0))
            if matcher.group(2) != null then from &= Bitboard.FILES(matcher.group(2).charAt(0) - 'a')
            if matcher.group(3) != null then from &= Bitboard.RANKS(matcher.group(3).charAt(0) - '1')

            to = Square.square(matcher.group(4).charAt(0) - 'a', matcher.group(4).charAt(1) - '1')

            if matcher.group(5) != null then promotion = charToRole(matcher.group(5).charAt(0))

      board.legalMoves(legals)
      legals.sort()
      var foundMatch = false
      for i <- 0 until legals.getSize() do
        val legal = legals.get(i)
        if legal.role == role && legal.to == to && legal.promotion == promotion && Bitboard.contains(
            from,
            legal.from
          )
        then
          if foundMatch then return null
          Huffman.write(i, writer)
          board.play(legal)
          foundMatch = true

      if !foundMatch then return null

    writer.toArray()
  end encode

  case class DecodeResult(
      pgnMoves: Array[String],
      board: Board,
      halfMoveClock: Int,
      positionHashes: Array[Byte],
      lastUci: String
  )

  def decode(input: Array[Byte], plies: Int): DecodeResult =
    val reader              = BitOps.Reader(input)
    val output              = Array.ofDim[String](plies)
    val board               = Board()
    val legals              = moveList.get()
    var lastUci: String     = null
    var lastZeroingPly      = -1
    var lastIrreversiblePly = -1
    val positionHashes      = Array.ofDim[Byte](3 * (plies + 1))
    setHash(positionHashes, -1, board.zobristHash())

    for i <- 0 to plies do
      if 0 < i || i < plies then board.legalMoves(legals)

      if 0 < i then if board.isCheck() then output(i - 1) += (if legals.isEmpty then "#" else "+")

      if i < plies then
        val moveIndex = Huffman.read(reader)
        legals.partialSort(moveIndex + 1)
        val move = legals.get(moveIndex)
        output(i) = san(move, legals)
        board.play(move)

        if move.isZeroing then lastZeroingPly = i
        if move.isIrreversible then lastIrreversiblePly = i
        setHash(positionHashes, i, board.zobristHash())

        if i + 1 == plies then lastUci = move.uci()
    end for

    DecodeResult(
      output,
      board,
      plies - 1 - lastZeroingPly,
      positionHashes.slice(0, 3 * (plies - lastIrreversiblePly)),
      lastUci
    )
  end decode

  private def san(move: Move, legals: MoveList): String = move.`type` match
    case Move.NORMAL | Move.EN_PASSANT =>
      val builder = StringBuilder(6)
      builder.append(move.role.symbol)

      if move.role != Role.PAWN then
        var file   = false
        var rank   = false
        var others = 0L

        for i <- 0 until legals.getSize() do
          val other = legals.get(i)
          if other.role == move.role && other.to == move.to && other.from != move.from then
            others |= 1L << other.from

        if others != 0 then
          if (others & Bitboard.RANKS(Square.rank(move.from))) != 0 then file = true
          if (others & Bitboard.FILES(Square.file(move.from))) != 0 then rank = true
          else file = true

        if file then builder.append((Square.file(move.from) + 'a').toChar)
        if rank then builder.append((Square.rank(move.from) + '1').toChar)
      else if move.capture then builder.append((Square.file(move.from) + 'a').toChar)

      if move.capture then builder.append('x')

      builder.append((Square.file(move.to) + 'a').toChar)
      builder.append((Square.rank(move.to) + '1').toChar)
      if move.promotion != null then builder.append(s"=${move.promotion.symbol}")

      builder.toString()

    case Move.CASTLING =>
      if move.from < move.to then "O-O" else "O-O-O"

    case _ => "--"
  end san

  private def setHash(buffer: Array[Byte], ply: Int, hash: Int): Unit =
    val base = buffer.length - 3 * (ply + 1 + 1)
    buffer(base) = (hash >>> 16).toByte
    buffer(base + 1) = (hash >>> 8).toByte
    buffer(base + 2) = hash.toByte
end Encoder
