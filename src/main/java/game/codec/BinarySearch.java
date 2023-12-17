package org.lichess.compression.game.codec;

import java.util.Arrays;

public class BinarySearch {
    public static int search(int[] a, int key) {
        int i = Arrays.binarySearch(a, key);
        boolean keyNotFound = i < 0;
        if (keyNotFound) {
            return -i - 2;
        }
        return i;
    }
}
