package com.flip.backend.uno.entities;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UnoHandTest {
    @Test
    void addAndRemoveCards() {
        UnoHand hand = new UnoHand();
        assertEquals(0, hand.size());
        UnoCard r5 = UnoCard.number(UnoCard.Color.RED,5);
        UnoCard g9 = UnoCard.number(UnoCard.Color.GREEN,9);
        hand.add(r5);
        hand.add(g9);
        assertEquals(2, hand.size());
        assertTrue(hand.remove(r5));
        assertEquals(1, hand.size());
        assertFalse(hand.remove(r5));
        assertEquals(1, hand.size());
        assertTrue(hand.removeByDisplay("GREEN 9"));
        assertEquals(0, hand.size());
    }
}
