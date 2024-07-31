package org.lichess.compression.game;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.nio.ByteBuffer;

import org.lichess.compression.BitReader;
import org.lichess.compression.BitWriter;

public class Encoder {
    private static final ThreadLocal<MoveList> moveList = new ThreadLocal<MoveList>() {
        @Override
        protected MoveList initialValue() {
            return new MoveList();
        }
    };

    public byte[] encode(String pgnMoves[]) {
        BitWriter writer = new BitWriter();

        Board board = new Board();
        MoveList legals = moveList.get();

        for (String pgnMove: pgnMoves) {
            // Parse SAN.
            SAN sanMove = SANParser.parse(pgnMove, board);
            if (sanMove == null) return null;

            // Find index in legal moves.
            board.legalMoves(legals);
            legals.sort();

            boolean foundMatch = false;
            int size = legals.size();

            for (int i = 0; i < size; i++) {
                Move legal = legals.get(i);
                if (legal.role == sanMove.role() && legal.to == sanMove.to() && legal.promotion == sanMove.promotion() && Bitboard.contains(sanMove.from(), legal.from)) {
                    if (!foundMatch) {
                        // Encode and play.
                        Huffman.write(i, writer);
                        board.play(legal);
                        foundMatch = true;
                    }
                    else return null;
                }
            }

            if (!foundMatch) return null;
        }

        return writer.toArray();
    }

    public static class DecodeResult {
        public final String pgnMoves[];
        public final Board board;
        public final int halfMoveClock;
        public final byte positionHashes[];
        public final String lastUci;

        public DecodeResult(String pgnMoves[], Board board, int halfMoveClock, byte positionHashes[], String lastUci) {
            this.pgnMoves = pgnMoves;
            this.board = board;
            this.halfMoveClock = halfMoveClock;
            this.positionHashes = positionHashes;
            this.lastUci = lastUci;
        }
    }

    public DecodeResult decode(byte input[], int plies) {
        BitReader reader = new BitReader(input);

        String output[] = new String[plies];

        Board board = new Board();
        MoveList legals = moveList.get();

        String lastUci = null;

        // Collect the position hashes (3 bytes each) since the last capture
        // or pawn move.
        int lastZeroingPly = -1;
        int lastIrreversiblePly = -1;
        byte positionHashes[] = new byte[3 * (plies + 1)];
        setHash(positionHashes, -1, board.zobristHash());

        for (int i = 0; i <= plies; i++) {
            if (0 < i || i < plies) board.legalMoves(legals);

            // Append check or checkmate suffix to previous move.
            if (0 < i) {
                if (board.isCheck()) output[i - 1] += (legals.isEmpty() ? "#" : "+");
            }

            // Decode and play next move.
            if (i < plies) {
                legals.sort();
                Move move = legals.get(Huffman.read(reader));
                output[i] = move.san(legals);
                board.play(move);

                if (move.isZeroing()) lastZeroingPly = i;
                if (move.isIrreversible()) lastIrreversiblePly = i;
                setHash(positionHashes, i, board.zobristHash());

                if (i + 1 == plies) lastUci = move.uci();
            }
        }

        return new DecodeResult(
            output,
            board,
            plies - 1 - lastZeroingPly,
            Arrays.copyOf(positionHashes, 3 * (plies - lastIrreversiblePly)),
            lastUci);
    }

    private static void setHash(byte buffer[], int ply, int hash) {
        // The hash for the starting position (ply = -1) goes last. The most
        // recent position goes first.
        int base = buffer.length - 3 * (ply + 1 + 1);
        buffer[base] = (byte) (hash >>> 16);
        buffer[base + 1] = (byte) (hash >>> 8);
        buffer[base + 2] = (byte) hash;
    }
}
