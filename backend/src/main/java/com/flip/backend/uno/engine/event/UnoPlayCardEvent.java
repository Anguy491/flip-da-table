package com.flip.backend.uno.engine.event;

import com.flip.backend.game.engine.event.GameEvent;
import com.flip.backend.uno.entities.*;

public class UnoPlayCardEvent extends GameEvent {
    private final UnoBoard board;
    private final UnoDeck deck;
    private final UnoPlayer player;
    private final UnoCard card;

    public UnoPlayCardEvent(UnoBoard board, UnoDeck deck, UnoPlayer player, UnoCard card) {
        super(player, System.currentTimeMillis());
        this.board = board; this.deck = deck; this.player = player; this.card = card;
    }

    @Override public boolean isValid() {
        if (card == null) return false;
        if (!player.getHand().view().contains(card)) return false;
        UnoCard top = board.lastPlayedCard();
        UnoCard.Color active = board.activeColor();
        if (card.getType() == UnoCard.Type.WILD || card.getType() == UnoCard.Type.WILD_DRAW_FOUR) return true;
        if (top == null) return true;
        if (card.getColor() != UnoCard.Color.WILD && card.getColor() == active) return true;
        if (card.getType() == top.getType() && card.getType() != UnoCard.Type.NUMBER) return true;
        return card.getType()==UnoCard.Type.NUMBER && top.getType()==UnoCard.Type.NUMBER && card.getNumber().equals(top.getNumber());
    }

    @Override public void execute() {
        player.playCard(card);
        deck.discard(card);
        UnoCard.Color chosen = null;
        if (card.getColor() == UnoCard.Color.WILD) {
            chosen = player.getHand().view().stream().filter(c -> c.getColor() != UnoCard.Color.WILD)
                    .map(UnoCard::getColor).findFirst().orElse(UnoCard.Color.RED);
        }
        board.applyTop(card, chosen);
    }
}
