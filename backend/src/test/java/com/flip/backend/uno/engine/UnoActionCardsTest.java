package com.flip.backend.uno.engine;

import com.flip.backend.uno.engine.event.UnoPlayCardEvent;
import com.flip.backend.uno.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused tests for UNO action card effects integrated in UnoPlayCardEvent.
 * These tests inject controlled hands / deck state to ensure deterministic outcomes.
 */
public class UnoActionCardsTest {

    private UnoDeck deck;
    private UnoBoard board;
    private UnoPlayer p1, p2, p3;

    @BeforeEach
    void setup() {
        deck = new UnoDeck();
        deck.initialize();
        p1 = new UnoPlayer("P1");
        p2 = new UnoPlayer("P2");
        p3 = new UnoPlayer("P3");
        board = new UnoBoard(java.util.List.of(p1, p2, p3));
        // Set a neutral starting card (e.g., Red 5)
        UnoCard starter = UnoCard.number(UnoCard.Color.RED, 5);
        board.applyTop(starter, null); // active color RED
    }

    @Test
    void skipCardSkipsNextPlayer() {
        // p1 plays Red Skip -> p2 skipped -> p3 becomes current after effect
        p1.giveCard(UnoCard.skip(UnoCard.Color.RED));
        UnoPlayCardEvent ev = new UnoPlayCardEvent(board, deck, p1, p1.getHand().view().get(0));
        assertTrue(ev.isValid());
        ev.execute();
        assertEquals(p1, board.currentPlayer()); // current pointer not advanced by event itself
        int advance = ev.getAdvanceSteps();
        // simulate advancement like runtime would do
        board.step(advance); // should land on p3 (skip p2)
        assertEquals(p3, board.currentPlayer());
    }

    @Test
    void reverseCardChangesDirection() {
        p1.giveCard(UnoCard.reverse(UnoCard.Color.RED));
        int beforeDir = board.direction();
        UnoPlayCardEvent ev = new UnoPlayCardEvent(board, deck, p1, p1.getHand().view().get(0));
        assertTrue(ev.isValid());
        ev.execute();
        assertEquals(-beforeDir, board.direction());
        assertEquals(1, ev.getAdvanceSteps());
    }

    @Test
    void drawTwoForcesNextPlayerToDrawAndSkip() {
        p1.giveCard(UnoCard.drawTwo(UnoCard.Color.RED));
        int p2HandBefore = p2.getHand().size();
        UnoPlayCardEvent ev = new UnoPlayCardEvent(board, deck, p1, p1.getHand().view().get(0));
        ev.execute();
        assertEquals(p2HandBefore + 2, p2.getHand().size());
        assertEquals(2, ev.getAdvanceSteps()); // skip p2 after drawing -> move to p3
    }
}
