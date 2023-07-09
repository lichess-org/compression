package org.lichess.compression.game;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.IntStream;


public class Histogram {
    private final int[] frequencies;
    private final int totalFrequency;
    private final int[] cdf;
    private final int[] quantileFunction;

    public Histogram(int[] frequencies, int numberOfQuantizationBits) {
        this.totalFrequency = 1 << numberOfQuantizationBits;
        this.frequencies = normalizeFrequencies(frequencies);
        this.cdf = computeCdf();
        this.quantileFunction = computeQuantileFunction();
    }

    private int[] normalizeFrequencies(int[] frequencies) {
        int[] quantizedFrequencies = quantizeFrequencies(frequencies);
        int[] normalizedFrequencies = normalizeZeroFrequencies(quantizedFrequencies);
        return normalizedFrequencies;
    }

    private int[] quantizeFrequencies(int[] frequencies) {
        int totalFrequencyBeforeQuantization = Arrays.stream(frequencies).sum();
        int[] quantizedFrequencies = Arrays.stream(frequencies)
                .mapToDouble(Double::valueOf)
                .map(symbolFrequency -> symbolFrequency / totalFrequencyBeforeQuantization)
                .map(symbolProbability -> symbolProbability * totalFrequency)
                .map(Math::floor)
                .mapToLong(Math::round)
                .mapToInt(Math::toIntExact)
                .toArray();
        return quantizedFrequencies;
    }

    private int[] normalizeZeroFrequencies(int[] frequencies) {
        assert Arrays.stream(frequencies).sum() >= frequencies.length;
        while (hasZeroFrequency(frequencies)) {
            int[] indexesWhereFrequencyZero = IntStream.range(0, frequencies.length).filter(i -> frequencies[i] == 0).toArray();
            int[] indexesWhereFrequencyGreaterThanOneSortedByFrequency = IntStream.range(0, frequencies.length)
                    .filter(i -> frequencies[i] > 1)
                    .mapToObj(i -> new AbstractMap.SimpleEntry<Integer, Integer>(i, frequencies[i]))
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .mapToInt(Map.Entry::getKey)
                    .toArray();
            for (int i = 0; i < Math.min(indexesWhereFrequencyGreaterThanOneSortedByFrequency.length, indexesWhereFrequencyZero.length); i++) {
                int indexWhereFrequencyIsZero = indexesWhereFrequencyZero[i];
                int indexWhereFrequencyGreaterThanOne = indexesWhereFrequencyGreaterThanOneSortedByFrequency[i];
                frequencies[indexWhereFrequencyIsZero]++;
                frequencies[indexWhereFrequencyGreaterThanOne]--;
            }
        }
        return frequencies;
    }

    private boolean hasZeroFrequency(int[] frequencies) {
        return Arrays.stream(frequencies).anyMatch(frequency -> frequency == 0);
    }

    private int[] computeCdf() {
        return IntStream.range(0, frequencies.length + 1).map(i -> Arrays.stream(frequencies).limit(i).sum()).toArray();
    }

    private int[] computeQuantileFunction() {
        int[] quantileFunction = new int[totalFrequency];
        for (int i = 1; i < cdf.length; i++) {
            for (int j = cdf[i - 1]; j < cdf[i]; j++) {
                quantileFunction[j] = i - 1;
            }
        }
        return quantileFunction;
    }

    public int getFrequencyAt(int i) {
        return frequencies[i];
    }

    public int getCdfAt(int i) {
        return cdf[i];
    }

    public int getQuantileFunctionAt(int i) {
        return quantileFunction[i];
    }
}
