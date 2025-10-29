package org.lichess.compression.benchmark

import org.lichess.compression.clock.*
import scala.util.Random

trait EncodingTestData:
  private val r = new Random(1)
  val startTime = 30000
  val centis    = Range(29100, 0, -1000).map(_ + (r.nextGaussian * 500) toInt).toArray
  val moves     = centis.size - 1
  val trunced   = centis.clone
  LowBitTruncator.truncate(trunced)
  val encodedRounds = trunced.clone
  LinearEstimator.encode(encodedRounds, startTime >> 3)
  val encoded = Encoder.encode(centis, startTime)
