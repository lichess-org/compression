package org.lichess.compression.game.entropycoding;

import org.lichess.compression.BitOps;
import org.lichess.compression.BitWriter;

class RansEncoder {
    private final RansConfiguration configuration;
    private final Histogram symbolDistribution;
    private final int[] stateUpperBoundBySymbol;
    private int state;
    private int symbol;

    public RansEncoder(RansConfiguration configuration, int[] symbolFrequencies) {
        this.configuration = configuration;
        this.symbolDistribution = new Histogram(symbolFrequencies);
        this.stateUpperBoundBySymbol = computeStateUpperBoundBySymbol();
    }

    public void write(int symbol, BitWriter writer) {
        this.symbol = symbol;
        normalizeOverflowedState(writer);
        encode();
    }

    public int getState() {
        return state;
    }

    public void reset() {
        state = configuration.getStateLowerBound();
    }

    private int[] computeStateUpperBoundBySymbol() {
        int[] stateUpperBoundBySymbol = new int[symbolDistribution.getNumberOfBins()];
        for (int symbol = 0; symbol < symbolDistribution.getNumberOfBins(); symbol++) {
            int stateUpperBound = (1 << configuration.getNumberOfNormalizationBits()) * symbolDistribution.getFrequencyAt(symbol) *
                    (1 << configuration.getNumberOfBitsToReadAndWrite()) - 1;
            stateUpperBoundBySymbol[symbol] = stateUpperBound;
        }
        return stateUpperBoundBySymbol;
    }

    private void normalizeOverflowedState(BitWriter writer) {
        while (nextStateOverflows()) {
            writeBits(writer);
            state >>= configuration.getNumberOfBitsToReadAndWrite();
        }
    }

    private boolean nextStateOverflows() {
        return state > stateUpperBoundBySymbol[symbol];
    }

    private int computeNextState() {
        int nextState = (Math.floorDiv(state, symbolDistribution.getFrequencyAt(symbol)) << configuration.getNumberOfQuantizationBits()) +
                symbolDistribution.getCdfAt(symbol) + (state % symbolDistribution.getFrequencyAt(symbol));
        return nextState;
    }

    private void writeBits(BitWriter writer) {
        int bits = BitOps.moduloPowerOfTwo(state, configuration.getNumberOfBitsToReadAndWrite());
        writer.writeBits(bits, configuration.getNumberOfBitsToReadAndWrite());
    }

    private void encode() {
        int nextState = computeNextState();
        state = nextState;
    }
}
