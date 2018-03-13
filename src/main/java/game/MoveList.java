package org.lichess.compression.game;

import java.util.Arrays;
import java.util.function.Predicate;

final class MoveList {
    private final Move buffer[];
    private int size = 0;

    public MoveList() {
        this(256);
    }

    public MoveList(int capacity) {
        buffer = new Move[capacity];

        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = new Move();
        }
    }

    public void clear() {
        size = 0;
    }

    public int size() {
        return size;
    }

    public Move get(int i) {
        assert i < size;
        return buffer[i];
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void pushNormal(Board board, Role role, int from, boolean capture, int to) {
        buffer[size++].set(board, Move.NORMAL, role, from, capture, to, null);
    }

    public void pushPromotion(Board board, int from, boolean capture, int to, Role promotion) {
        buffer[size++].set(board, Move.NORMAL, Role.PAWN, from, capture, to, promotion);
    }

    public void pushCastle(Board board, int king, int rook) {
        buffer[size++].set(board, Move.CASTLING, Role.KING, king, false, rook, null);
    }

    public void pushEnPassant(Board board, int capturer, int to) {
        buffer[size++].set(board, Move.EN_PASSANT, Role.PAWN, capturer, true, to, null);
    }

    public void sort() {
        Arrays.sort(buffer, 0, size, null);
    }

    public void retain(Predicate<Move> predicate) {
        int i = 0;
        while (i < size) {
            if (predicate.test(buffer[i])) {
                i += 1;
            } else {
                swapRemove(i);
            }
        }
    }

    private void swapRemove(int i) {
        assert i < size;
        size--;
        Move tmp = buffer[i];
        buffer[i] = buffer[size];
        buffer[size] = tmp;
    }
}
