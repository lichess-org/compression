package org.lichess.compression.clock

import org.lichess.compression.BitOps

object LowBitTruncator:
  private val CENTI_CUTOFF = 1000

  def truncate(centis: Array[Int]): Unit =
    val len = centis.length
    var i   = 0
    while i < len do
      centis(i) >>= 3
      i += 1

  def truncate(centi: Int): Int = centi >> 3

  def writeDigits(centis: Array[Int], writer: BitOps.Writer): Unit =
    val maxIdx = centis.length - 1
    var i      = 0
    while i < maxIdx do
      if centis(i) < CENTI_CUTOFF then writer.writeBits(centis(i), 3)
      i += 1
    writer.writeBits(centis(maxIdx), 3)

  def decode(trunced: Array[Int], reader: BitOps.Reader): Unit =
    val maxIdx = trunced.length - 1
    var i      = 0
    while i <= maxIdx do
      val rounded = trunced(i) << 3
      trunced(i) =
        if rounded < CENTI_CUTOFF || i == maxIdx then rounded | reader.readBits(3)
        else rounded | 3
      i += 1
