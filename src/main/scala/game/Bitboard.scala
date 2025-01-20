package org.lichess.compression.game

import scala.annotation.tailrec
import scala.collection.mutable.HashSet

object Bitboard:
  val ALL: Long = -1L

  val RANKS: Array[Long] = Array.tabulate(8)(i => 0xffL << (i * 8))
  val FILES: Array[Long] = Array.tabulate(8)(i => 0x0101010101010101L << i)

  private val KNIGHT_DELTAS     = Array(17, 15, 10, 6, -17, -15, -10, -6)
  private val BISHOP_DELTAS     = Array(7, -7, 9, -9)
  private val ROOK_DELTAS       = Array(1, -1, 8, -8)
  private val KING_DELTAS       = Array(1, 7, 8, 9, -1, -7, -8, -9)
  private val WHITE_PAWN_DELTAS = Array(7, 9)
  private val BLACK_PAWN_DELTAS = Array(-7, -9)

  val KNIGHT_ATTACKS: Array[Long]     = Array.ofDim(64)
  val KING_ATTACKS: Array[Long]       = Array.ofDim(64)
  val WHITE_PAWN_ATTACKS: Array[Long] = Array.ofDim(64)
  val BLACK_PAWN_ATTACKS: Array[Long] = Array.ofDim(64)

  val BETWEEN: Array[Array[Long]] = Array.ofDim(64, 64)
  val RAYS: Array[Array[Long]]    = Array.ofDim(64, 64)

  private val ATTACKS: Array[Long] = Array.ofDim(88772)

  def slidingAttacks(square: Int, occupied: Long, deltas: Array[Int]): Long =
    def attackLoop(deltaIndex: Int, acc: Long): Long =
      if deltaIndex >= deltas.length then acc
      else
        @tailrec
        def deltaLoop(sq: Int, tempAcc: Long): Long =
          val newSq = sq + deltas(deltaIndex)
          if newSq < 0 || newSq >= 64 || Square.distance(newSq, sq) > 2 then
            attackLoop(deltaIndex + 1, tempAcc)
          else
            val newAcc = tempAcc | (1L << newSq)
            if (Bitboard.contains(occupied, newSq)) attackLoop(deltaIndex + 1, newAcc)
            else deltaLoop(newSq, newAcc)
        deltaLoop(square, acc)
    attackLoop(0, 0L)

  private def initMagics(square: Int, magic: Magic, shift: Int, deltas: Array[Int]): Unit =
    @tailrec
    def updateAttacks(subset: Long): Unit =
      val attack = slidingAttacks(square, subset, deltas)
      val idx    = ((magic.factor * subset) >>> (64 - shift)).toInt + magic.offset
      assert(ATTACKS(idx) == 0 || ATTACKS(idx) == attack)
      ATTACKS(idx) = attack
      val nextSubset = (subset - magic.mask) & magic.mask
      if (nextSubset != 0) updateAttacks(nextSubset)

    updateAttacks(0L)

  for i <- 0 until 64
  do
    KNIGHT_ATTACKS(i) = slidingAttacks(i, Bitboard.ALL, KNIGHT_DELTAS)
    KING_ATTACKS(i) = slidingAttacks(i, Bitboard.ALL, KING_DELTAS)
    WHITE_PAWN_ATTACKS(i) = slidingAttacks(i, Bitboard.ALL, WHITE_PAWN_DELTAS)
    BLACK_PAWN_ATTACKS(i) = slidingAttacks(i, Bitboard.ALL, BLACK_PAWN_DELTAS)

    initMagics(i, Magic.ROOK(i), 12, ROOK_DELTAS)
    initMagics(i, Magic.BISHOP(i), 9, BISHOP_DELTAS)

  for
    a <- 0 until 64
    b <- 0 until 64
  do
    if contains(slidingAttacks(a, 0, ROOK_DELTAS), b) then
      BETWEEN(a)(b) = slidingAttacks(a, 1L << b, ROOK_DELTAS) &
        slidingAttacks(b, 1L << a, ROOK_DELTAS)
      RAYS(a)(b) = (1L << a) | (1L << b) |
        slidingAttacks(a, 0, ROOK_DELTAS) &
        slidingAttacks(b, 0, ROOK_DELTAS)
    else if contains(slidingAttacks(a, 0, BISHOP_DELTAS), b) then
      BETWEEN(a)(b) = slidingAttacks(a, 1L << b, BISHOP_DELTAS) &
        slidingAttacks(b, 1L << a, BISHOP_DELTAS)
      RAYS(a)(b) = (1L << a) | (1L << b) |
        slidingAttacks(a, 0, BISHOP_DELTAS) &
        slidingAttacks(b, 0, BISHOP_DELTAS)

  def bishopAttacks(square: Int, occupied: Long): Long =
    val magic = Magic.BISHOP(square)
    ATTACKS(((magic.factor * (occupied & magic.mask)) >>> (64 - 9)).toInt + magic.offset)

  def rookAttacks(square: Int, occupied: Long): Long =
    val magic = Magic.ROOK(square)
    ATTACKS(((magic.factor * (occupied & magic.mask)) >>> (64 - 12)).toInt + magic.offset)

  def queenAttacks(square: Int, occupied: Long): Long =
    bishopAttacks(square, occupied) ^ rookAttacks(square, occupied)

  def pawnAttacks(white: Boolean, square: Int): Long =
    if white then WHITE_PAWN_ATTACKS(square) else BLACK_PAWN_ATTACKS(square)

  def lsb(b: Long): Int =
    assert(b != 0)
    java.lang.Long.numberOfTrailingZeros(b)

  def msb(b: Long): Int =
    assert(b != 0)
    63 - java.lang.Long.numberOfLeadingZeros(b)

  def moreThanOne(b: Long): Boolean =
    (b & (b - 1L)) != 0

  def contains(b: Long, sq: Int): Boolean =
    (b & (1L << sq)) != 0

  def squareSet(b: Long): Set[Int] =
    var remaining = b
    val set       = HashSet[Int]()
    while remaining != 0 do
      val sq = lsb(remaining)
      set.add(sq)
      remaining &= remaining - 1L
    set.toSet
