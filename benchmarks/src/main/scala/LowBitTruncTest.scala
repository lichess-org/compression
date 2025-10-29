package org.lichess.compression.benchmark

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.TimeUnit

import org.lichess.compression.clock.LowBitTruncator

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
class LowBitTruncTest extends EncodingTestData:

  val testData = centis.clone()

  @Benchmark
  def testEncode(blackhole: Blackhole) =
    LowBitTruncator.truncate(testData)
    blackhole.consume(testData)
