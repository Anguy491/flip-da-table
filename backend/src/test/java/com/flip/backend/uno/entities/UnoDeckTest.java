package com.flip.backend.uno.entities;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UnoDeckTest {
    @Test
    void deckInitializationHas108Cards() {
        UnoDeck deck = new UnoDeck();
        deck.initialize();
        assertEquals(108, deck.total(), "UNO deck must have 108 cards");
        assertEquals(108, deck.remaining());
    }

    @Test
    void drawingReducesRemaining() {
        UnoDeck deck = new UnoDeck();
        deck.initialize();
        deck.draw();
    assertEquals(107, deck.remaining());
    assertEquals(0, deck.discards());
    }

    @Test
    void discardThenReshuffle() {
        UnoDeck deck = new UnoDeck();
        deck.initialize();
        // draw all cards into hand and discard them except keep one each loop
        for (int i = 0; i < 108; i++) {
            UnoCard c = deck.draw();
            assertNotNull(c);
            deck.discard(c);
        }
        assertEquals(0, deck.remaining());
        assertTrue(deck.discards() > 0);
        // next draw should reshuffle keeping top discard
        UnoCard beforeTop = deck.draw();
        assertNotNull(beforeTop);
        assertTrue(deck.remaining() > 0, "Deck should have been reshuffled");
    }
}
