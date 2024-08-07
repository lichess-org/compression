package org.lichess.compression.game;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SANParser {
    private static final Pattern SAN_PATTERN = Pattern.compile(
            "([NBKRQ])?([a-h])?([1-8])?x?([a-h][1-8])(?:=([NBRQK]))?[\\+#]?");
    
    public static SAN parse(String pgnMove, Board board) {
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
            role = roleStr == null ? Role.PAWN : CharToRoleConverter.convert(roleStr.charAt(0));

            if (matcher.group(2) != null) from &= Bitboard.FILES[matcher.group(2).charAt(0) - 'a'];
            if (matcher.group(3) != null) from &= Bitboard.RANKS[matcher.group(3).charAt(0) - '1'];

            to = Square.square(matcher.group(4).charAt(0) - 'a', matcher.group(4).charAt(1) - '1');

            if (matcher.group(5) != null) {
                promotion = CharToRoleConverter.convert(matcher.group(5).charAt(0));
            }
        }
        return new SAN(role, from, to, promotion);
    }
}
