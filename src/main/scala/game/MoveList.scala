package org.lichess.compression.game

final class MoveList(capacity: Int = 256):
  private final val buffer = Array.tabulate(capacity)(_ => Move())
  private var size         = 0

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

  def find(predicate: Move => Boolean): Option[Move] =
    buffer.view.take(size).find(predicate)

  def exists(predicate: Move => Boolean): Boolean =
    buffer.view.take(size).exists(predicate)

  def rank(move: Move): Int =
    buffer.view.take(size).count(_ < move)

  def retain(predicate: Move => Boolean): Unit =
    var i = 0
    while i < size do
      if !predicate(buffer(i)) then swapRemove(i)
      else i += 1

  private def swapRemove(i: Int): Unit =
    require(i < size)
    size -= 1
    swap(i, size)

  private def swap(i: Int, j: Int): Unit =
    val tmp = buffer(i)
    buffer(i) = buffer(j)
    buffer(j) = tmp

  def selectRank(rank: Int): Move =
    require(rank < size)
    // Quickselect. Bounds are small enough that naive pivot selection is ok,
    // even on adversarial inputs.
    var left = 0
    var right = size - 1
    while left < right do
      val pivot = partition(left, right)
      if pivot == rank then return buffer(rank)
      if pivot < rank then left = pivot + 1
      else right = pivot - 1
    buffer(rank)

  private def partition(left: Int, right: Int): Int =
    val pivot = buffer(right)
    var i = left - 1
    for j <- left until right do
      if buffer(j) < pivot then
        i += 1
        swap(i, j)
    i += 1
    swap(i, right)
    i

  def pretty(): String =
    val builder = StringBuilder()
    for i <- 0 until size do
      val m = buffer(i)
      builder.append(s"${m.uci()} ")
      // builder.append(s"${m.role} ${m.from} ${m.to} ${m.capture} ${m.promotion.getOrElse("")}")
    builder.toString()
