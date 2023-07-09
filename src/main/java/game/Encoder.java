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

    private static final Pattern SAN_PATTERN = Pattern.compile(
            "([NBKRQ])?([a-h])?([1-8])?x?([a-h][1-8])(?:=([NBRQK]))?[\\+#]?");
    private final int numberOfQuantizationBits = 14;
    private final int numberOfNormalizationBits = 16;
    private final RansConfiguration ransConfiguration = new RansConfiguration(numberOfQuantizationBits, numberOfNormalizationBits);
    private static final int[] MOVE_INDEX_FREQUENCIES = {225883932, 134956126, 89041269, 69386238, 57040790, 44974559, 36547155, 31624920, 28432772, 26540493, 24484873, 23058034, 23535272, 20482457, 20450172, 18316057, 17214833, 16964761, 16530028, 15369510, 14178440, 14275714, 13353306, 12829602, 13102592, 11932647, 10608657, 10142459, 8294594, 7337490, 6337744, 5380717, 4560556, 3913313, 3038767, 2480514, 1951026, 1521451, 1183252, 938708, 673339, 513153, 377299, 276996, 199682, 144602, 103313, 73046, 52339, 36779, 26341, 18719, 13225, 9392, 6945, 4893, 3698, 2763, 2114, 1631, 1380, 1090, 887, 715, 590, 549, 477, 388, 351, 319, 262, 236, 200, 210, 153, 117, 121, 121, 115, 95, 75, 67, 55, 50, 55, 33, 33, 30, 32, 28, 29, 27, 21, 15, 9, 10, 12, 12, 8, 7, 2, 4, 5, 5, 1, 5, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
    private final Rans rans = new Rans(ransConfiguration, MOVE_INDEX_FREQUENCIES);
    private final Huffman huffman = new Huffman();
    private static Role charToRole(char c) {
        switch (c) {
            case 'N': return Role.KNIGHT;
            case 'B': return Role.BISHOP;
            case 'R': return Role.ROOK;
            case 'Q': return Role.QUEEN;
            case 'K': return Role.KING;
            default: throw new IllegalArgumentException();
        }
    }
    public static class EncodeResult {
        public final int ransState;
        public final byte[] encodedMoveIndexes;

        public EncodeResult(int ransState, byte[] encodedMoveIndexes) {
            this.ransState = ransState;
            this.encodedMoveIndexes = encodedMoveIndexes;
        }
    }
    public EncodeResult encodeWithRans(String pgnMoves[]) {
        rans.resetEncoder();
        int[] moveIndexes = findMoveIndexes(pgnMoves);
        int[] reversedMoveIndexes = IntStream.rangeClosed(1, moveIndexes.length).map(i -> moveIndexes[moveIndexes.length-i]).toArray();
        byte[] encodedMoveIndexes = encode(reversedMoveIndexes, rans);
        byte[] encodedMoveIndexesReversed = new byte[encodedMoveIndexes.length];
        for (int i = 0; i < encodedMoveIndexesReversed.length; i++) {
            encodedMoveIndexesReversed[i] = encodedMoveIndexes[encodedMoveIndexes.length - (i + 1)];
        }
        int ransState = rans.getState();
        return new EncodeResult(ransState, encodedMoveIndexesReversed);
    }
    public byte[] encodeWithHuffman(String pgnMoves[]) {
        int[] moveIndexes = findMoveIndexes(pgnMoves);
        return encode(moveIndexes, huffman);
    }

    private byte[] encode(int[] moveIndexes, Compression encoder) {
        BitWriter writer = new BitWriter();
        for (int moveIndex: moveIndexes) {
            encoder.write(moveIndex, writer);
        }
        return writer.toArray();
    }
    private static int[] findMoveIndexes(String[] pgnMoves) {
        int[] moveIndexes = new int[pgnMoves.length];
        Board board = new Board();
        MoveList legals = moveList.get();

        for (int ply = 0; ply < pgnMoves.length; ply++) {
            String pgnMove = pgnMoves[ply];
            LAN played = parseSAN(pgnMove, board);
            board.legalMoves(legals);
            legals.sort();
            boolean foundMatch = false;
            int size = legals.size();
            for (int i = 0; i < size; i++) {
                Move legal = legals.get(i);
                if (legal.role == played.role && legal.to == played.to && legal.promotion == played.promotion && Bitboard.contains(played.from, legal.from)) {
                    if (!foundMatch) {
                        moveIndexes[ply] = i;
                        board.play(legal);
                        foundMatch = true;
                    } else return null;
                }
            }

            if (!foundMatch) return null;
        }
        return moveIndexes;
    }

    private static LAN parseSAN(String pgnMove, Board board) {
        Role role = null, promotion = null;
        long from = Bitboard.ALL;
        int to;

        if (pgnMove.startsWith("O-O-O")) {
            role = Role.KING;
            from = board.kings;
            to = Bitboard.lsb(board.rooks & Bitboard.RANKS[board.turn ? 0 : 7]);
        } else if (pgnMove.startsWith("O-O")) {
            role = Role.KING;
            from = board.kings;
            to = Bitboard.msb(board.rooks & Bitboard.RANKS[board.turn ?  0 : 7]);
        } else {
            Matcher matcher = SAN_PATTERN.matcher(pgnMove);
            if (!matcher.matches()) return null;

            String roleStr = matcher.group(1);
            role = roleStr == null ? Role.PAWN : charToRole(roleStr.charAt(0));

            if (matcher.group(2) != null) from &= Bitboard.FILES[matcher.group(2).charAt(0) - 'a'];
            if (matcher.group(3) != null) from &= Bitboard.RANKS[matcher.group(3).charAt(0) - '1'];

            to = Square.square(matcher.group(4).charAt(0) - 'a', matcher.group(4).charAt(1) - '1');

            if (matcher.group(5) != null) {
                promotion = charToRole(matcher.group(5).charAt(0));
            }
        }
        return new LAN(role, promotion, from, to);
    }

    private static class LAN {
        public final Role role;
        public final Role promotion;
        public final long from;
        public final int to;

        public LAN(Role role, Role promotion, long from, int to) {
            this.role = role;
            this.promotion = promotion;
            this.from = from;
            this.to = to;
        }
    }

    public static class DecodeResult {
        public final String pgnMoves[];
        public final Map<Integer, Piece> pieces;
        public final Set<Integer> unmovedRooks;
        public final int halfMoveClock;
        public final byte positionHashes[];
        public final String lastUci;

        public DecodeResult(String pgnMoves[], Map<Integer, Piece> pieces, Set<Integer> unmovedRooks, int halfMoveClock, byte positionHashes[], String lastUci) {
            this.pgnMoves = pgnMoves;
            this.pieces = pieces;
            this.unmovedRooks = unmovedRooks;
            this.halfMoveClock = halfMoveClock;
            this.positionHashes = positionHashes;
            this.lastUci = lastUci;
        }
    }
    public DecodeResult decodeWithRans(EncodeResult input, int plies) {
        rans.initializeDecoder(input.ransState);
        return decode(input.encodedMoveIndexes, plies, rans);
    }
    public DecodeResult decodeWithHuffman(byte input[], int plies) {
        return decode(input, plies, huffman);
    }
    private DecodeResult decode(byte input[], int plies, Compression decoder) {
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
                Move move = legals.get(decoder.read(reader));
                output[i] = san(move, legals);
                board.play(move);

                if (move.isZeroing()) lastZeroingPly = i;
                if (move.isIrreversible()) lastIrreversiblePly = i;
                setHash(positionHashes, i, board.zobristHash());

                if (i + 1 == plies) lastUci = move.uci();
            }
        }

        return new DecodeResult(
                output,
                board.pieceMap(),
                Bitboard.squareSet(board.castlingRights),
                plies - 1 - lastZeroingPly,
                Arrays.copyOf(positionHashes, 3 * (plies - lastIrreversiblePly)),
                lastUci);
    }

    private static String san(Move move, MoveList legals) {
        switch (move.type) {
            case Move.NORMAL:
            case Move.EN_PASSANT:
                StringBuilder builder = new StringBuilder(6);
                builder.append(move.role.symbol);

                // From.
                if (move.role != Role.PAWN) {
                    boolean file = false, rank = false;
                    long others = 0;

                    for (int i = 0; i < legals.size(); i++) {
                        Move other = legals.get(i);
                        if (other.role == move.role && other.to == move.to && other.from != move.from) {
                            others |= 1L << other.from;
                        }
                    }

                    if (others != 0) {
                        if ((others & Bitboard.RANKS[Square.rank(move.from)]) != 0) file = true;
                        if ((others & Bitboard.FILES[Square.file(move.from)]) != 0) rank = true;
                        else file = true;
                    }

                    if (file) builder.append((char) (Square.file(move.from) + 'a'));
                    if (rank) builder.append((char) (Square.rank(move.from) + '1'));
                } else if (move.capture) {
                    builder.append((char) (Square.file(move.from) + 'a'));
                }

                // Capture.
                if (move.capture) builder.append('x');

                // To.
                builder.append((char) (Square.file(move.to) + 'a'));
                builder.append((char) (Square.rank(move.to) + '1'));

                // Promotion.
                if (move.promotion != null) {
                    builder.append('=');
                    builder.append(move.promotion.symbol);
                }

                return builder.toString();

            case Move.CASTLING:
                return move.from < move.to ? "O-O" : "O-O-O";
        }

        return "--";
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
