package org.lichess.compression.game.codec;

import org.lichess.compression.BitReader;
import org.lichess.compression.BitWriter;

public class Rans {
    private final int[] MOVE_INDEX_FREQUENCIES = {205, 120, 77, 58, 47, 35, 27, 23, 20, 18, 16, 15, 15, 12, 13, 11, 10, 10, 10, 8, 7, 7, 7, 6, 6, 5, 4, 4, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
    private final int NUMBER_OF_QUANTIZATION_BITS = 10;
    private final int NUMBER_OF_NORMALIZATION_BITS = 13;
    private final RansConfiguration configuration = new RansConfiguration(NUMBER_OF_QUANTIZATION_BITS, NUMBER_OF_NORMALIZATION_BITS);
    private final RansEncoder encoder;
    private final RansDecoder decoder;

    public Rans() {
        this.encoder = new RansEncoder(configuration, MOVE_INDEX_FREQUENCIES);
        this.decoder = new RansDecoder(configuration, MOVE_INDEX_FREQUENCIES);
    }

    public void write(int symbol, BitWriter writer) {
        encoder.write(symbol, writer);
    }

    public int read(BitReader reader) {
        int symbol = decoder.read(reader);
        return symbol;
    }

    public void resetEncoder() {
        encoder.reset();
    }

    public void initializeDecoder(int stateAfterEncoding) {
        decoder.initialize(stateAfterEncoding);
    }

    public int getState() {
        return encoder.getState();
    }
}
