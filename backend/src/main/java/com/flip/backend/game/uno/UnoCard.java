package com.flip.backend.game.uno;

import java.util.Objects;

/** Immutable card: color may be null for wild cards. */
public record UnoCard(UnoColor color, UnoValue value) {
    public UnoCard {
        Objects.requireNonNull(value, "value");
        if (isWild() && color != null) {
            throw new IllegalArgumentException("Wild card color must be null initially");
        }
        if (!isWild() && color == null) {
            throw new IllegalArgumentException("Non wild card needs color");
        }
    }

    public boolean isWild() {
        return value == UnoValue.WILD || value == UnoValue.WILD_DRAW_FOUR;
    }
}
