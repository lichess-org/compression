package org.lichess.compression.game

import scala.collection.mutable.HashMap
import scala.util.control.Breaks.{ break, breakable }

final class Board(
    var pawns: Long = 0xff00000000ff00L,
    var knights: Long = 0x4200000000000042L,
    var bishops: Long = 0x2400000000000024L,
    var rooks: Long = 0x8100000000000081L,
    var queens: Long = 0x800000000000008L,
    var kings: Long = 0x1000000000000010L,
    var white: Long = 0xffffL,
    var black: Long = 0xffff000000000000L,
    var turn: Boolean = true,
    var epSquare: Int = 0,
    var castlingRights: Long = 0x8100000000000081L
):
  var occupied             = white | black
  var incrementalHash: Int = ZobristHash.hashPieces(this) ^ ZobristHash.hashTurn(this)

  def copy(): Board =
    val board =
      Board(pawns, knights, bishops, rooks, queens, kings, white, black, turn, epSquare, castlingRights)
    board.occupied = occupied
    board.incrementalHash = incrementalHash
    board

  private def isOccupied(square: Int): Boolean =
    Bitboard.contains(this.occupied, square)

  private def discard(square: Int): Unit =
    if isOccupied(square) then
      val mask = 1L << square
      val role = roleAt(square)
      role match
        case Role.PAWN   => this.pawns ^= mask
        case Role.KNIGHT => this.knights ^= mask
        case Role.BISHOP => this.bishops ^= mask
        case Role.ROOK   => this.rooks ^= mask
        case Role.QUEEN  => this.queens ^= mask
        case Role.KING   => this.kings ^= mask
      val color = whiteAt(square)
      if color then this.white ^= mask else this.black ^= mask

      this.occupied ^= mask
      this.incrementalHash ^= ZobristHash.hashPiece(square, color, role)

  private def put(square: Int, color: Boolean, role: Role): Unit =
    discard(square)
    val mask = 1L << square
    role match
      case Role.PAWN   => this.pawns ^= mask
      case Role.KNIGHT => this.knights ^= mask
      case Role.BISHOP => this.bishops ^= mask
      case Role.ROOK   => this.rooks ^= mask
      case Role.QUEEN  => this.queens ^= mask
      case Role.KING   => this.kings ^= mask

    if color then this.white ^= mask else this.black ^= mask

    this.occupied ^= mask
    this.incrementalHash ^= ZobristHash.hashPiece(square, color, role)

  def roleAt(square: Int): Role =
    if Bitboard.contains(this.pawns, square) then Role.PAWN
    else if Bitboard.contains(this.knights, square) then Role.KNIGHT
    else if Bitboard.contains(this.bishops, square) then Role.BISHOP
    else if Bitboard.contains(this.rooks, square) then Role.ROOK
    else if Bitboard.contains(this.queens, square) then Role.QUEEN
    else if Bitboard.contains(this.kings, square) then Role.KING
    else null

  def whiteAt(square: Int): Boolean =
    Bitboard.contains(this.white, square)

  def zobristHash(): Int =
    this.incrementalHash ^ ZobristHash.hashCastling(this) ^ ZobristHash.hashEnPassant(this)

  def pieceMap(): Map[Int, Piece] =
    val map      = HashMap[Int, Piece]()
    var occupied = this.occupied
    while occupied != 0 do
      val sq = Bitboard.lsb(occupied)
      map.put(sq, Piece(whiteAt(sq), roleAt(sq)))
      occupied &= occupied - 1L
    map.toMap

  def play(move: Move): Unit =
    this.epSquare = 0
    move.`type` match
      case Move.NORMAL =>
        if move.role == Role.PAWN && Math.abs(move.from - move.to) == 16 then
          val theirPawns = them() & this.pawns
          if theirPawns != 0 then
            val sq = move.from + (if this.turn then 8 else -8)
            if (Bitboard.pawnAttacks(this.turn, sq) & theirPawns) != 0 then this.epSquare = sq

        if this.castlingRights != 0 then
          if move.role == Role.KING then this.castlingRights &= Bitboard.RANKS(if this.turn then 7 else 0)
          else if move.role == Role.ROOK then this.castlingRights &= ~(1L << move.from)

          if move.capture then this.castlingRights &= ~(1L << move.to)

        discard(move.from)
        put(move.to, this.turn, if move.promotion != null then move.promotion else move.role)

      case Move.CASTLING =>
        this.castlingRights &= Bitboard.RANKS(if this.turn then 7 else 0)
        val rookTo = Square.combine(if move.to < move.from then Square.D1 else Square.F1, move.to)
        val kingTo = Square.combine(if move.to < move.from then Square.C1 else Square.G1, move.from)
        discard(move.from)
        discard(move.to)
        put(rookTo, this.turn, Role.ROOK)
        put(kingTo, this.turn, Role.KING)

      case Move.EN_PASSANT =>
        discard(Square.combine(move.to, move.from))
        discard(move.from)
        put(move.to, this.turn, Role.PAWN)

    this.turn = !this.turn
    this.incrementalHash ^= ZobristHash.POLYGLOT(780)

  def us(): Long =
    byColor(this.turn)

  def them(): Long =
    byColor(!this.turn)

  def byColor(white: Boolean): Long =
    if white then this.white else this.black

  private def getKing(white: Boolean): Int =
    Bitboard.lsb(this.kings & byColor(white))

  private def sliderBlockers(king: Int): Long =
    val snipers = them() & (
      Bitboard.rookAttacks(king, 0) & (this.rooks ^ this.queens) |
        Bitboard.bishopAttacks(king, 0) & (this.bishops ^ this.queens)
    )

    var blockers         = 0L
    var remainingSnipers = snipers
    while remainingSnipers != 0 do
      val sniper  = Bitboard.lsb(remainingSnipers)
      val between = Bitboard.BETWEEN(king)(sniper) & this.occupied
      if !Bitboard.moreThanOne(between) then blockers |= between
      remainingSnipers &= remainingSnipers - 1L
    blockers

  def isCheck(): Boolean =
    attacksTo(getKing(this.turn), !this.turn) != 0

  private def attacksTo(sq: Int, attacker: Boolean): Long =
    attacksTo(sq, attacker, this.occupied)

  private def attacksTo(sq: Int, attacker: Boolean, occupied: Long): Long =
    byColor(attacker) & (
      Bitboard.rookAttacks(sq, occupied) & (this.rooks ^ this.queens) |
        Bitboard.bishopAttacks(sq, occupied) & (this.bishops ^ this.queens) |
        Bitboard.KNIGHT_ATTACKS(sq) & this.knights |
        Bitboard.KING_ATTACKS(sq) & this.kings |
        Bitboard.pawnAttacks(!attacker, sq) & this.pawns
    )

  def legalMoves(moves: MoveList): Unit =
    moves.clear()
    if this.epSquare != 0 then genEnPassant(moves)

    val king     = getKing(this.turn)
    val checkers = attacksTo(king, !this.turn)
    if checkers == 0 then
      val target = ~us()
      genNonKing(target, moves)
      genSafeKing(king, target, moves)
      genCastling(king, moves)
    else genEvasions(king, checkers, moves)

    val blockers = sliderBlockers(king)

    if blockers != 0 || this.epSquare != 0 then moves.retain(m => isSafe(king, m, blockers))

  def hasLegalEnPassant(): Boolean =
    if this.epSquare == 0 then false
    else
      val moves = MoveList(2)
      genEnPassant(moves)

      val king     = getKing(this.turn)
      val blockers = sliderBlockers(king)
      moves.anyMatch(m => isSafe(king, m, blockers))

  private def genNonKing(mask: Long, moves: MoveList): Unit =
    genPawn(mask, moves)

    var knights = us() & this.knights
    while knights != 0 do
      val from    = Bitboard.lsb(knights)
      var targets = Bitboard.KNIGHT_ATTACKS(from) & mask
      while targets != 0 do
        val to = Bitboard.lsb(targets)
        moves.pushNormal(this, Role.KNIGHT, from, isOccupied(to), to)
        targets &= targets - 1L
      knights &= knights - 1L

    var bishops = us() & this.bishops
    while bishops != 0 do
      val from    = Bitboard.lsb(bishops)
      var targets = Bitboard.bishopAttacks(from, this.occupied) & mask
      while targets != 0 do
        val to = Bitboard.lsb(targets)
        moves.pushNormal(this, Role.BISHOP, from, isOccupied(to), to)
        targets &= targets - 1L
      bishops &= bishops - 1L

    var rooks = us() & this.rooks
    while rooks != 0 do
      val from    = Bitboard.lsb(rooks)
      var targets = Bitboard.rookAttacks(from, this.occupied) & mask
      while targets != 0 do
        val to = Bitboard.lsb(targets)
        moves.pushNormal(this, Role.ROOK, from, isOccupied(to), to)
        targets &= targets - 1L
      rooks &= rooks - 1L

    var queens = us() & this.queens
    while queens != 0 do
      val from    = Bitboard.lsb(queens)
      var targets = Bitboard.queenAttacks(from, this.occupied) & mask
      while targets != 0 do
        val to = Bitboard.lsb(targets)
        moves.pushNormal(this, Role.QUEEN, from, isOccupied(to), to)
        targets &= targets - 1L
      queens &= queens - 1L

  private def genSafeKing(king: Int, mask: Long, moves: MoveList): Unit =
    var targets = Bitboard.KING_ATTACKS(king) & mask
    while targets != 0 do
      val to = Bitboard.lsb(targets)
      if attacksTo(to, !this.turn) == 0 then moves.pushNormal(this, Role.KING, king, isOccupied(to), to)
      targets &= targets - 1L

  private def genEvasions(king: Int, checkers: Long, moves: MoveList): Unit =
    var sliders  = checkers & (this.bishops ^ this.rooks ^ this.queens)
    var attacked = 0L
    while sliders != 0 do
      val slider = Bitboard.lsb(sliders)
      attacked |= Bitboard.RAYS(king)(slider) ^ (1L << slider)
      sliders &= sliders - 1L

    genSafeKing(king, ~us() & ~attacked, moves)

    if checkers != 0 && !Bitboard.moreThanOne(checkers) then
      val checker = Bitboard.lsb(checkers)
      val target  = Bitboard.BETWEEN(king)(checker) | checkers
      genNonKing(target, moves)

  private def genPawn(mask: Long, moves: MoveList): Unit =
    var capturers = us() & this.pawns
    while capturers != 0 do
      val from    = Bitboard.lsb(capturers)
      var targets = Bitboard.pawnAttacks(this.turn, from) & them() & mask
      while targets != 0 do
        val to = Bitboard.lsb(targets)
        addPawnMoves(from, true, to, moves)
        targets &= targets - 1L
      capturers &= capturers - 1L

    var singleMoves =
      ~this.occupied & (if this.turn then (this.white & this.pawns) << 8 else (this.black & this.pawns) >>> 8)
    var doubleMoves =
      ~this.occupied & (if this.turn then singleMoves << 8 else singleMoves >>> 8) & Bitboard.RANKS(
        if this.turn then 3 else 4
      )

    singleMoves &= mask
    doubleMoves &= mask

    while singleMoves != 0 do
      val to   = Bitboard.lsb(singleMoves)
      val from = to + (if this.turn then -8 else 8)
      addPawnMoves(from, false, to, moves)
      singleMoves &= singleMoves - 1L

    while doubleMoves != 0 do
      val to   = Bitboard.lsb(doubleMoves)
      val from = to + (if this.turn then -16 else 16)
      moves.pushNormal(this, Role.PAWN, from, false, to)
      doubleMoves &= doubleMoves - 1L

  private def addPawnMoves(from: Int, capture: Boolean, to: Int, moves: MoveList): Unit =
    if Square.rank(to) == (if this.turn then 7 else 0) then
      moves.pushPromotion(this, from, capture, to, Role.QUEEN)
      moves.pushPromotion(this, from, capture, to, Role.KNIGHT)
      moves.pushPromotion(this, from, capture, to, Role.ROOK)
      moves.pushPromotion(this, from, capture, to, Role.BISHOP)
    else moves.pushNormal(this, Role.PAWN, from, capture, to)

  private def genEnPassant(moves: MoveList): Unit =
    var pawns = us() & this.pawns & Bitboard.pawnAttacks(!this.turn, this.epSquare)
    while pawns != 0 do
      val pawn = Bitboard.lsb(pawns)
      moves.pushEnPassant(this, pawn, this.epSquare)
      pawns &= pawns - 1L

  private def genCastling(king: Int, moves: MoveList): Unit =
    var rooks = this.castlingRights & Bitboard.RANKS(if this.turn then 0 else 7)
    while rooks != 0 do
      val rook = Bitboard.lsb(rooks)
      val path = Bitboard.BETWEEN(king)(rook)
      if (path & this.occupied) == 0 then
        val kingTo   = Square.combine(if rook < king then Square.C1 else Square.G1, king)
        var kingPath = Bitboard.BETWEEN(king)(kingTo) | (1L << kingTo) | (1L << king)
        breakable:
          while kingPath != 0 do
            val sq = Bitboard.lsb(kingPath)
            if attacksTo(sq, !this.turn, this.occupied ^ (1L << king)) != 0 then break
            kingPath &= kingPath - 1L
          if kingPath == 0 then moves.pushCastle(this, king, rook)
      rooks &= rooks - 1L

  private def isSafe(king: Int, move: Move, blockers: Long): Boolean =
    move.`type` match
      case Move.NORMAL =>
        !Bitboard.contains(us() & blockers, move.from) || Square.aligned(move.from, move.to, king)
      case Move.EN_PASSANT =>
        var occupied = this.occupied
        occupied ^= (1L << move.from)
        occupied ^= (1L << Square.combine(move.to, move.from))
        occupied |= (1L << move.to)
        (Bitboard.rookAttacks(king, occupied) & them() & (this.rooks ^ this.queens)) == 0 &&
        (Bitboard.bishopAttacks(king, occupied) & them() & (this.bishops ^ this.queens)) == 0
      case _ => true
