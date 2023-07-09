package org.lichess.compression.game;

import org.lichess.compression.BitOps;
import org.lichess.compression.BitReader;

class RansDecoder {
    private final RansConfiguration configuration;
    private final Histogram symbolDistribution;
    private int state;
    private int symbol;

    public RansDecoder(RansConfiguration configuration, int[] symbolFrequencies) {
        this.configuration = configuration;
        this.symbolDistribution = new Histogram(symbolFrequencies, configuration.getNumberOfQuantizationBits());
    }

    public int read(BitReader reader) {
        assert stateInsideRange();
        decode();
        normalizeStateByReadingBits(reader);
        assert stateInsideRange();
        return symbol;
    }

    public void initialize(int state) {
        this.state = state;
    }

    private boolean stateInsideRange() {
        return !stateOutsideRange();
    }

    private void decode() {
        symbol = computeNextSymbol();
        state = computePreviousState();
    }

    private int computeNextSymbol() {
        int i = BitOps.moduloPowerOfTwo(state, configuration.getNumberOfQuantizationBits());
        int nextSymbol = symbolDistribution.getQuantileFunctionAt(i);
        return nextSymbol;
    }

    private int computePreviousState() {
        int previousState = symbolDistribution.getFrequencyAt(symbol) * (state >> configuration.getNumberOfQuantizationBits()) + BitOps.moduloPowerOfTwo(state, configuration.getNumberOfQuantizationBits()) - symbolDistribution.getCdfAt(symbol);
        return previousState;
    }

    private void normalizeStateByReadingBits(BitReader reader) {
        while (stateOutsideRange()) {
            int bits = readBits(reader);
            state = (state << configuration.getNumberOfBitsToReadAndWrite()) + bits;
        }
    }

    private boolean stateOutsideRange() {
        return stateUnderflowed() || stateOverflowed();
    }

    private boolean stateUnderflowed() {
        int lowerBoundInclusive = 1 << configuration.getNumberOfNormalizationBits();
        return state < lowerBoundInclusive;
    }

    private boolean stateOverflowed() {
        int upperBoundExclusive = 1 << configuration.getNumberOfBitsToReadAndWrite() + configuration.getNumberOfNormalizationBits();
        return state >= upperBoundExclusive;
    }

    private int readBits(BitReader reader) {
        int bits = reader.readBits(configuration.getNumberOfBitsToReadAndWrite());
        return bits;
    }
}
