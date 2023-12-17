package org.lichess.compression.game.entropycoding;

public final class RansConfiguration {
    private final int numberOfQuantizationBits;
    private final int numberOfNormalizationBits;
    private final int numberOfBitsToReadAndWrite;
    private final int stateLowerBound;

    public RansConfiguration(int numberOfQuantizationBits, int numberOfNormalizationBits) {
        this.numberOfQuantizationBits = numberOfQuantizationBits;
        this.numberOfNormalizationBits = numberOfNormalizationBits;
        numberOfBitsToReadAndWrite = 8;
        stateLowerBound = 1 << (numberOfNormalizationBits + numberOfQuantizationBits);
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

    public int getStateLowerBound() {
        return stateLowerBound;
    }

    public boolean stateUnderflowed(int state) {
        return state < stateLowerBound;
    }
}
