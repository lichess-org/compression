package org.lichess.compression.benchmark

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.TimeUnit

import org.lichess.compression.clock.LinearEstimator

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
class LinearEstimateTest extends EncodingTestData:

  @Benchmark
  def testEncode(blackhole: Blackhole) =
    LinearEstimator.encode(trunced, startTime)
    blackhole.consume(trunced)
