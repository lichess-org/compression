package org.lichess.compression.game

final class MoveList(capacity: Int = 256):
  private val buffer     = Array.tabulate(capacity)(_ => Move())
  private val comparator = java.util.Comparator.comparingInt[Move](_.score).reversed()

  private var size = 0

  def clear(): Unit = size = 0

  def getSize(): Int = size

  def get(i: Int): Move =
    require(i < size)
    buffer(i)

  def isEmpty: Boolean = size == 0

  def pushNormal(board: Board, role: Role, from: Int, capture: Boolean, to: Int): Unit =
    buffer(size).set(board, Move.NORMAL, role, from, capture, to, null)
    size += 1

  def pushPromotion(board: Board, from: Int, capture: Boolean, to: Int, promotion: Role): Unit =
    buffer(size).set(board, Move.NORMAL, Role.PAWN, from, capture, to, promotion)
    size += 1

  def pushCastle(board: Board, king: Int, rook: Int): Unit =
    buffer(size).set(board, Move.CASTLING, Role.KING, king, false, rook, null)
    size += 1

  def pushEnPassant(board: Board, capturer: Int, to: Int): Unit =
    buffer(size).set(board, Move.EN_PASSANT, Role.PAWN, capturer, true, to, null)
    size += 1

  def sort(): Unit =
    java.util.Arrays.sort[Move](buffer, 0, size, comparator)

  def anyMatch(predicate: Move => Boolean): Boolean = buffer.take(size).exists(predicate)

  def retain(predicate: Move => Boolean): Unit =
    var i = 0
    while i < size do
      if !predicate(buffer(i)) then swapRemove(i)
      else i += 1

  private def swapRemove(i: Int): Unit =
    require(i < size)
    size -= 1
    val tmp = buffer(i)
    buffer(i) = buffer(size)
    buffer(size) = tmp

  def pretty(): String =
    val builder = StringBuilder()
    for i <- 0 until size do
      val m = buffer(i)
      builder.append(s"${m.uci()} ")
      // builder.append(s"${m.role} ${m.from} ${m.to} ${m.capture} ${m.promotion.getOrElse("")}")
    builder.toString()
