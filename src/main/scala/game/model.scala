package org.lichess.compression.game

case class Piece(white: Boolean, role: Role)

enum Role(val index: Int, val symbol: String):
  case PAWN   extends Role(0, "")
  case KNIGHT extends Role(1, "N")
  case BISHOP extends Role(2, "B")
  case ROOK   extends Role(3, "R")
  case QUEEN  extends Role(4, "Q")
  case KING   extends Role(5, "K")

object Square:
  val A1 = 0
  val C1 = 2
  val D1 = 3
  val F1 = 5
  val G1 = 6
  val H1 = 7
  val A8 = 56
  val H8 = 63

  def square(file: Int, rank: Int): Int = file ^ (rank << 3)

  def file(square: Int): Int = square & 7

  def rank(square: Int): Int = square >>> 3

  def mirror(square: Int): Int = square ^ 0x38

  def combine(a: Int, b: Int): Int = square(file(a), rank(b))

  def distance(a: Int, b: Int): Int = Math.max(Math.abs(file(a) - file(b)), Math.abs(rank(a) - rank(b)))

  def aligned(a: Int, b: Int, c: Int): Boolean = Bitboard.contains(Bitboard.RAYS(a)(b), c)
