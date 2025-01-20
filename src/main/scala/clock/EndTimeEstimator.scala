package org.lichess.compression.clock

object EndTimeEstimator:

  def encode(vals: Array[Int], startTime: Int): Unit =
    val maxIdx = vals.length - 1
    if maxIdx < 32 then vals(maxIdx) -= startTime - ((startTime * maxIdx) >>> 5)

  def decode(vals: Array[Int], startTime: Int): Unit =
    val maxIdx = vals.length - 1
    if maxIdx < 32 then vals(maxIdx) += startTime - ((startTime * maxIdx) >>> 5)
