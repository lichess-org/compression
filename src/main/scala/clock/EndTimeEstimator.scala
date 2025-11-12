package org.lichess.compression.clock

object EndTimeEstimator:
  private inline def endEstimate(maxIdx: Int, startTime: Int): Int =
    startTime - ((startTime * maxIdx) >>> 5)

  def encode(vals: Array[Int], startTime: Int): Unit =
    val maxIdx = vals.length - 1
    if maxIdx < 32 then vals(maxIdx) -= endEstimate(maxIdx, startTime)

  def decode(vals: Array[Int], startTime: Int): Unit =
    val maxIdx = vals.length - 1
    if maxIdx < 32 then vals(maxIdx) += endEstimate(maxIdx, startTime)
