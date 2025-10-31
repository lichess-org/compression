package org.lichess.compression.clock

object LinearEstimator:

  def encode(dest: Array[Int], startTime: Int): Unit =
    val maxIdx = dest.length - 1
    encode(dest, -1, startTime, maxIdx, dest(maxIdx))

  def decode(dest: Array[Int], startTime: Int): Unit =
    val maxIdx = dest.length - 1
    decode(dest, -1, startTime, maxIdx, dest(maxIdx))

  private def encode(dest: Array[Int], startIdx: Int, start: Int, endIdx: Int, end: Int): Unit =
    val midIdx = (startIdx + endIdx) >> 1
    if startIdx == midIdx then return

    val mid = dest(midIdx)
    dest(midIdx) = mid - ((start + end) >> 1)

    encode(dest, startIdx, start, midIdx, mid)
    encode(dest, midIdx, mid, endIdx, end)

  private def decode(dest: Array[Int], startIdx: Int, start: Int, endIdx: Int, end: Int): Unit =
    val midIdx = (startIdx + endIdx) >> 1
    if startIdx == midIdx then return

    dest(midIdx) += (start + end) >> 1
    val mid = dest(midIdx)

    decode(dest, startIdx, start, midIdx, mid)
    decode(dest, midIdx, mid, endIdx, end)
