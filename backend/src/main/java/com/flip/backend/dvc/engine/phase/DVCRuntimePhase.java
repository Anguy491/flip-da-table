package com.flip.backend.dvc.engine.phase;

import com.flip.backend.game.engine.phase.RuntimePhase;
import com.flip.backend.dvc.entities.*;
import com.flip.backend.game.engine.event.EventQueue;
import com.flip.backend.dvc.engine.event.*;
import com.flip.backend.dvc.engine.view.*;
import java.util.*;

public class DVCRuntimePhase extends RuntimePhase {
    // Core references
    private final DVCDeck deck;
    private final DVCBoard board;
    private final List<DVCPlayer> players;
    private final EventQueue queue = new EventQueue();

    // Game outcome
    private String winnerId;
    private boolean finished;
    private DVCEndingPhase endingPhase;

    // Turn state
    private long turnId = 0L; // increments each time a player's action fully settles
    private DVCPlayer current() { return (DVCPlayer) board.currentPlayer(); }

    // Interaction waiting state (what input the UI must provide before processing next event)
    public enum Awaiting {
        NONE,
        DRAW_COLOR,          // need color for DVCDrawCardEvent
        GUESS_SELECTION,     // need target player + index + guess value
        REVEAL_DECISION,     // after correct guess need continue or stop
        SETTLE_POSITION,     // need insert index for pending joker (optional for non-joker)
        SELF_REVEAL_CHOICE   // deck empty & guess wrong: choose one of own unrevealed to reveal
    }
    private Awaiting awaiting = Awaiting.NONE;

    // Cached references to current head event for context
    private DVCDrawCardEvent pendingDraw;
    private DVCGuessCardEvent pendingGuess;
    private DVCRevealCardEvent pendingReveal;
    private DVCSettleCardEvent pendingSettle;

    public DVCRuntimePhase(DVCDeck deck, DVCBoard board, List<DVCPlayer> players) {
        this.deck = deck; this.board = board; this.players = players;
    }

    @Override public void enter() {
        // First action: enqueue an implicit draw (or guess if deck empty)
        startTurn();
    }

    @Override public String run() { return winnerId; }

    public DVCDeck deck() { return deck; }
    public DVCBoard board() { return board; }
    public List<DVCPlayer> players() { return players; }
    public boolean isFinished() { return finished; }
    public String winnerId() { return winnerId; }
    public Awaiting awaiting() { return awaiting; }
    public long turnId() { return turnId; }
    public DVCEndingPhase endingPhase() { return endingPhase; }

    /* ===================== View Construction ===================== */
    public DVCView buildView(String perspectivePlayerId) {
        // Order snapshot starting from board.currentPlayer for index mapping
        var order = board.snapshotOrder();
        int currentIndex = 0;
        for (int i=0;i<order.size();i++) if (order.get(i).getId().equals(current().getId())) { currentIndex = i; break; }
        DVCBoardView boardView = new DVCBoardView(
            "DVC",
            turnId,
            board.direction(),
            currentIndex,
            deck.remaining(),
            awaiting.name(),
            winnerId
        );
        List<DVCPlayerView> pviews = new ArrayList<>();
        for (var p : order) {
            boolean self = p.getId().equals(perspectivePlayerId);
            var snapshot = p.hand().snapshot();
            int hidden = (int) snapshot.stream().filter(c -> !c.isRevealed()).count();
            List<String> cards = self ? snapshot.stream().map(c -> c.getDisplay()).toList() : null; // opponents不暴露
            List<Boolean> revealedFlags = snapshot.stream().map(DVCCard::isRevealed).toList();
            pviews.add(new DVCPlayerView(p.getId(), p.isBot(), snapshot.size(), hidden, cards, revealedFlags));
        }
        return new DVCView(boardView, List.copyOf(pviews), perspectivePlayerId);
    }

    /* ===================== Turn Lifecycle ===================== */
    private void startTurn() {
        if (finished) return;
        // Skip eliminated players (all revealed)
        while (board.isEliminated(current()) && !finished) {
            advanceSeatOnly();
        }
        if (finished) return;
        if (deck.remaining() > 0) {
            pendingDraw = new DVCDrawCardEvent(deck, board, current(), queue);
            queue.enqueue(pendingDraw);
            awaiting = Awaiting.DRAW_COLOR;
        } else {
            // Directly enqueue a guess event
            pendingGuess = new DVCGuessCardEvent(board, current(), queue);
            queue.enqueue(pendingGuess);
            awaiting = Awaiting.GUESS_SELECTION;
        }
    }

    private void advanceSeatOnly() {
        board.step(1); board.tickTurn(); // seat advance
        // After seat moved check win condition (maybe only one active left)
        checkVictory();
    }

    private void endTurnAndAdvance() {
        turnId++;
        checkEliminations();
        checkVictory();
        if (!finished) {
            advanceSeatOnly();
            if (!finished) startTurn();
        }
    }

    private void checkEliminations() {
        // passive: nothing else needed now
    }

