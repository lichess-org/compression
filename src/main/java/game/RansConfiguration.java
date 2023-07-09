package org.lichess.compression.game;

public class RansConfiguration {
    private final int numberOfQuantizationBits;
    private final int numberOfNormalizationBits;
    private final int numberOfBitsToReadAndWrite = 8;

    public RansConfiguration(int numberOfQuantizationBits, int numberOfNormalizationBits) {
        assert numberOfNormalizationBits >= numberOfQuantizationBits;
        assert numberOfNormalizationBits + numberOfQuantizationBits < 32;
        this.numberOfQuantizationBits = numberOfQuantizationBits;
        this.numberOfNormalizationBits = numberOfNormalizationBits;
    }

    public int getNumberOfQuantizationBits() {
        return numberOfQuantizationBits;
    }

    public int getNumberOfNormalizationBits() {
        return numberOfNormalizationBits;
    }

    public int getNumberOfBitsToReadAndWrite() {
        return numberOfBitsToReadAndWrite;
    }
}
