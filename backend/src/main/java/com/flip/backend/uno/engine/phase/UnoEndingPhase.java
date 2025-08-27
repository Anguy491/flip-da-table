package com.flip.backend.uno.engine.phase;

import com.flip.backend.game.engine.phase.Phase;
import com.flip.backend.uno.entities.UnoPlayer;

/** Simple ending phase for UNO: records winner and prints summary. */
public class UnoEndingPhase extends Phase {
    private final UnoPlayer winner;

    public UnoEndingPhase(UnoPlayer winner) { this.winner = winner; }

    public UnoPlayer winner() { return winner; }

    @Override
    public void enter() {
        System.out.println("UNO Game Over. Winner: " + (winner != null ? winner.getId() : "<none>"));
    }
}
