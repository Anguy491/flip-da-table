package com.flip.backend.uno.engine.event;

import com.flip.backend.game.engine.event.GameEvent;
import com.flip.backend.uno.entities.*;

public class UnoDrawCardEvent extends GameEvent {
    private final UnoDeck deck;
    private final UnoPlayer player;
    private UnoCard drawn;

    public UnoDrawCardEvent(UnoDeck deck, UnoPlayer player) {
        super(player, System.currentTimeMillis());
        this.deck = deck; this.player = player;
    }

    @Override public boolean isValid() { return true; }

    @Override public void execute() {
        drawn = deck.draw();
        if (drawn != null) player.giveCard(drawn);
    }

    public UnoCard drawnCard() { return drawn; }
}
