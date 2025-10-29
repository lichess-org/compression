package org.lichess.compression.benchmark

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.TimeUnit

import org.lichess.compression.clock.Encoder

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
class OverallEncodingTest extends EncodingTestData:

  @Benchmark
  def testEncode(blackhole: Blackhole) =
    blackhole.consume(Encoder.encode(centis, startTime))

  @Benchmark
  def testDecode(blackhole: Blackhole) =
    blackhole.consume(Encoder.decode(encoded, startTime))
