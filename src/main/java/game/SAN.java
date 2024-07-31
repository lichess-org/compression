package org.lichess.compression.game;

public record SAN(Role role, long from, int to, Role promotion) {
}
