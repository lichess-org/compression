package org.lichess.compression.game;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameToMoveIndexesConverter
{
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

    public static int[] convert(String[] pgnMoves) {
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
}
