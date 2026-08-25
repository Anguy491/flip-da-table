package com.flip.backend.lasvegas.entities;

import com.flip.backend.game.entities.Card;

/** A single Las Vegas Royale money card, stored as whole dollars. */
public final class LasVegasMoneyCard extends Card {
    private final int amount;

    public LasVegasMoneyCard(int amount) {
        if (amount < 30_000 || amount > 100_000 || amount % 10_000 != 0) {
            throw new IllegalArgumentException("unsupported money card amount");
        }
        this.amount = amount;
    }

    public int amount() {
        return amount;
    }

    @Override
    public String getDisplay() {
        return "$" + amount;
    }
}
