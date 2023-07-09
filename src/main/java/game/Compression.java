package org.lichess.compression.game;

import org.lichess.compression.BitReader;
import org.lichess.compression.BitWriter;

public interface Compression {
    void write(int symbol, BitWriter writer);

    int read(BitReader reader);
}
