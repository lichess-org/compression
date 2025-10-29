package org.lichess.compression.clock

object EndTimeEstimator:

  @inline def encode(vals: Array[Int], startTime: Int): Unit =
    val maxIdx = vals.length - 1
    if maxIdx < 32 then vals(maxIdx) -= startTime - ((startTime * maxIdx) >>> 5)

  @inline def decode(vals: Array[Int], startTime: Int): Unit =
    val maxIdx = vals.length - 1
    if maxIdx < 32 then vals(maxIdx) += startTime - ((startTime * maxIdx) >>> 5)
