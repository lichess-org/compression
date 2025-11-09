package org.lichess.compression.game

final class Move(
    var `type`: Int = 0,
    var role: Role = null,
    var from: Int = 0,
    var capture: Boolean = false,
    var to: Int = 0,
    var promotion: Role = null,
    var score: Int = 0
) extends Ordered[Move]:

  def set(
      board: Board,
      `type`: Int,
      role: Role,
      from: Int,
      capture: Boolean,
      to: Int,
      promotion: Role
  ): Unit =
    this.`type` = `type`
    this.role = role
    this.from = from
    this.capture = capture
    this.to = to
    this.promotion = promotion
    val defendingPawns = Bitboard.pawnAttacks(board.turn, to) & board.pawns & board.them()
    val moveValue      = pieceValue(board, role, to) - pieceValue(board, role, from)

    this.score = (if promotion == null then 0 else promotion.index << 26) +
      (if capture then 1 << 25 else 0) +
      ((if defendingPawns == 0 then 6 else 5 - role.index) << 22) +
      (512 + moveValue << 12) +
      (to << 6) +
      from

  override def compare(that: Move): Int = Integer.compare(that.score, score)

  def uci(): String =
    val toSquare =
      if `type` == Move.CASTLING then Square.combine(if to < from then Square.C1 else Square.G1, from) else to
    val builder = StringBuilder(if this.promotion == null then 4 else 5)
    builder.append((Square.file(from) + 'a').toChar)
    builder.append((Square.rank(from) + '1').toChar)
    builder.append((Square.file(toSquare) + 'a').toChar)
    builder.append((Square.rank(toSquare) + '1').toChar)
    if this.promotion != null then builder.append(this.promotion.symbol.toLowerCase)
    builder.toString

  def isZeroing: Boolean = capture || role == Role.PAWN

  def isIrreversible: Boolean = isZeroing || `type` == Move.CASTLING

  private def pieceValue(board: Board, role: Role, square: Int): Int =
    Move.PSQT(role.index)(if board.turn then Square.mirror(square) else square)

object Move:
  val NORMAL     = 0
  val EN_PASSANT = 1
  val CASTLING   = 2

  private val PSQT: Array[Array[Int]] = Array(
    Array(0, 0, 0, 0, 0, 0, 0, 0, 50, 50, 50, 50, 50, 50, 50, 50, 10, 10, 20, 30, 30, 20, 10, 10, 5, 5, 10,
      25, 25, 10, 5, 5, 0, 0, 0, 20, 21, 0, 0, 0, 5, -5, -10, 0, 0, -10, -5, 5, 5, 10, 10, -31, -31, 10, 10,
      5, 0, 0, 0, 0, 0, 0, 0, 0),
    Array(-50, -40, -30, -30, -30, -30, -40, -50, -40, -20, 0, 0, 0, 0, -20, -40, -30, 0, 10, 15, 15, 10, 0,
      -30, -30, 5, 15, 20, 20, 15, 5, -30, -30, 0, 15, 20, 20, 15, 0, -30, -30, 5, 10, 15, 15, 11, 5, -30,
      -40, -20, 0, 5, 5, 0, -20, -40, -50, -40, -30, -30, -30, -30, -40, -50),
    Array(-20, -10, -10, -10, -10, -10, -10, -20, -10, 0, 0, 0, 0, 0, 0, -10, -10, 0, 5, 10, 10, 5, 0, -10,
      -10, 5, 5, 10, 10, 5, 5, -10, -10, 0, 10, 10, 10, 10, 0, -10, -10, 10, 10, 10, 10, 10, 10, -10, -10, 5,
      0, 0, 0, 0, 5, -10, -20, -10, -10, -10, -10, -10, -10, -20),
    Array(0, 0, 0, 0, 0, 0, 0, 0, 5, 10, 10, 10, 10, 10, 10, 5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0,
      0, -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, 0, 0, 0, 5, 5, 0,
      0, 0),
    Array(-20, -10, -10, -5, -5, -10, -10, -20, -10, 0, 0, 0, 0, 0, 0, -10, -10, 0, 5, 5, 5, 5, 0, -10, -5, 0,
      5, 5, 5, 5, 0, -5, 0, 0, 5, 5, 5, 5, 0, -5, -10, 5, 5, 5, 5, 5, 0, -10, -10, 0, 5, 0, 0, 0, 0, -10, -20,
      -10, -10, -5, -5, -10, -10, -20),
    Array(-30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50,
      -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30, -20, -30, -30, -40, -40, -30, -30, -20, -10,
      -20, -20, -20, -20, -20, -20, -10, 20, 20, 0, 0, 0, 0, 20, 20, 0, 30, 10, 0, 0, 10, 30, 0)
  )
