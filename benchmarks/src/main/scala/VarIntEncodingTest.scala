package org.lichess.compression.benchmark

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.TimeUnit

import org.lichess.compression.BitOps
import org.lichess.compression.BitOps.{ Reader, Writer }

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
class VarIntEncodingTest extends EncodingTestData:

  @Benchmark
  def testEncode(blackhole: Blackhole) =
    val writer = new Writer
    BitOps.writeSigned(encodedRounds, writer)
    blackhole.consume(writer.toArray())

  @Benchmark
  def testDecode(blackhole: Blackhole) =
    blackhole.consume(BitOps.readSigned(new Reader(encoded), moves))
