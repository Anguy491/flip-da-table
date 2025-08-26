package com.flip.backend.uno.entities;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UnoHandImmutabilityTest {
    @Test
    void viewIsUnmodifiable() {
        UnoHand hand = new UnoHand();
        hand.add(UnoCard.number(UnoCard.Color.RED,1));
        var view = hand.view();
        assertThrows(UnsupportedOperationException.class, () -> view.add(UnoCard.number(UnoCard.Color.BLUE,2)));
        assertEquals(1, hand.size());
    }
}
