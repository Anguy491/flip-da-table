package com.flip.backend.uno.engine.event;

import com.flip.backend.game.engine.event.GameEvent;
import com.flip.backend.uno.entities.*;

public class UnoPlayCardEvent extends GameEvent {
    private final UnoBoard board;
    private final UnoDeck deck;
    private final UnoPlayer player;
    private final UnoCard card;
    private int advanceSteps = 1; // how many seats to step after this card resolves

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
        // Apply action effects (simplified order): Skip, Reverse, Draw Two, Wild Draw Four
        switch (card.getType()) {
            case SKIP -> advanceSteps = 2; // skip next player
            case REVERSE -> { board.reverse(); advanceSteps = 1; }
            case DRAW_TWO -> { drawNext(2); advanceSteps = 2; }
            case WILD_DRAW_FOUR -> { if (chosen == null) chosen = UnoCard.Color.RED; drawNext(4); advanceSteps = 2; }
            case WILD, NUMBER -> { /* no extra */ }
        }
    }

    private void drawNext(int n) {
        UnoPlayer target = (UnoPlayer) board.peekNext();
        for (int i=0;i<n;i++) {
            UnoCard d = deck.draw();
            if (d != null) target.giveCard(d);
        }
    }

    public int getAdvanceSteps() { return advanceSteps; }
}
