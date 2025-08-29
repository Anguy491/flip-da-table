package com.flip.backend.uno.engine.event;

import com.flip.backend.game.engine.event.GameEvent;
import com.flip.backend.uno.entities.*;

public class UnoPlayCardEvent extends GameEvent {
    private final UnoBoard board;
    private final UnoDeck deck;
    private final UnoPlayer player;
    private final UnoCard card;
    private int advanceSteps = 1; // how many seats to step after this card resolves
    private boolean requiresColorSelection = false; // set true when a human plays a wild and must choose

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
        if (card.getType() == UnoCard.Type.WILD || card.getType() == UnoCard.Type.WILD_DRAW_FOUR) {
            if (player.isBot()) {
                // Bot auto-select color heuristically: pick most frequent remaining color (excluding wild)
                java.util.Map<UnoCard.Color, Integer> freq = new java.util.EnumMap<>(UnoCard.Color.class);
                for (UnoCard c : player.getHand().view()) {
                    if (c.getColor() != UnoCard.Color.WILD) {
                        freq.merge(c.getColor(), 1, Integer::sum);
                    }
                }
                chosen = freq.entrySet().stream()
                        .filter(e -> e.getKey() != UnoCard.Color.WILD)
                        .max(java.util.Map.Entry.comparingByValue())
                        .map(java.util.Map.Entry::getKey)
                        .orElseGet(() -> {
                            // fallback random color
                            UnoCard.Color[] colors = {UnoCard.Color.RED, UnoCard.Color.GREEN, UnoCard.Color.BLUE, UnoCard.Color.YELLOW};
                            return colors[new java.util.Random().nextInt(colors.length)];
                        });
            } else {
                // Human must choose later -> clear any previous active color to signal pending state
                requiresColorSelection = true;
                chosen = null; // ensure not applied
                // Explicitly clear active color (previous color could wrongly satisfy next-player logic)
                board.setActiveColor(null);
            }
        }
        board.applyTop(card, chosen); // chosen may be null awaiting human selection
        // Apply action effects (simplified order): Skip, Reverse, Draw Two, Wild Draw Four
        switch (card.getType()) {
            case SKIP -> advanceSteps = 2; // skip next player
            case REVERSE -> { board.reverse(); advanceSteps = 1; }
            case DRAW_TWO -> { drawNext(2); advanceSteps = 2; }
            case WILD_DRAW_FOUR -> { drawNext(4); advanceSteps = 2; }
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
    public boolean requiresColorSelection() { return requiresColorSelection; }
    public UnoPlayer getPlayer() { return player; }
}
