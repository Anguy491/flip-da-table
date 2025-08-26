package com.flip.backend.uno.entities;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class UnoBoardTest {

    private UnoBoard make(List<String> ids) {
        return new UnoBoard(ids.stream().map(UnoPlayer::new).toList());
    }

    @Test
    void topAndActiveColorMaintenance() {
        UnoBoard b = make(List.of("A","B","C"));
        UnoCard red5 = UnoCard.number(UnoCard.Color.RED,5);
        b.applyTop(red5, null);
        assertEquals(red5, b.lastPlayedCard());
        assertEquals(UnoCard.Color.RED, b.activeColor());
        UnoCard wild = UnoCard.wild();
        b.applyTop(wild, UnoCard.Color.BLUE);
        assertEquals(wild, b.lastPlayedCard());
        assertEquals(UnoCard.Color.BLUE, b.activeColor());
        UnoCard blue9 = UnoCard.number(UnoCard.Color.BLUE,9);
        b.applyTop(blue9, null);
        assertEquals(UnoCard.Color.BLUE, b.activeColor());
    }

    @Test
    void independenceFromSeatOps() {
        UnoBoard b = make(List.of("A","B","C","D"));
        b.applyTop(UnoCard.number(UnoCard.Color.GREEN,7), null);
        long tc = b.turnCount();
        UnoCard last = b.lastPlayedCard();
        b.step(1); b.reverse(); b.step(2); b.tickTurn();
        assertEquals(last, b.lastPlayedCard());
        assertEquals(tc+1, b.turnCount());
    }

    @Test
    void idempotentSetters() {
        UnoBoard b = make(List.of("A","B"));
        UnoCard wild = UnoCard.wild();
        b.applyTop(wild, UnoCard.Color.YELLOW);
        b.setLastPlayedCard(wild); // repeat
        assertEquals(wild, b.lastPlayedCard());
        assertEquals(UnoCard.Color.YELLOW, b.activeColor());
    }
}
