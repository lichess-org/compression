package org.lichess.compression.game;

import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;

import org.lichess.compression.BitReader;
import org.lichess.compression.BitWriter;

public class Encoder {
    private static final ThreadLocal<MoveList> moveList = new ThreadLocal<MoveList>() {
        @Override
        protected MoveList initialValue() {
            return new MoveList();
        }
    };

    private final OpeningTrie openingTrie = OpeningTrie.mostCommonOpenings();

    public byte[] encode(String pgnMoves[]) {
        BitWriter writer = new BitWriter();

        Optional<String> longestCommonOpening = openingTrie.findLongestCommonOpening(pgnMoves);
        longestCommonOpening.ifPresentOrElse(
                opening -> {
                    writer.writeBits(1, 1);
                    writer.writeBits(openingTrie.get(opening), openingTrie.getBitVectorLength());
                },
                () -> writer.writeBits(0, 1));

        long numPliesLongestCommonOpening = longestCommonOpening
                .map(opening -> opening
                        .chars()
                        .filter(c -> c == ' ')
                        .count())
                .orElse(0L);

        Board board = new Board();
        MoveList legals = moveList.get();

        for (int ply = 0; ply < pgnMoves.length; ply++) {
            String pgnMove = pgnMoves[ply];
            // Parse SAN.
            SAN sanMove = SANParser.parse(pgnMove, board);
            if (sanMove == null) return null;

            // Find index in legal moves.
            board.legalMoves(legals);
            legals.sort();
            
            OptionalInt correctIndex = findSanInLegalMoves(sanMove, legals);
            if (correctIndex.isPresent()) {
                int i = correctIndex.getAsInt();
                Move legal = legals.get(i);
                if (ply >= numPliesLongestCommonOpening) Huffman.write(i, writer);
                board.play(legal);
            }
            else {
                return null;
            }
        }

        return writer.toArray();
    }
    
    private OptionalInt findSanInLegalMoves(SAN sanMove, MoveList legals) {
        boolean foundMatch = false;
        int size = legals.size();
        OptionalInt correctIndex = OptionalInt.empty();
        for (int i = 0; i < size; i++) {
            Move legal = legals.get(i);
            if (legal.role == sanMove.role() && legal.to == sanMove.to() && legal.promotion == sanMove.promotion() && Bitboard.contains(sanMove.from(), legal.from)) {
                if (!foundMatch) {
                    // Encode and play.
                    foundMatch = true;
                    correctIndex = OptionalInt.of(i);
                }
                else return OptionalInt.empty();
            }
        }
        return correctIndex;
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
        
        long numPliesDecodedOpening = 0L;

        if (reader.readBits(1) == 1) {
            int code = reader.readBits(openingTrie.getBitVectorLength());
            Optional<String> decodedOpening = openingTrie.getFirstOpeningMappingToCode(code);
            decodedOpening.ifPresent(opening -> {
               String moves[] = opening.split(" ");
               for (int i = 0; i < Math.min(plies, moves.length); i++) {
                   output[i] = moves[i];
               }
            });
            numPliesDecodedOpening = decodedOpening
                    .map(opening -> opening
                            .chars()
                            .filter(c -> c == ' ')
                            .count())
                    .orElse(0L);
        }

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
                Move move;
                if (i >= numPliesDecodedOpening) {
                    move = legals.get(Huffman.read(reader));
                    output[i] = move.san(legals);
                }
                else {
                    SAN sanMove = SANParser.parse(output[i], board);
                    int correctIndex = findSanInLegalMoves(sanMove, legals).getAsInt();
                    move = legals.get(correctIndex);
                }
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
