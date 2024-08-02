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
        int argMax = -1;
        StringBuilder openingBuilder = new StringBuilder();
        for (int i = 0; i < maxOpeningPlies; i++) {
            openingBuilder.append(pgnMoves[i]);
            String opening = openingBuilder.toString();
            boolean foundLongerCommonOpening = !openingTrie.prefixMap(opening).isEmpty();
            if (foundLongerCommonOpening) {
                argMax = i;
            }
            else {
                break;
            }
            openingBuilder.append(' ');
        }
        if (argMax == -1) {
            return Optional.empty();
        }
        StringBuilder longestCommonOpeningBuilder = new StringBuilder();
        for (int i = 0; i <= argMax; i++) {
            longestCommonOpeningBuilder.append(pgnMoves[i]);
            if (i < argMax) {
                longestCommonOpeningBuilder.append(' ');
            }
        }
        String longestCommonOpening = longestCommonOpeningBuilder.toString();
        if (openingTrie.containsKey(longestCommonOpening)) {
            return Optional.of(longestCommonOpening);
        }
        return Optional.empty();
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
