package org.lichess.compression.game.codec;

public class FrequencyDistribution
{
    private final int[] cdf;

    public FrequencyDistribution(int[] frequencies) {
        this.cdf = computeCdf(frequencies);
    }

    private static int[] computeCdf(int[] frequencies) {
        int[] cdf = new int[frequencies.length + 1];
        cdf[0] = 0;
        for (int i = 1; i < cdf.length; i++) {
            cdf[i] = cdf[i - 1] + frequencies[i - 1];
        }
        return cdf;
    }

    public int getFrequencyAt(int i) {
        return cdf[i + 1] - cdf[i];
    }

    public int getCdfAt(int i) {
        return cdf[i];
    }

    public int getQuantileFunction(int frequency) {
        return BinarySearch.search(cdf, frequency);
    }

    public int getNumberOfBins() {
        return cdf.length - 1;
    }
}
