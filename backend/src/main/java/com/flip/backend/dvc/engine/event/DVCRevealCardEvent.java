package com.flip.backend.dvc.engine.event;

import com.flip.backend.game.engine.event.GameEvent;
import com.flip.backend.game.engine.event.EventQueue;
import com.flip.backend.dvc.entities.*;

/**
 * Reveal event: If guess was correct -> reveal target card, then wait for player decision to continue guessing or stop.
 * Decision is set via continueGuess flag before execute processes (UI action). If continue -> enqueue new Guess event;
 * else -> enqueue settle event. If guess incorrect -> reveal self pending card (or if deck empty scenario: reveal one of self by UI earlier?)
 * For failed guess (with deck card pending) we reveal the pending drawn card and then enqueue settle.
 */
public class DVCRevealCardEvent extends GameEvent {
    private final DVCBoard board;
    private final DVCPlayer actor; // the guessing player
    private final DVCPlayer target; // may be null if self reveal path
    private final int targetIndex; // index attempted
    private final boolean correct;
    private final EventQueue queue;
    private boolean continueGuess; // input from UI when correct
    private boolean decisionSet; // ensure UI sets decision
    private boolean executed;

    public DVCRevealCardEvent(DVCBoard board, DVCPlayer actor, DVCPlayer target, int targetIndex, boolean correct, EventQueue queue) {
        super(actor, System.currentTimeMillis());
        this.board = board; this.actor = actor; this.target = target; this.targetIndex = targetIndex; this.correct = correct; this.queue = queue;
    }

    public void setContinueGuess(boolean cont) { this.continueGuess = cont; this.decisionSet = true; }
    public boolean needsDecision() { return correct && !decisionSet; }
    public boolean isCorrect() { return correct; }
    public boolean correct() { return correct; }
    public String targetPlayerId() { return target != null ? target.getId() : null; }
    public int getTargetIndex() { return targetIndex; }

    @Override public boolean isValid() {
        if (executed) return false;
        if (correct) {
            // must have unrevealed card at target index
            if (target == null) return false;
            var list = target.hand().snapshot();
            if (targetIndex < 0 || targetIndex >= list.size()) return false;
            // Allow already-revealed (we may reveal immediately after a correct guess for UI freshness)
            // need decision before execution
            return decisionSet;
        } else {
            // incorrect path always valid; pending card must exist if deck draw occurred OR actor will self reveal separate card at runtime
            return true;
        }
    }

    @Override public void execute() {
        if (executed) return; executed = true;
        if (correct) {
            target.revealAt(targetIndex); // reveal guessed card
            if (continueGuess) {
                queue.enqueue(new DVCGuessCardEvent(board, actor, queue));
            } else {
                // proceed to settle (place pending card if any)
                queue.enqueue(new DVCSettleCardEvent(board, actor, null));
            }
        } else {
            // incorrect guess: reveal pending drawn card if exists
            DVCCard pending = board.getPending(actor.getId());
            if (pending != null && !pending.isFaceUp()) pending.reveal();
            queue.enqueue(new DVCSettleCardEvent(board, actor, null));
        }
    }
}
