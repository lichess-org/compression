package org.lichess.compression.game

import org.specs2.mutable.*

import scala.jdk.CollectionConverters.*

class OpeningTrieTest extends Specification:
    "opening trie" should:
      "find longest common opening" in:
        val openingToCode: Map[String, Integer] = Map(
          "e4 c5" -> 0,
          "d4 d5" -> 1,
          "d4 Sf6" -> 2)
        val bitVectorLength = 3
        val openingTrie = OpeningTrie(openingToCode.asJava, bitVectorLength)
        val pgnMoves = "d4 Sf6 c4".split(" ")
        val longestCommonOpening = openingTrie.findLongestCommonOpening(pgnMoves)
        val expectedLongestCommonOpening = "d4 Sf6"
        longestCommonOpening.get() must_== expectedLongestCommonOpening
