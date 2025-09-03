package com.flip.backend.dvc.entities;

import java.util.ArrayList;
import java.util.List;
import com.flip.backend.game.entities.Deck;

/**
 * DVC deck: 2 colors (BLACK, WHITE); each color has numbers 0-11 plus one Joker (hyphen card) ⇒ 13 × 2 = 26 cards.
 */
public class DVCDeck extends Deck<DVCCard> {

    @Override
    protected List<DVCCard> buildInitialCards() {
        List<DVCCard> list = new ArrayList<>(26);
        for (DVCCard.Color c : DVCCard.Color.values()) {
            for (int n = 0; n <= 11; n++) list.add(DVCCard.number(c, n));
            list.add(DVCCard.joker(c));
        }
        return list;
    }
}
