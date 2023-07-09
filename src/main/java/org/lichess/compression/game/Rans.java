package org.lichess.compression.game;

import org.lichess.compression.BitReader;
import org.lichess.compression.BitWriter;

public class Rans implements Compression {
    private final RansEncoder encoder;
    private final RansDecoder decoder;

    public Rans(RansConfiguration configuration, int[] symbolFrequencies) {
        this.encoder = new RansEncoder(configuration, symbolFrequencies);
        this.decoder = new RansDecoder(configuration, symbolFrequencies);
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
