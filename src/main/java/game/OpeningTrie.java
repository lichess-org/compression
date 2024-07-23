package org.lichess.compression.game;

import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;

import java.util.*;

public class OpeningTrie {
    private final int bitVectorLength;
    private final int maxOpeningPlies;
    private final Trie<String, BitSet> openingTrie;

    public OpeningTrie(Map<String, Integer> openingToCode) {
        this.maxOpeningPlies = getMaxOpeningPlies(openingToCode);
        this.bitVectorLength = getLowestSufficientBitVectorLength(openingToCode.values().toArray(Integer[]::new));
        this.openingTrie = buildOpeningTrie(openingToCode);
    }

    public BitSet get(String opening) {
        return openingTrie.get(opening);
    }

    public Optional<String> findLongestCommonOpening(String pgnMoves[]) {
        String openingMoves[] = Arrays.copyOf(pgnMoves, maxOpeningPlies);
        Optional<String> currentLongestCommonOpening = Optional.empty();
        long currentLongestCommonOpeningLength = 0;
        int fromIndex = 0;
        int toIndex = openingMoves.length;
        while (toIndex >= fromIndex) {
            int middleIndex = Math.floorDiv(fromIndex + toIndex, 2);
            StringBuilder openingBuilder = new StringBuilder();
            for (int i = 0; i <= middleIndex; i++) {
                openingBuilder.append(openingMoves[i]);
                openingBuilder.append(' ');
            }
            String opening = openingBuilder.toString();
            Set<String> commonOpenings = openingTrie.prefixMap(opening).keySet();
            if (commonOpenings.isEmpty()) {
                toIndex = middleIndex - 1;
            }
            else {
                String longestCommonOpening = commonOpenings
                        .stream()
                        .max(Comparator.comparingLong(o -> o.chars().filter(c -> c == ' ').count()))
                        .orElseThrow();
                long longestCommonOpeningLength = longestCommonOpening.chars().filter(c -> c == ' ').count();
                if (longestCommonOpeningLength > currentLongestCommonOpeningLength) {
                    currentLongestCommonOpening = Optional.of(longestCommonOpening);
                    currentLongestCommonOpeningLength = longestCommonOpeningLength;
                }
                fromIndex = middleIndex + 1;
            }
        }
        return currentLongestCommonOpening;
    }

    private int getMaxOpeningPlies(Map<String, Integer> openingToCode) {
        return openingToCode
                .keySet()
                .stream()
                .map(opening -> opening.split("\\s+").length)
                .max(Integer::compare)
                .orElse(0);
    }

    private int getLowestSufficientBitVectorLength(Integer integers[]) {
        int max = Arrays.stream(integers).max(Integer::compare).orElse(0);
        return (31 - Integer.numberOfLeadingZeros(max)) + 1;
    }

    private Trie<String, BitSet> buildOpeningTrie(Map<String, Integer> openingToCode) {
        Trie<String, BitSet> openingTrie = new PatriciaTrie<>();
        for (String opening : openingToCode.keySet()) {
            try {
                int code = openingToCode.get(opening);
                BitSet bitVector = convertIntToBitVector(code);
                openingTrie.put(opening, bitVector);
            } catch (IllegalArgumentException e) {
            }
        }
        return openingTrie;
    }

    private BitSet convertIntToBitVector(int i) throws IllegalArgumentException {
        if (i > (1 << bitVectorLength) - 1) {
            String errorMessage = String.format("The integer %d is too large to fit into %d bits", i, bitVectorLength);
            throw new IllegalArgumentException(errorMessage);
        }
        BitSet bitVector = new BitSet();
        int index = 0;
        for (int j = bitVectorLength - 1; j >= 0; j--) {
            boolean jThBit = ((i >> j) & 1) == 1;
            bitVector.set(index, jThBit);
        }
        return bitVector;
    }
}
