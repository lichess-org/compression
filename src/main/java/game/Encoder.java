package org.lichess.compression.game;

import org.lichess.compression.BitReader;
import org.lichess.compression.BitWriter;
import org.lichess.compression.game.codec.Rans;

public class Encoder {
    private final Rans rans = new Rans();

    private static final ThreadLocal<MoveList> moveList = new ThreadLocal<MoveList>() {
        @Override
        protected MoveList initialValue() {
            return new MoveList();
        }
    };

    private static Pattern SAN_PATTERN = Pattern.compile(
        "([NBKRQ])?([a-h])?([1-8])?x?([a-h][1-8])(?:=([NBRQK]))?[\\+#]?");

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

    private static int[] getMoveIndexes(String[] pgnMoves) {
        Board board = new Board();
        MoveList legals = moveList.get();

        int[] moveIndexes = new int[pgnMoves.length];

        for (int ply = 0; ply < pgnMoves.length; ply++) {
            String pgnMove = pgnMoves[ply];
            Lan current = parseSan(pgnMove, board);

            // Find index in legal moves.
            board.legalMoves(legals);
            legals.sort();

            boolean foundMatch = false;
            int size = legals.size();

            for (int i = 0; i < size; i++) {
                Move legal = legals.get(i);
                if (legal.role == current.role && legal.to == current.to && legal.promotion == current.promotion && Bitboard.contains(current.from, legal.from)) {
                    if (!foundMatch) {
                        // Save and play.
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

    private static Lan parseSan(String pgnMove, Board board) {
        Role role;
        Role promotion = null;
        long from = Bitboard.ALL;
        int to;

        if (pgnMove.startsWith("O-O-O")) {
            role = Role.KING;
            from = board.kings;
            to = Bitboard.lsb(board.rooks & Bitboard.RANKS[board.turn ? 0 : 7]);
        } else if (pgnMove.startsWith("O-O")) {
            role = Role.KING;
            from = board.kings;
            to = Bitboard.msb(board.rooks & Bitboard.RANKS[board.turn ? 0 : 7]);
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
        return new Lan(role, promotion, from, to);
    }

    public EncodeResult encode(String[] pgnMoves) {
        BitWriter writer = new BitWriter();
        int[] moveIndexes = getMoveIndexes(pgnMoves);
        if (moveIndexes == null) {
            return null;
        }
        rans.resetEncoder();
        for (int i = moveIndexes.length - 1; i >= 0; i--) {
            int moveIndex = moveIndexes[i];
            rans.write(moveIndex, writer);
        }
        byte[] encoded = writer.toArray();
        byte[] encodedReversed = new byte[encoded.length];
        for (int i = 0; i < encoded.length; i++) {
            encodedReversed[i] = encoded[encoded.length - (i + 1)];
        }
        return new EncodeResult(encodedReversed, rans.getState());
    }

    public DecodeResult decode(EncodeResult input, int plies) {
        BitReader reader = new BitReader(input.code);

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

        rans.initializeDecoder(input.state);

        for (int i = 0; i <= plies; i++) {
            if (0 < i || i < plies) board.legalMoves(legals);

            // Append check or checkmate suffix to previous move.
            if (0 < i) {
                if (board.isCheck()) output[i - 1] += (legals.isEmpty() ? "#" : "+");
            }

            // Decode and play next move.
            if (i < plies) {
                legals.sort();
                Move move = legals.get(rans.read(reader));
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
            board,
            plies - 1 - lastZeroingPly,
            Arrays.copyOf(positionHashes, 3 * (plies - lastIrreversiblePly)),
            lastUci);
    }

    public static class EncodeResult {
        public final byte[] code;
        public final int state;

        public EncodeResult(byte[] code, int state) {
            this.code = code;
            this.state = state;
        }
    }

    public static class DecodeResult {
        public final String[] pgnMoves;
        public final Board board;
        public final int halfMoveClock;
        public final byte[] positionHashes;
        public final String lastUci;

        public DecodeResult(String[] pgnMoves, Board board, int halfMoveClock, byte[] positionHashes, String lastUci) {
            this.pgnMoves = pgnMoves;
            this.board = board;
            this.halfMoveClock = halfMoveClock;
            this.positionHashes = positionHashes;
            this.lastUci = lastUci;
        }
    }

    public static class Lan {
        public final Role role;
        public final Role promotion;
        public long from;
        public int to;

        public Lan(Role role, Role promotion, long from, int to) {
            this.role = role;
            this.promotion = promotion;
            this.from = from;
            this.to = to;
        }
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
