package com.flip.backend.dvc.engine.event;

import com.flip.backend.game.engine.event.GameEvent;
import com.flip.backend.game.engine.event.EventQueue;
import com.flip.backend.dvc.entities.*;

/**
 * Guess event: asks current player to select a target (other player's unrevealed card index) and a guess value.
 * After resolution immediately enqueue reveal event (with result and target/self info).
 * Interaction points (UI) set targetPlayerId, targetIndex, and guess before queue processing calls execute().
 */
public class DVCGuessCardEvent extends GameEvent {
    private final DVCBoard board;
    private final DVCPlayer actor;
    private final EventQueue queue;
    private String targetPlayerId; // must not be actor
    private Integer targetIndex;   // index in target player's ordered hand
    private DVCPlayer.Guess guess; // number or joker
    private Boolean correct;       // outcome after execute
    private boolean executed;

    public DVCGuessCardEvent(DVCBoard board, DVCPlayer actor, EventQueue queue) {
        super(actor, System.currentTimeMillis());
        this.board = board; this.actor = actor; this.queue = queue;
    }

    public void setSelection(String targetPlayerId, Integer targetIndex, DVCPlayer.Guess guess) {
        this.targetPlayerId = targetPlayerId;
        this.targetIndex = targetIndex;
        this.guess = guess;
    }

    public Boolean isCorrect() { return correct; }
    public String targetPlayerId() { return targetPlayerId; }
    public Integer targetIndex() { return targetIndex; }

    @Override public boolean isValid() {
        if (executed) return false;
        if (targetPlayerId == null || targetIndex == null || guess == null) return false; // needs input
        if (actor.getId().equals(targetPlayerId)) return false;
        // ensure target exists and index valid & unrevealed
        return board.snapshotOrder().stream().anyMatch(p -> p.getId().equals(targetPlayerId));
    }

    @Override public void execute() {
        if (executed) return; executed = true;
        DVCPlayer target = board.snapshotOrder().stream().filter(p -> p.getId().equals(targetPlayerId)).findFirst().orElse(null);
        if (target == null) { correct = false; return; }
        correct = target.verifyGuess(targetIndex, guess);
        // enqueue reveal event carrying result
    queue.enqueue(new DVCRevealCardEvent(board, actor, target, targetIndex, correct, queue));
    }
}
