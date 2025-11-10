package org.lichess.compression

import java.util.Arrays
import java.nio.ByteBuffer

object BitOps:
  private val BitMasks: Array[Int] = Array.tabulate(32)(i => (1 << i) - 1)

  def writeSigned(values: Array[Int], writer: Writer): Unit =
    values.foreach(writeSigned(_, writer))

  def writeSigned(n: Int, writer: Writer): Unit =
    // zigzag encode
    writeUnsigned((n << 1) ^ (n >> 31), writer)

  def writeUnsigned(n: Int, writer: Writer): Unit =
    if (n & ~0x1f) == 0 then writer.writeBits(n, 6)
    else
      writer.writeBits(n | 0x20, 6)
      var remaining = n >>> 5
      while (remaining & ~0x07) != 0 do
        writer.writeBits(remaining | 0x08, 4)
        remaining >>>= 3
      // While loop terminated, so 4th bit is 0
      writer.writeBits(remaining, 4)

  def readUnsigned(reader: Reader): Int =
    var n = reader.readBits(6)
    if n > 0x1f then
      n &= 0x1f
      var curShift = 5
      var curVal   = 0
      while
        curVal = reader.readBits(4)
        curVal > 0x07
      do
        n |= (curVal & 0x07) << curShift
        curShift += 3
      n |= curVal << curShift
    n

  def readSigned(reader: Reader): Int =
    val n = readUnsigned(reader)
    (n >>> 1) ^ -(n & 1) // zigzag decode

  def readSigned(reader: Reader, numMoves: Int): Array[Int] =
    val arr = new Array[Int](numMoves)
    var i   = 0
    while i < numMoves do
      val n = readUnsigned(reader)
      arr(i) = (n >>> 1) ^ -(n & 1) // zigzag decode
      i += 1
    arr

  final class Reader(bytes: Array[Byte]):
    private val bb               = ByteBuffer.wrap(bytes)
    private var numRemainingBits = 0
    private var pendingBits      = 0

    private def readNext(): Unit =
      if bb.remaining >= 4 then
        pendingBits = bb.getInt()
        numRemainingBits = 32
      else
        numRemainingBits = bb.remaining * 8
        pendingBits = (bb.get() & 0xff) << (numRemainingBits - 8)
        var s = numRemainingBits - 16
        while s >= 0 do
          pendingBits |= (bb.get() & 0xff) << s
          s -= 8

    def readBits(numReqBits: Int): Int =
      if numRemainingBits >= numReqBits then
        numRemainingBits -= numReqBits
        (pendingBits >>> numRemainingBits) & BitMasks(numReqBits)
      else
        val res        = pendingBits & BitMasks(numRemainingBits)
        val neededBits = numReqBits - numRemainingBits
        readNext()
        (res << neededBits) | readBits(neededBits)

  final class Writer:
    private val buffer           = new IntArrayList()
    private var numRemainingBits = 32
    private var pendingBits      = 0

    def writeBits(data: Int, numBits: Int): Unit =
      val maskedData = data & BitMasks(numBits)
      numRemainingBits -= numBits
      if numRemainingBits >= 0 then pendingBits |= maskedData << numRemainingBits
      else
        buffer.add(pendingBits | (maskedData >>> -numRemainingBits))
        numRemainingBits += 32
        pendingBits = maskedData << numRemainingBits

    def toArray(): Array[Byte] =
      val numPendingBytes = (39 - numRemainingBits) >> 3
      val bb              = ByteBuffer.allocate(4 * buffer.size + numPendingBytes)
      buffer.writeTo(bb)
      if numPendingBytes == 4 then bb.putInt(pendingBits)
      else
        var i = 0
        while i < numPendingBytes do
          bb.put((pendingBits >>> (24 - i * 8)).toByte)
          i += 1
      bb.array

  private final class IntArrayList:
    private var data  = new Array[Int](10)
    private var index = 0

    def add(elt: Int): Unit =
      // This is the growth strategy used by OpenJDK ArrayList.
      if index == data.length then data = Arrays.copyOf(data, index + (index >> 1) + 5)
      data(index) = elt
      index += 1

    def size: Int = index

    def toArray(): Array[Int] = Arrays.copyOf(data, index)

    def writeTo(bb: ByteBuffer): Unit =
      var i = 0
      while i < index do
        bb.putInt(data(i))
        i += 1
