package org.lichess.compression.game;

import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class OpeningTrie {
    private final int bitVectorLength;
    private final int maxOpeningPlies;
    private final Trie<String, Integer> openingTrie;

    public OpeningTrie(Map<String, Integer> openingToCode) {
        this.maxOpeningPlies = getMaxOpeningPlies(openingToCode);
        this.bitVectorLength = getLowestSufficientBitVectorLength(openingToCode.values().toArray(Integer[]::new));
        this.openingTrie = buildOpeningTrie(openingToCode);
    }

    public static OpeningTrie mostCommonOpenings() {
        Map<String, Integer> mostCommonOpeningToCode = getMostCommonOpeningToCode();
        return new OpeningTrie(mostCommonOpeningToCode);
    }

    public int get(String opening) {
        return openingTrie.get(opening);
    }
    
    public Set<String> keySet() {
        return openingTrie.keySet();
    }
    
    public int getBitVectorLength() {
        return bitVectorLength;
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
        return bitWidth(max);
    }

    private Trie<String, Integer> buildOpeningTrie(Map<String, Integer> openingToCode) {
        Trie<String, Integer> openingTrie = new PatriciaTrie<>();
        for (String opening : openingToCode.keySet()) {
            int code = openingToCode.get(opening);
            if (bitWidth(code) <= bitVectorLength) {
                openingTrie.put(opening, code);
            }
        }
        return openingTrie;
    }

    private int bitWidth(int i) {
        return (31 - Integer.numberOfLeadingZeros(i)) + 1;
    }

    private static Map<String, Integer> getMostCommonOpeningToCode() {
        try {
            Path filepath = Path.of("src/main/java/game/most_common_opening_moves_sorted.txt");
            List<String> mostCommonOpenings = Files.readAllLines(filepath);
            Map<String, Integer> mostCommonOpeningToCode = new HashMap<>();
            int i = 0;
            for (String opening: mostCommonOpenings) {
                mostCommonOpeningToCode.put(opening, i);
                i++;
            }
            return mostCommonOpeningToCode;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
