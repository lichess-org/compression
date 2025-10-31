package org.lichess.compression.clock

import java.util.Arrays
import org.lichess.compression.BitOps

object Encoder:

  def encode(centis: Array[Int], startTime: Int): Array[Byte] =
    if centis.isEmpty then new Array(0)
    else

      val encoded        = Arrays.copyOf(centis, centis.length)
      val truncatedStart = LowBitTruncator.truncate(startTime)

      LowBitTruncator.truncate(encoded)
      LinearEstimator.encode(encoded, truncatedStart)
      EndTimeEstimator.encode(encoded, truncatedStart)

      val writer = BitOps.Writer()
      BitOps.writeUnsigned(encoded.length - 1, writer)
      BitOps.writeSigned(encoded, writer)
      LowBitTruncator.writeDigits(centis, writer)

      writer.toArray()

  def decode(bytes: Array[Byte], startTime: Int): Array[Int] =
    if bytes.isEmpty then new Array(0)
    else

      val reader         = BitOps.Reader(bytes)
      val truncatedStart = LowBitTruncator.truncate(startTime)

      val numMoves = BitOps.readUnsigned(reader) + 1
      val decoded  = BitOps.readSigned(reader, numMoves)

      EndTimeEstimator.decode(decoded, truncatedStart)
      LinearEstimator.decode(decoded, truncatedStart)
      LowBitTruncator.decode(decoded, reader)

      decoded