    private void checkVictory() {
        if (finished) return;
        long active = board.activePlayerCount();
        if (active <= 1) {
            var survivor = board.snapshotOrder().stream().filter(p -> p.hiddenCount() > 0).findFirst().orElse(null);
            winnerId = survivor != null ? survivor.getId() : null;
            finished = true;
            endingPhase = new DVCEndingPhase(winnerId);
            endingPhase.enter();
        }
    }

    /* ===================== UI Input Methods ===================== */
    public boolean provideDrawColor(String playerId, String colorName) {
        if (awaiting != Awaiting.DRAW_COLOR || pendingDraw == null) return false;
        if (!current().getId().equals(playerId)) return false;
        try {
            pendingDraw.chooseColor(DVCCard.Color.valueOf(colorName.toUpperCase()));
        } catch (Exception e) { return false; }
        if (!pendingDraw.isValid()) return false;
        pendingDraw.execute();
        pendingDraw = null;
        // queue now has guess event
        pendingGuess = (DVCGuessCardEvent) queue.poll(); // draw enqueued guess
        awaiting = Awaiting.GUESS_SELECTION;
        return true;
    }

    public boolean provideGuess(String playerId, String targetPlayerId, int targetIndex, boolean joker, Integer number) {
        if (awaiting != Awaiting.GUESS_SELECTION || pendingGuess == null) return false;
        if (!current().getId().equals(playerId)) return false;
        DVCPlayer.Guess guess = joker ? DVCPlayer.Guess.jokerGuess() : DVCPlayer.Guess.number(number);
        pendingGuess.setSelection(targetPlayerId, targetIndex, guess);
        if (!pendingGuess.isValid()) return false;
        pendingGuess.execute();
        // reveal event enqueued
        pendingReveal = (DVCRevealCardEvent) queue.poll();
    if (pendingReveal.correct()) {
            awaiting = Awaiting.REVEAL_DECISION; // need continue or stop from player
        } else {
            // incorrect: if deck empty and no pending card we must self reveal one chosen by player later
            if (deck.remaining() == 0 && board.getPending(playerId) == null) {
                awaiting = Awaiting.SELF_REVEAL_CHOICE;
            } else {
                // immediate execute (no decision) -> will queue settle
                pendingReveal.setContinueGuess(false); // ensure decision
                if (!pendingReveal.isValid()) return false; // should be valid
                pendingReveal.execute();
                pendingReveal = null;
                handlePostRevealChain();
            }
        }
        pendingGuess = null;
        return true;
    }

    public boolean provideRevealDecision(String playerId, boolean continueGuess) {
        if (awaiting != Awaiting.REVEAL_DECISION || pendingReveal == null) return false;
        if (!current().getId().equals(playerId)) return false;
        pendingReveal.setContinueGuess(continueGuess);
        if (!pendingReveal.isValid()) return false;
        pendingReveal.execute();
        pendingReveal = null;
        handlePostRevealChain();
        return true;
    }

    public boolean provideSelfReveal(String playerId, int ownIndex) {
        if (awaiting != Awaiting.SELF_REVEAL_CHOICE || pendingReveal == null) return false;
        if (!current().getId().equals(playerId)) return false;
        // Simulate revealing self card due to wrong guess with empty deck
        DVCPlayer self = current();
        try { self.revealAt(ownIndex); } catch (Exception e) { return false; }
        // now treat like incorrect path finalize reveal -> settle (no pending card)
        pendingReveal.setContinueGuess(false); // unify path
        if (!pendingReveal.isValid()) { // still requires decisionSet for correct only; incorrect path valid
            // For incorrect path pendingReveal.isValid() returns true without decisionSet.
        }
        pendingReveal.execute();
        pendingReveal = null;
        handlePostRevealChain();
        return true;
    }

    public boolean provideSettlePosition(String playerId, Integer insertIndex) {
        if (awaiting != Awaiting.SETTLE_POSITION || pendingSettle == null) return false;
        if (!current().getId().equals(playerId)) return false;
        pendingSettle.setInsertIndex(insertIndex);
        if (!pendingSettle.isValid()) return false;
        pendingSettle.execute();
        pendingSettle = null; awaiting = Awaiting.NONE;
        endTurnAndAdvance();
        return true;
    }

    private void handlePostRevealChain() {
        // After reveal we either received a new guess or a settle
        if (queue.isEmpty()) {
            // Should not happen: reveal enqueues either guess or settle
            awaiting = Awaiting.NONE; endTurnAndAdvance(); return;
        }
        var next = queue.poll();
        if (next instanceof DVCGuessCardEvent ge) {
            pendingGuess = ge; awaiting = Awaiting.GUESS_SELECTION; return;
        }
        if (next instanceof DVCSettleCardEvent se) {
            pendingSettle = se;
            // Need placement if pending card exists and is joker
            DVCCard pending = board.getPending(current().getId());
            if (pending != null && pending.isJoker()) {
                awaiting = Awaiting.SETTLE_POSITION; // wait for player to choose
            } else {
                // can auto settle with ordering
                if (pending != null) {
                    pendingSettle.setInsertIndex(null); // auto
                }
                if (!pendingSettle.isValid()) {
                    // for non-joker with insertIndex null validity is true because pending may be null or auto
                }
                pendingSettle.execute();
                pendingSettle = null; awaiting = Awaiting.NONE; endTurnAndAdvance();
            }
        }
    }
}
