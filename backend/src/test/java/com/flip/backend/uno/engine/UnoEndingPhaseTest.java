package com.flip.backend.uno.engine;

import com.flip.backend.uno.engine.phase.*;
import com.flip.backend.uno.entities.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Integration test verifying runtime detects winner and constructs ending phase. */
public class UnoEndingPhaseTest {

    @Test
    void playerWinsAndEndingPhaseCreated() {
        // Construct start phase with 2 players; then manually craft a near-win state.
        UnoStartPhase start = new UnoStartPhase(List.of("A", "B"));
        start.enter();
        UnoRuntimePhase runtime = start.transit();

        // Force a deterministic simple state: Clear hands then set winning player with one playable card.
        UnoBoard board = start.board();
        UnoDeck deck = start.deck();
        UnoPlayer current = (UnoPlayer) board.currentPlayer();
        UnoPlayer other = (UnoPlayer) board.peekNext();

        // Clear both hands
        current.getHand().clear();
        other.getHand().clear();

        // Set top card and active color
        UnoCard starter = UnoCard.number(UnoCard.Color.RED, 5);
        board.applyTop(starter, null);
        deck.discard(starter);

        // Give current player a single matching number card (any color RED 7) so it can play and win
        UnoCard winningCard = UnoCard.number(UnoCard.Color.RED, 7);
        current.giveCard(winningCard);

        // Run a single turn; should win
        runtime.runSingleTurn();
        assertEquals(0, current.cardCount());
        assertNotNull(runtime.endingPhase());
        assertEquals(current.getId(), runtime.endingPhase().winner().getId());
        assertNull(((UnoPlayer) other).getHand().view().stream().filter(c -> c == winningCard).findFirst().orElse(null));
    }
}
