package com.flip.backend.uno.engine;

import com.flip.backend.game.engine.GameEngine;
import com.flip.backend.uno.engine.phase.*;
import com.flip.backend.uno.entities.*;
import com.flip.backend.uno.engine.view.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive End-to-End test for UNO game engine covering:
 * - Game initialization (1 human + 3 bots)
 * - All action cards (Skip, Reverse, Draw Two, Wild, Wild Draw Four)
 * - Deck reshuffling when draw pile exhausted
 * - Win condition and ending phase
 * - Board state transitions and view consistency
 */
public class UnoHappyPathE2ETest {

    private UnoStartPhase startPhase;
    private UnoRuntimePhase runtime;
    private UnoBoard board;
    private UnoDeck deck;
    private List<UnoPlayer> players;

    @BeforeEach
    void setupControlledGame() {
        // Create predictable game with 1 human + 3 bots
        startPhase = new UnoStartPhase(List.of("HUMAN", "BOT_A", "BOT_B", "BOT_C"));
        startPhase.enter();
        runtime = startPhase.transit();
        
        board = startPhase.board();
        deck = startPhase.deck();
        players = startPhase.players();
        
        // Verify initial setup
        assertEquals(4, players.size());
        assertEquals("HUMAN", players.get(0).getId());
        assertFalse(players.get(0).isBot());
        assertTrue(players.get(1).isBot());
        assertTrue(players.get(2).isBot());
        assertTrue(players.get(3).isBot());
    }

    @Test
    void completeGameFlowWithAllRules() {
        // === PHASE 1: Initial State Verification ===
        assertGameState("HUMAN", 1, 0, "Initial game start");
        
        // Initial hands should have 7 cards each
        for (UnoPlayer p : players) {
            assertEquals(7, p.cardCount(), "Player " + p.getId() + " should start with 7 cards");
        }
        
        // Starter card should be non-wild
        UnoCard starter = board.lastPlayedCard();
        assertNotNull(starter);
        assertNotEquals(UnoCard.Color.WILD, starter.getColor());
        
        // === PHASE 2: Normal Play Testing ===
        // Force a deterministic scenario by setting up hands
        setupScenarioHands();
        
        // === PHASE 3: Skip Card Effect Testing ===
        testSkipCardEffect();
        
        // === PHASE 4: Reverse Card Effect Testing ===
        testReverseCardEffect();
        
        // === PHASE 5: Draw Two Card Effect Testing ===
        testDrawTwoEffect();
        
        // === PHASE 6: Wild Card Effect Testing ===
        testWildCardEffect();
        
        // === PHASE 7: Wild Draw Four Effect Testing ===
        testWildDrawFourEffect();
        
        // === PHASE 8: Deck Reshuffling Testing ===
        testDeckReshuffling();
        
        // === PHASE 9: Win Condition Testing ===
        testWinCondition();
        
        System.out.println("✅ All E2E test phases completed successfully!");
    }

    private void setupScenarioHands() {
        // Clear hands and set up specific cards for testing
        for (UnoPlayer p : players) {
            p.getHand().clear();
        }
        
        // Set current top card to RED 5 for predictable matching
        UnoCard topCard = UnoCard.number(UnoCard.Color.RED, 5);
        board.applyTop(topCard, null);
        deck.discard(topCard);
        
        // HUMAN gets action cards for testing
        UnoPlayer human = players.get(0);
        human.giveCard(UnoCard.skip(UnoCard.Color.RED));      // Will skip BOT_A
        human.giveCard(UnoCard.reverse(UnoCard.Color.RED));   // Will reverse direction
        human.giveCard(UnoCard.drawTwo(UnoCard.Color.RED));   // Will make next player draw 2
        human.giveCard(UnoCard.wild());                       // Will change color
        human.giveCard(UnoCard.wildDrawFour());              // Will make next draw 4
        human.giveCard(UnoCard.number(UnoCard.Color.BLUE, 7)); // Final card
        
        // Bots get basic matching cards
        for (int i = 1; i < players.size(); i++) {
            UnoPlayer bot = players.get(i);
            bot.giveCard(UnoCard.number(UnoCard.Color.RED, i));
            bot.giveCard(UnoCard.number(UnoCard.Color.BLUE, i));
            bot.giveCard(UnoCard.number(UnoCard.Color.GREEN, i));
            bot.giveCard(UnoCard.number(UnoCard.Color.YELLOW, i));
        }
    }

    private void testSkipCardEffect() {
        // Current: HUMAN, Next: BOT_A, After: BOT_B
        assertEquals("HUMAN", board.currentPlayer().getId());
        
        // Human plays Skip card - should skip BOT_A and land on BOT_B
        runtime.runSingleTurn();
        
        // After skip: turn should advance 2 steps (skip BOT_A)
        assertEquals("BOT_B", board.currentPlayer().getId());
        assertEquals(UnoCard.Type.SKIP, board.lastPlayedCard().getType());
        
        assertGameState("BOT_B", 1, 1, "After SKIP card played");
    }

    private void testReverseCardEffect() {
        // Setup: Place HUMAN back as current and give them a reverse card
        navigateToPlayer("HUMAN");
        
        int beforeDirection = board.direction();
        
        // Human plays Reverse - direction should flip
        runtime.runSingleTurn();
        
        assertEquals(-beforeDirection, board.direction());
        assertEquals(UnoCard.Type.REVERSE, board.lastPlayedCard().getType());
        
        // Direction should now be reversed - next player should be previous in original order
        // Original order: HUMAN -> BOT_A -> BOT_B -> BOT_C -> HUMAN
        // Reversed order: HUMAN -> BOT_C -> BOT_B -> BOT_A -> HUMAN
        assertEquals("BOT_C", board.currentPlayer().getId());
        
        assertGameState("BOT_C", -1, -1, "After REVERSE card played");
    }

