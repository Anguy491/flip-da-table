package com.flip.backend.uno.entities;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

public class UnoBoardWildTest {

    private UnoBoard board() { return new UnoBoard(List.of(new UnoPlayer("P1"), new UnoPlayer("P2"))); }

    @Test
    void wildColorSelectionOverrides() {
        UnoBoard b = board();
        UnoCard wild = UnoCard.wild();
        b.applyTop(wild, UnoCard.Color.YELLOW);
        assertEquals(UnoCard.Color.YELLOW, b.activeColor());
        // play non-wild sets active color implicitly
        UnoCard g3 = UnoCard.number(UnoCard.Color.GREEN,3);
        b.applyTop(g3, null);
        assertEquals(UnoCard.Color.GREEN, b.activeColor());
    }

    @Test
    void wildDrawFourBehavesSameForColor() {
        UnoBoard b = board();
        UnoCard w4 = UnoCard.wildDrawFour();
        b.applyTop(w4, UnoCard.Color.RED);
        assertEquals(w4, b.lastPlayedCard());
        assertEquals(UnoCard.Color.RED, b.activeColor());
    }

    @Test
    void applyTopNullDoesNotChangeColor() {
        UnoBoard b = board();
        b.applyTop(UnoCard.wild(), UnoCard.Color.BLUE);
        b.applyTop(null, null); // no new card
        assertEquals(UnoCard.Color.BLUE, b.activeColor());
    }
}
