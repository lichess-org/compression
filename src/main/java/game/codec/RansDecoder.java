package org.lichess.compression.game.entropycoding;

import org.lichess.compression.BitOps;
import org.lichess.compression.BitReader;

class RansDecoder {
    private final RansConfiguration configuration;
    private final Histogram symbolDistribution;
    private int state;
    private int symbol;

    public RansDecoder(RansConfiguration configuration, int[] symbolFrequencies) {
        this.configuration = configuration;
        this.symbolDistribution = new Histogram(symbolFrequencies);
    }

    public int read(BitReader reader) {
        decode();
        normalizeUnderflowedState(reader);
        return symbol;
    }

    public void initialize(int state) {
        this.state = state;
    }

    private void decode() {
        symbol = computeNextSymbol();
        state = computePreviousState();
    }

    private int computeNextSymbol() {
        int i = BitOps.moduloPowerOfTwo(state, configuration.getNumberOfQuantizationBits());
        int nextSymbol = symbolDistribution.getQuantileFunction(i);
        return nextSymbol;
    }

    private int computePreviousState() {
        int previousState = symbolDistribution.getFrequencyAt(symbol) * (state >> configuration.getNumberOfQuantizationBits()) + BitOps.moduloPowerOfTwo(state, configuration.getNumberOfQuantizationBits()) - symbolDistribution.getCdfAt(symbol);
        return previousState;
    }

    private void normalizeUnderflowedState(BitReader reader) {
        while (configuration.stateUnderflowed(state)) {
            int bits = readBits(reader);
            state = (state << configuration.getNumberOfBitsToReadAndWrite()) + bits;
        }
    }

    private int readBits(BitReader reader) {
        int bits = reader.readBits(configuration.getNumberOfBitsToReadAndWrite());
        return bits;
    }
}
