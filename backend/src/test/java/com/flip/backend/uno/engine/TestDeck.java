package com.flip.backend.uno.engine;

import com.flip.backend.uno.entities.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Test-specific deck with predictable card sequence for E2E testing.
 * Designed to trigger specific game scenarios including action cards, reshuffling, etc.
 */
public class TestDeck extends UnoDeck {
    private final List<UnoCard> predefinedCards;

    public TestDeck(List<UnoCard> cards) {
        this.predefinedCards = new ArrayList<>(cards);
    }

    @Override
    protected List<UnoCard> buildInitialCards() {
        // Return our predefined sequence - initialize() will handle shuffling behavior
        return new ArrayList<>(predefinedCards);
    }

    /**
     * Builder for creating specific test scenarios
     */
    public static class Builder {
        private final List<UnoCard> cards = new ArrayList<>();

        public Builder addCard(UnoCard card) {
            cards.add(card);
            return this;
        }

        public Builder addCards(UnoCard... cards) {
            for (UnoCard card : cards) {
                this.cards.add(card);
            }
            return this;
        }

        // Helper methods for common patterns
        public Builder addNumberCards(UnoCard.Color color, int count) {
            for (int i = 0; i < count; i++) {
                cards.add(UnoCard.number(color, i % 10));
            }
            return this;
        }

        public Builder addActionCard(UnoCard.Type type, UnoCard.Color color) {
            switch (type) {
                case SKIP -> cards.add(UnoCard.skip(color));
                case REVERSE -> cards.add(UnoCard.reverse(color));
                case DRAW_TWO -> cards.add(UnoCard.drawTwo(color));
                case WILD -> cards.add(UnoCard.wild());
                case WILD_DRAW_FOUR -> cards.add(UnoCard.wildDrawFour());
                case NUMBER -> throw new IllegalArgumentException("Use addNumberCards for number cards");
            }
            return this;
        }

        public TestDeck build() {
            if (cards.size() < 50) {
                // Pad with basic number cards to ensure enough for gameplay
                while (cards.size() < 80) {
                    UnoCard.Color color = UnoCard.Color.values()[cards.size() % 4];
                    cards.add(UnoCard.number(color, cards.size() % 10));
                }
            }
            return new TestDeck(cards);
        }
    }
}
