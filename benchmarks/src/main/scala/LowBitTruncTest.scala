package org.lichess.compression.benchmark

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.TimeUnit

import org.lichess.compression.clock.LowBitTruncator

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
class LowBitTruncTest extends EncodingTestData:
  private val testData: Array[Int] = new Array(centis.length)

  @Setup(Level.Invocation)
  def testSetup() =
    System.arraycopy(centis, 0, testData, 0, centis.length)

  @Benchmark
  def testEncode(blackhole: Blackhole) =
    LowBitTruncator.truncate(testData)
    blackhole.consume(testData)
