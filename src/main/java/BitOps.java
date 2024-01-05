package org.lichess.compression;

public class BitOps {
    static int[] getBitMasks() {
        int[] mask = new int[32];
        for (int i = 0; i < 32; i++) {
            mask[i] = (1 << i) - 1;
        }
        return mask;
    }

    public static int moduloPowerOfTwo(int dividend, int exponent) {
        return dividend & ((1 << exponent) - 1);
    }
}
