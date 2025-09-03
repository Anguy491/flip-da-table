package com.flip.backend.dvc.engine.event;

import com.flip.backend.game.engine.event.GameEvent;
import com.flip.backend.dvc.entities.*;

/**
 * Settle event: place pending card into player's hand at a chosen position (player provided). If no pending card
 * (e.g. deck empty turn, incorrect guess forced self reveal earlier) just ends. Turn advancement will be managed by runtime loop.
 */
public class DVCSettleCardEvent extends GameEvent {
    private final DVCBoard board;
    private final DVCPlayer player;
    private Integer insertIndex; // chosen insertion point (0..hand.size())
    private boolean executed;

    public DVCSettleCardEvent(DVCBoard board, DVCPlayer player, com.flip.backend.game.engine.event.EventQueue queueIgnored) {
        super(player, System.currentTimeMillis());
        this.board = board; this.player = player;
    }

    public void setInsertIndex(Integer idx) { this.insertIndex = idx; }

    @Override public boolean isValid() {
        if (executed) return false;
        DVCCard pending = board.getPending(player.getId());
        if (pending == null) return true; // nothing to place
        if (insertIndex == null) return false; // need placement choice
        if (insertIndex < 0 || insertIndex > player.hand().snapshot().size()) return false;
        return true;
    }

    @Override public void execute() {
        if (executed) return; executed = true;
        DVCCard pending = board.removePending(player.getId());
        if (pending != null) {
            // insert according to ordering rules if index not given (safety) or joker flexibility
            if (insertIndex == null) {
                player.giveCard(pending); // fallback ordered add
            } else {
                if (pending.isJoker()) {
                    player.hand().snapshot(); // ensure snapshot ok
                    // manual positional insert: we need access to underlying list (cards protected in Hand)
                    // We'll use reflection-free approach by removing ordered add then repositioning via temporary list.
                    // Simpler: addOrdered then adjust if needed only for jokers.
                    player.giveCard(pending); // add at end or ordered place
                    // reposition if current index differs
                    var list = new java.util.ArrayList<>(player.hand().snapshot());
                    list.remove(pending); list.add(insertIndex, pending);
                    // rewrite hand internal list via clear + add
                    player.hand().clear();
                    for (DVCCard c : list) player.hand().addOrdered(c.isJoker()?c:c); // addOrdered keeps sort for numbers
                } else {
                    // non-joker: ignore explicit index; keep auto ordering semantics
                    player.giveCard(pending);
                }
            }
        }
    }
}
