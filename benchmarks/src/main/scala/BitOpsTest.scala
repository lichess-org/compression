package org.lichess.compression.benchmark

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

import org.lichess.compression.BitOps.Reader

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
class BitOpsTest extends EncodingTestData:

  @Benchmark
  def testRead(blackhole: Blackhole) =
    val r = new Reader(encoded)
    blackhole.consume(Range(0, 50).map(_ => r.readBits(4)))
