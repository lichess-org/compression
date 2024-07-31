package org.lichess.compression.game;

public class CharToRoleConverter {
    public static Role convert(char c) {
        switch (c) {
            case 'N': return Role.KNIGHT;
            case 'B': return Role.BISHOP;
            case 'R': return Role.ROOK;
            case 'Q': return Role.QUEEN;
            case 'K': return Role.KING;
            default: throw new IllegalArgumentException();
        }
    }
}