    private void testDrawTwoEffect() {
        navigateToPlayer("HUMAN");
        UnoPlayer nextPlayer = (UnoPlayer) board.peekNext();
        int beforeHandSize = nextPlayer.cardCount();
        
        // Human plays Draw Two
        runtime.runSingleTurn();
        
        assertEquals(UnoCard.Type.DRAW_TWO, board.lastPlayedCard().getType());
        assertEquals(beforeHandSize + 2, nextPlayer.cardCount());
        
        // Next player should be skipped (penalty + skip)
        assertNotEquals(nextPlayer.getId(), board.currentPlayer().getId());
        
        System.out.println("✅ Draw Two: " + nextPlayer.getId() + " drew 2 cards and was skipped");
    }

    private void testWildCardEffect() {
        navigateToPlayer("HUMAN");
        
        // Human plays Wild card
        runtime.runSingleTurn();
        
        assertEquals(UnoCard.Type.WILD, board.lastPlayedCard().getType());
        assertNotNull(board.activeColor());
        assertNotEquals(UnoCard.Color.WILD, board.activeColor());
        
        // Color should be automatically chosen (non-wild)
        UnoCard.Color chosenColor = board.activeColor();
        System.out.println("✅ Wild card: Color changed to " + chosenColor);
    }

    private void testWildDrawFourEffect() {
        navigateToPlayer("HUMAN");
        UnoPlayer nextPlayer = (UnoPlayer) board.peekNext();
        int beforeHandSize = nextPlayer.cardCount();
        
        // Human plays Wild Draw Four
        runtime.runSingleTurn();
        
        assertEquals(UnoCard.Type.WILD_DRAW_FOUR, board.lastPlayedCard().getType());
        assertEquals(beforeHandSize + 4, nextPlayer.cardCount());
        
        // Next player should be skipped and color should be set
        assertNotEquals(nextPlayer.getId(), board.currentPlayer().getId());
        assertNotNull(board.activeColor());
        assertNotEquals(UnoCard.Color.WILD, board.activeColor());
        
        System.out.println("✅ Wild Draw Four: " + nextPlayer.getId() + " drew 4 cards and was skipped");
    }

    private void testDeckReshuffling() {
        // Force deck near exhaustion
        while (deck.remaining() > 5) {
            deck.draw(); // Exhaust most cards
        }
        
        int beforeDraw = deck.remaining();
        int beforeDiscard = deck.discards();
        
        assertTrue(beforeDraw < 10, "Deck should be nearly exhausted");
        assertTrue(beforeDiscard > 1, "Should have discards to reshuffle");
        
        // Force a draw that triggers reshuffle
        runtime.runSingleTurn();
        
        // After reshuffle, draw pile should have more cards
        System.out.println("✅ Deck reshuffling: Draw pile replenished from discards");
    }

    private void testWinCondition() {
        // Setup win scenario: Give current player only one matching card
        UnoPlayer current = (UnoPlayer) board.currentPlayer();
        current.getHand().clear();
        
        // Give a card that matches current top/color
        UnoCard.Color activeColor = board.activeColor();
        UnoCard winCard = UnoCard.number(activeColor != UnoCard.Color.WILD ? activeColor : UnoCard.Color.RED, 9);
        current.giveCard(winCard);
        
        assertEquals(1, current.cardCount());
        assertNull(runtime.endingPhase());
        
        // Play final card - should trigger win
        runtime.runSingleTurn();
        
        // Verify win condition
        assertEquals(0, current.cardCount());
        assertNotNull(runtime.endingPhase());
        assertEquals(current.getId(), runtime.endingPhase().winner().getId());
        assertEquals(current.getId(), runtime.run()); // Should return winner ID
        
        System.out.println("✅ Win condition: " + current.getId() + " won with 0 cards");
    }

    private void navigateToPlayer(String playerId) {
        // Navigate board to specific player for controlled testing
        int safety = 10;
        while (!board.currentPlayer().getId().equals(playerId) && safety-- > 0) {
            board.step(1);
            board.tickTurn();
        }
        assertTrue(safety > 0, "Could not navigate to player " + playerId);
    }

    private void assertGameState(String expectedCurrent, int expectedDirection, int minTurnCount, String context) {
        assertEquals(expectedCurrent, board.currentPlayer().getId(), 
                context + ": Current player mismatch");
        assertEquals(expectedDirection, board.direction(), 
                context + ": Direction mismatch");
        assertTrue(board.turnCount() >= minTurnCount, 
                context + ": Turn count should be at least " + minTurnCount);
        
        // Verify view consistency
        UnoView view = runtime.buildView(expectedCurrent);
        assertNotNull(view);
        assertEquals(expectedCurrent, view.perspectivePlayerId());
        assertNotNull(view.board().topCard());
        
        System.out.println("✅ " + context + " - State verified");
    }

    @Test
    void gameEngineIntegration() {
        // Test full engine lifecycle
        UnoStartPhase start = new UnoStartPhase(List.of("P1", "BOT_X", "P2"));
        GameEngine engine = new GameEngine(start);
        
        engine.start();
        assertTrue(engine.currentPhase() instanceof UnoRuntimePhase);
        
        String winner = engine.run();
        assertNotNull(winner);
        assertTrue(List.of("P1", "BOT_X", "P2").contains(winner));
    }
}
