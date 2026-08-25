package com.flip.backend.lasvegas.entities;

import com.flip.backend.game.entities.Deck;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** The 90-card money deck used by Las Vegas Royale basic mode. */
public final class LasVegasMoneyDeck extends Deck<LasVegasMoneyCard> {
    private static final int[][] COMPOSITION = {
            {30_000, 11},
            {40_000, 11},
            {50_000, 13},
            {60_000, 15},
            {70_000, 13},
            {80_000, 11},
            {90_000, 9},
            {100_000, 7}
    };

    public LasVegasMoneyDeck() {
        super();
    }

    public LasVegasMoneyDeck(Random random) {
        super(random);
    }

    @Override
    protected List<LasVegasMoneyCard> buildInitialCards() {
        var cards = new ArrayList<LasVegasMoneyCard>(90);
        for (int[] entry : COMPOSITION) {
            for (int index = 0; index < entry[1]; index++) {
                cards.add(new LasVegasMoneyCard(entry[0]));
            }
        }
        return cards;
    }

    public List<Integer> snapshotAmounts() {
        return drawPileSnapshot().stream().map(LasVegasMoneyCard::amount).toList();
    }

    public void restoreAmounts(List<Integer> amounts) {
        if (amounts == null) throw new IllegalArgumentException("deck amounts are required");
        restorePiles(amounts.stream().map(LasVegasMoneyCard::new).toList(), List.of());
    }
}
