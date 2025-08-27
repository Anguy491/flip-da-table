package com.flip.backend.uno.engine;

import com.flip.backend.uno.engine.phase.UnoStartPhase;
import com.flip.backend.uno.engine.phase.UnoRuntimePhase;
import com.flip.backend.uno.engine.view.*;
import com.flip.backend.uno.entities.UnoCard;
import com.flip.backend.uno.entities.UnoPlayer;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class UnoViewBuildTest {

    @Test
    void selfSeesFullHandOthersOnlyCounts() {
        UnoStartPhase start = new UnoStartPhase(List.of("P1","P2","P3"));
        start.enter();
        UnoRuntimePhase runtime = start.transit();

        UnoView viewP1 = runtime.buildView("P1");
        assertEquals("UNO", viewP1.board().gameType());
        UnoPlayerView p1v = viewP1.players().stream().filter(p -> p.playerId().equals("P1")).findFirst().orElseThrow();
        UnoPlayerView p2v = viewP1.players().stream().filter(p -> p.playerId().equals("P2")).findFirst().orElseThrow();
        assertNotNull(p1v.hand());
        assertEquals(p1v.handSize(), p1v.hand().size());
        assertNull(p2v.hand());
        assertTrue(p2v.handSize() > 0);

        // Different perspective
        UnoView viewP2 = runtime.buildView("P2");
        UnoPlayerView p2vSelf = viewP2.players().stream().filter(p -> p.playerId().equals("P2")).findFirst().orElseThrow();
        assertNotNull(p2vSelf.hand());
        assertEquals(p2vSelf.handSize(), p2vSelf.hand().size());
    }

    @Test
    void initialBoardViewFieldsPopulated() {
        UnoStartPhase start = new UnoStartPhase(List.of("A","B"));
        start.enter();
        UnoRuntimePhase runtime = start.transit();
        UnoView viewA = runtime.buildView("A");
        assertNotNull(viewA.board().topCard());
        assertNotNull(viewA.board().activeColor());
        assertTrue(viewA.board().drawPileSize() > 0);
        assertTrue(viewA.board().discardPileSize() >= 1); // starter card
    }

    @Test
    void boardViewUpdatesAfterPlayTurn() {
        UnoStartPhase start = new UnoStartPhase(List.of("X","Y"));
        start.enter();
        UnoRuntimePhase runtime = start.transit();
        var board = start.board();
        var deck = start.deck();
        // Force deterministic state: top = RED 1
        UnoCard forcedTop = UnoCard.number(UnoCard.Color.RED,1);
        deck.discard(forcedTop);
        board.applyTop(forcedTop, null);

        // Prepare current player's hand with two playable RED numbers
        UnoPlayer current = (UnoPlayer) board.currentPlayer();
        current.getHand().clear();
        UnoCard willPlay = UnoCard.number(UnoCard.Color.RED,3);
        UnoCard remain = UnoCard.number(UnoCard.Color.RED,5);
        current.giveCard(willPlay);
        current.giveCard(remain);

        UnoView before = runtime.buildView(current.getId());
        int beforeDiscard = before.board().discardPileSize();
        long beforeTurn = before.board().turnCount();
        String expectedPlayDisplay = willPlay.getDisplay();

        runtime.runSingleTurn();
        UnoView after = runtime.buildView(current.getId());
        assertEquals(expectedPlayDisplay, after.board().topCard());
        assertEquals(beforeDiscard + 1, after.board().discardPileSize());
        assertEquals(beforeTurn + 1, after.board().turnCount());
        // Hand now has 1 card and not winner yet
        UnoPlayerView selfView = after.players().stream().filter(p -> p.playerId().equals(current.getId())).findFirst().orElseThrow();
        assertEquals(1, selfView.handSize());
        assertFalse(selfView.hand().isEmpty());
    }
}
