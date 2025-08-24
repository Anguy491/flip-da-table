package com.flip.backend.game.uno;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** Immutable card: color may be null for wild cards. */
public record UnoCard(UnoColor color, UnoValue value) {
    public UnoCard {
        // allow null color for any card (validation done in engine)
    }
    @JsonIgnore
    public boolean isWild() { return value == UnoValue.WILD || value == UnoValue.WILD_DRAW_FOUR; }
}
