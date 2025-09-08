package com.flip.backend.dvc.engine;

import com.flip.backend.dvc.engine.phase.DVCStartPhase;
import com.flip.backend.dvc.engine.phase.DVCRuntimePhase;
import com.flip.backend.dvc.entities.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DVC runtime flow with deck starting at remaining=1, two players A and B.
 * Steps:
 * 1) A draws the last card (0); A guesses correctly, chooses to stop, then must settle the pending card.
 * 2) B then guesses correctly and chooses to continue; engine should prompt B to guess again.
 * 3) B then guesses incorrectly; engine requires B to self-reveal one of their own cards.
 * Throughout, assert awaiting transitions are correct.
 */
public class DVCRuntimeDeckEmptyTest {

    @Test
    void deckRemainingOne_thenA_draws_zero_and_flows_through_required_states() {
        // Setup start phase with players A and B
        DVCStartPhase start = new DVCStartPhase(List.of("A", "B"));
        start.enter();
        DVCDeck deck = start.deck();
        DVCBoard board = start.board();
        List<DVCPlayer> players = start.players();

        // Normalize hands for determinism
        DVCPlayer A = players.stream().filter(p -> p.getId().equals("A")).findFirst().orElseThrow();
        DVCPlayer B = players.stream().filter(p -> p.getId().equals("B")).findFirst().orElseThrow();
        A.hand().clear();
        B.hand().clear();
        // Give A and B known cards (all hidden)
        // A: BLACK 7, WHITE 9
        A.giveCard(DVCCard.number(DVCCard.Color.BLACK, 7));
        A.giveCard(DVCCard.number(DVCCard.Color.WHITE, 9));
        // B: WHITE 3, BLACK 5 (so B[0] is WHITE 3)
        B.giveCard(DVCCard.number(DVCCard.Color.WHITE, 3));
        B.giveCard(DVCCard.number(DVCCard.Color.BLACK, 5));

        // Make deck contain exactly one card: BLACK 0 (the last drawable)
        while (deck.remaining() > 0) deck.draw();
        deck.putBottom(DVCCard.number(DVCCard.Color.BLACK, 0));
        assertEquals(1, deck.remaining());

        // All settled -> transit to runtime
        start.settled("A");
        start.settled("B");
        DVCRuntimePhase runtime = start.transit();

        // Enter runtime: since deck has 1, first awaiting should be DRAW_COLOR for A
        runtime.enter();
        assertEquals(DVCRuntimePhase.Awaiting.DRAW_COLOR, runtime.awaiting());
        assertEquals("A", board.currentPlayer().getId());

        // A draws BLACK (the only remaining card 0B becomes pending)
        boolean drawChosen = runtime.provideDrawColor("A", "BLACK");
        assertTrue(drawChosen);
        assertEquals(0, deck.remaining(), "Deck should now be empty after A draws");
        assertEquals(DVCRuntimePhase.Awaiting.GUESS_SELECTION, runtime.awaiting());

        // 1) A guesses B[0] correctly (WHITE 3), then chooses to stop
        long bHiddenBefore = B.hiddenCount();
        boolean guessOk = runtime.provideGuess("A", "B", 0, false, 3);
        assertTrue(guessOk);
        // After correct guess, target card is revealed immediately; awaiting REVEAL_DECISION
        assertEquals(bHiddenBefore - 1, B.hiddenCount());
        assertEquals(DVCRuntimePhase.Awaiting.REVEAL_DECISION, runtime.awaiting());

        // A decides to stop -> should require settling pending card placement
        boolean decisionOk = runtime.provideRevealDecision("A", false);
        assertTrue(decisionOk);
        assertEquals(DVCRuntimePhase.Awaiting.SETTLE_POSITION, runtime.awaiting());

        // Settle: auto placement (null index allowed), end turn then advance to B
        boolean settleOk = runtime.provideSettlePosition("A", null);
        assertTrue(settleOk);
        assertEquals("B", board.currentPlayer().getId());
        // With deck empty, B should directly be in GUESS_SELECTION
        assertEquals(DVCRuntimePhase.Awaiting.GUESS_SELECTION, runtime.awaiting());

        // 2) B guesses A's BLACK 7 correctly, chooses to continue
        // After A settled pending BLACK 0 with auto-ordering, A's hand is likely [B0, B7, W9]; compute index dynamically
        int idxBlack7 = -1;
        for (int i = 0; i < A.hand().snapshot().size(); i++) {
            DVCCard c = A.hand().snapshot().get(i);
            if (!c.isFaceUp() && c.getColor() == DVCCard.Color.BLACK && Integer.valueOf(7).equals(c.getNumber())) { idxBlack7 = i; break; }
        }
        assertTrue(idxBlack7 >= 0, "Should find BLACK 7 in A's hand");
        long aHiddenBefore = A.hiddenCount();
        boolean bGuessOk = runtime.provideGuess("B", "A", idxBlack7, false, 7);
        assertTrue(bGuessOk);
        assertEquals(aHiddenBefore - 1, A.hiddenCount());
        assertEquals(DVCRuntimePhase.Awaiting.REVEAL_DECISION, runtime.awaiting());

        boolean bDecisionContinue = runtime.provideRevealDecision("B", true);
        assertTrue(bDecisionContinue);
        // After continue, engine should enqueue a new guess for B
        assertEquals(DVCRuntimePhase.Awaiting.GUESS_SELECTION, runtime.awaiting());

        // 3) B now guesses incorrectly (use impossible number 99) on any remaining hidden card index
        int anyHiddenIdx = -1;
        for (int i = 0; i < A.hand().snapshot().size(); i++) {
            if (!A.hand().snapshot().get(i).isFaceUp()) { anyHiddenIdx = i; break; }
        }
        assertTrue(anyHiddenIdx >= 0, "Should have at least one hidden card to guess incorrectly");
        boolean bWrong = runtime.provideGuess("B", "A", anyHiddenIdx, false, 99);
        assertTrue(bWrong);
        assertEquals(DVCRuntimePhase.Awaiting.SELF_REVEAL_CHOICE, runtime.awaiting());

        long bHiddenBeforeSelf = B.hiddenCount();
        int bHiddenIdx = -1;
        for (int i = 0; i < B.hand().snapshot().size(); i++) {
            if (!B.hand().snapshot().get(i).isFaceUp()) { bHiddenIdx = i; break; }
        }
        assertTrue(bHiddenIdx >= 0, "B should have a hidden card to self reveal");
        boolean selfReveal = runtime.provideSelfReveal("B", bHiddenIdx);
        assertTrue(selfReveal);
        assertEquals(bHiddenBeforeSelf - 1, B.hiddenCount());
        // With 2 players, B now has 0 hidden -> game ends immediately; winner should be A
        assertTrue(runtime.isFinished());
        assertEquals("A", runtime.winnerId());
        assertEquals(DVCRuntimePhase.Awaiting.NONE, runtime.awaiting());
    }
}
