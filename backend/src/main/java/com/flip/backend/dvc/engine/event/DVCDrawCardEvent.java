package com.flip.backend.dvc.engine.event;

import com.flip.backend.game.engine.event.GameEvent;
import com.flip.backend.game.engine.event.EventQueue;
import com.flip.backend.dvc.entities.*;

/**
 * Draw step: player chooses a color (BLACK/WHITE) if deck not empty. We then draw the first card of that color
 * via a filtered draw algorithm (cycling non-matching cards to bottom). The card becomes a pending card on board
 * (NOT inserted into the player's hand yet). Afterwards automatically enqueue a guess event.
 * If deck empty, we simply enqueue a guess event (skip draw, no pending card created).
 */
public class DVCDrawCardEvent extends GameEvent {
    private final DVCDeck deck;
    private final DVCBoard board;
    private final DVCPlayer player;
    private final EventQueue queue;
    private DVCCard.Color chosenColor; // must be set before execute when deck has cards
    private boolean executed;

    public DVCDrawCardEvent(DVCDeck deck, DVCBoard board, DVCPlayer player, EventQueue queue) {
        super(player, System.currentTimeMillis());
        this.deck = deck; this.board = board; this.player = player; this.queue = queue;
    }

    public void chooseColor(DVCCard.Color color) { this.chosenColor = color; }
    public DVCCard.Color chosenColor() { return chosenColor; }

    @Override public boolean isValid() {
        if (executed) return false;
        if (deck.remaining() == 0) return true; // skipping draw allowed
        return chosenColor != null && board.getPending(player.getId()) == null;
    }

    @Override public void execute() {
        if (executed) return; executed = true;
        if (deck.remaining() > 0) {
            DVCCard c = drawColor(deck, chosenColor);
            if (c == null) { // fallback: normal draw (any color) if none of chosen color exists
                c = deck.draw();
            }
            if (c != null) board.setPending(player.getId(), c); // pending state stored on board
        }
        // Chain: enqueue guess event automatically
        queue.enqueue(new DVCGuessCardEvent(board, player, queue));
    }

    /** Attempt to draw first card of desired color by rotating deck. */
    private DVCCard drawColor(DVCDeck deck, DVCCard.Color color) {
        int attempts = deck.remaining();
        if (attempts <= 0) return null;
        java.util.List<DVCCard> temp = new java.util.ArrayList<>();
        DVCCard found = null;
        for (int i=0;i<attempts;i++) {
            DVCCard c = deck.draw();
            if (c == null) break;
            if (found == null && c.getColor() == color) { found = c; break; }
            temp.add(c);
        }
        // restore others
        for (DVCCard t : temp) deck.putBottom(t);
        return found;
    }
}
