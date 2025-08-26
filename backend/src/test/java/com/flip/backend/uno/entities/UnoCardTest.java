package com.flip.backend.uno.entities;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UnoCardTest {
    @Test
    void createNumberCard() {
        UnoCard c = UnoCard.number(UnoCard.Color.RED, 5);
        assertEquals(UnoCard.Color.RED, c.getColor());
        assertEquals(UnoCard.Type.NUMBER, c.getType());
        assertEquals(5, c.getNumber());
        assertEquals("RED 5", c.getDisplay());
    }

    @Test
    void createActionCards() {
        assertEquals(UnoCard.Type.SKIP, UnoCard.skip(UnoCard.Color.BLUE).getType());
        assertEquals(UnoCard.Type.REVERSE, UnoCard.reverse(UnoCard.Color.GREEN).getType());
        assertEquals(UnoCard.Type.DRAW_TWO, UnoCard.drawTwo(UnoCard.Color.YELLOW).getType());
        assertEquals(UnoCard.Type.WILD, UnoCard.wild().getType());
        assertEquals(UnoCard.Type.WILD_DRAW_FOUR, UnoCard.wildDrawFour().getType());
    }

    @Test
    void invalidNumberCard() {
        assertThrows(IllegalArgumentException.class, () -> UnoCard.number(UnoCard.Color.WILD, 3));
        assertThrows(IllegalArgumentException.class, () -> UnoCard.number(UnoCard.Color.RED, 10));
    }
}
