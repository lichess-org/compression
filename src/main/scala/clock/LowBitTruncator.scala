package org.lichess.compression.clock

import org.lichess.compression.BitOps

object LowBitTruncator:
  private final val CENTI_CUTOFF = 1000

  def truncate(centis: Array[Int]): Unit =
    for i <- 0 until centis.length do centis(i) >>= 3

  def truncate(centi: Int): Int = centi >> 3

  def writeDigits(centis: Array[Int], writer: BitOps.Writer): Unit =
    val maxIdx = centis.length - 1
    for i <- 0 until maxIdx do if centis(i) < CENTI_CUTOFF then writer.writeBits(centis(i), 3)
    writer.writeBits(centis(maxIdx), 3)

  def decode(trunced: Array[Int], reader: BitOps.Reader): Unit =
    val maxIdx = trunced.length - 1
    for i <- 0 until maxIdx do
      val rounded = trunced(i) << 3
      trunced(i) = rounded | (if rounded < CENTI_CUTOFF then reader.readBits(3) else 3)
    trunced(maxIdx) = (trunced(maxIdx) << 3) | reader.readBits(3)
