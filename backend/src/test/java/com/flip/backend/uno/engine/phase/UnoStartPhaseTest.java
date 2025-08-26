package com.flip.backend.uno.engine.phase;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import com.flip.backend.uno.entities.*;

public class UnoStartPhaseTest {
    @Test
    void initializationSetsHandsAndBoardAndDeck() {
        List<String> ids = List.of("P1","BOT_X","P2");
        UnoStartPhase phase = new UnoStartPhase(ids);
        phase.enter();
        assertNotNull(phase.deck());
        assertNotNull(phase.board());
        assertEquals(3, phase.players().size());
        // each hand has 7
        phase.players().forEach(p -> assertEquals(7, p.cardCount()));
        // deck remaining = 108 - players*7 - 1 (starter)
        int expectedRemaining = 108 - (3*7) - 1;
        assertEquals(expectedRemaining, phase.deck().remaining());
        // board current is first id
        assertEquals("P1", phase.board().currentPlayer().getId());
        // starter card must not be wild color
        assertNotNull(phase.board().lastPlayedCard());
        assertNotEquals(UnoCard.Color.WILD, phase.board().lastPlayedCard().getColor());
    }

    @Test
    void activeColorMatchesStarterAndBotFlag() {
        UnoStartPhase phase = new UnoStartPhase(List.of("A","BOT_1","B"));
        phase.enter();
        var starter = phase.board().lastPlayedCard();
        assertEquals(starter.getColor(), phase.board().activeColor());
        // bot flag
        var bot = phase.players().stream().filter(p -> p.getId().equals("BOT_1")).findFirst().orElseThrow();
        assertTrue(bot.isBot());
    }

    @Test
    void totalCardAccounting() {
        UnoStartPhase phase = new UnoStartPhase(List.of("P1","P2","P3","BOT_Z"));
        phase.enter();
        int hands = phase.players().stream().mapToInt(UnoPlayer::cardCount).sum();
        int remaining = phase.deck().remaining();
        int discards = phase.deck().discards();
        assertEquals(108, hands + remaining + discards);
        assertEquals(1, discards); // only starter
    }

    @Test
    void boardNavigationAfterStart() {
        UnoStartPhase phase = new UnoStartPhase(List.of("P1","BOT_X","P2"));
        phase.enter();
        assertEquals("P1", phase.board().currentPlayer().getId());
        phase.board().step(1); // bot
        assertEquals("BOT_X", phase.board().currentPlayer().getId());
        phase.board().step(1); // P2
        assertEquals("P2", phase.board().currentPlayer().getId());
        phase.board().reverse();
        phase.board().step(1); // back to BOT_X
        assertEquals("BOT_X", phase.board().currentPlayer().getId());
    }

    @Test
    void simulatePlayerPlaysCardAndDiscardUpdatesBoard() {
        UnoStartPhase phase = new UnoStartPhase(List.of("P1","P2"));
        phase.enter();
        UnoPlayer current = phase.players().get(0); // P1
        int before = current.cardCount();
        UnoCard card = current.getHand().view().get(0);
        assertTrue(current.playCard(card));
        phase.deck().discard(card);
        phase.board().applyTop(card, null);
        assertEquals(before - 1, current.cardCount());
        assertEquals(card, phase.board().lastPlayedCard());
        if (card.getColor() != UnoCard.Color.WILD) {
            assertEquals(card.getColor(), phase.board().activeColor());
        }
    }
}
