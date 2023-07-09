package org.lichess.compression.game;

import org.lichess.compression.BitOps;
import org.lichess.compression.BitWriter;

class RansEncoder {
    private final RansConfiguration configuration;
    private final Histogram symbolDistribution;
    private int state;
    private int symbol;

    public RansEncoder(RansConfiguration configuration, int[] symbolFrequencies) {
        this.configuration = configuration;
        this.symbolDistribution = new Histogram(symbolFrequencies, configuration.getNumberOfQuantizationBits());
    }


    public void write(int symbol, BitWriter writer) {
        this.symbol = symbol;
        assert stateInsideRange();
        normalizeStateByWritingBits(writer);
        encode();
        assert stateInsideRange();
    }

    public int getState() {
        return state;
    }

    public void reset() {
        state = 1 << configuration.getNumberOfNormalizationBits();
    }

    private boolean stateInsideRange() {
        return !stateOutsideRange(state);
    }

    private void normalizeStateByWritingBits(BitWriter writer) {
        while (nextStateOutsideRange()) {
            writeBits(writer);
            state >>= configuration.getNumberOfBitsToReadAndWrite();
        }
    }

    private boolean nextStateOutsideRange() {
        long nextState = computeNextState();
        return stateOutsideRange(nextState);
    }

    private boolean stateOutsideRange(long state) {
        return stateUnderflowed(state) || stateOverflowed(state);
    }

    private boolean stateUnderflowed(long state) {
        int lowerBoundInclusive = 1 << configuration.getNumberOfNormalizationBits();
        return state < lowerBoundInclusive;
    }

    private boolean stateOverflowed(long state) {
        int upperBoundExclusive = 1 << configuration.getNumberOfBitsToReadAndWrite() + configuration.getNumberOfNormalizationBits();
        return state >= upperBoundExclusive;
    }

    private int computeNextState() {
        int nextState = (Math.floorDiv(state, symbolDistribution.getFrequencyAt(symbol)) << configuration.getNumberOfQuantizationBits()) + symbolDistribution.getCdfAt(symbol) + state % symbolDistribution.getFrequencyAt(symbol);
        return nextState;
    }

    private void writeBits(BitWriter writer) {
        writer.writeBits(BitOps.moduloPowerOfTwo(state, configuration.getNumberOfBitsToReadAndWrite()), configuration.getNumberOfBitsToReadAndWrite());
    }

    private void encode() {
        int nextState = computeNextState();
        state = nextState;
    }
}
