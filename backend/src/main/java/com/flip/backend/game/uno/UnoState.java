package com.flip.backend.game.uno;

import java.util.*;

/** Aggregate state for an UNO game (simplified). */
public class UnoState {
    public final List<UnoPlayer> players = new ArrayList<>();
    public final Deque<UnoCard> drawPile = new ArrayDeque<>();
    public final Deque<UnoCard> discardPile = new ArrayDeque<>();
    public int direction = 1; // 1 normal, -1 reversed
    public int currentPlayerIndex = 0;
    public int pendingDraw = 0; // accumulated draw cards
    public boolean mustChooseColor = false;
    public final Set<String> winners = new LinkedHashSet<>();
    public GamePhase phase = GamePhase.RUNNING;

    public record PlayerInitSpec(String id, boolean bot) {}

    public static UnoState initial(List<PlayerInitSpec> specs) {
        if (specs.size() < 2) throw new IllegalArgumentException("Need at least 2 players");
        UnoState s = new UnoState();
        for (PlayerInitSpec spec : specs) s.players.add(new UnoPlayer(spec.id(), spec.bot()));
        // Build full standard UNO deck (108 cards)
        List<UnoCard> deck = new ArrayList<>(108);
        for (UnoColor c : UnoColor.values()) {
            // one zero
            deck.add(new UnoCard(c, UnoValue.ZERO));
            // two each of 1-9
            UnoValue[] numbers = {UnoValue.ONE, UnoValue.TWO, UnoValue.THREE, UnoValue.FOUR, UnoValue.FIVE, UnoValue.SIX, UnoValue.SEVEN, UnoValue.EIGHT, UnoValue.NINE};
            for (UnoValue v : numbers) {
                deck.add(new UnoCard(c, v));
                deck.add(new UnoCard(c, v));
            }
            // two each of action cards
            UnoValue[] actions = {UnoValue.SKIP, UnoValue.REVERSE, UnoValue.DRAW_TWO};
            for (UnoValue a : actions) {
                deck.add(new UnoCard(c, a));
                deck.add(new UnoCard(c, a));
            }
        }
        // 4 wild + 4 wild draw four
        for (int i=0;i<4;i++) deck.add(new UnoCard(null, UnoValue.WILD));
        for (int i=0;i<4;i++) deck.add(new UnoCard(null, UnoValue.WILD_DRAW_FOUR));
        Collections.shuffle(deck, new Random());
        for (UnoCard c : deck) s.drawPile.push(c);
        // Deal 5 each
        for (int i=0;i<5;i++) {
            for (UnoPlayer p : s.players) {
                p.hand().add(s.drawPile.pop());
            }
        }
        // Flip first non wild as discard top
        while (!s.drawPile.isEmpty()) {
            UnoCard top = s.drawPile.pop();
            if (!top.isWild()) { s.discardPile.push(top); break; }
            // Put wild back bottom
            s.drawPile.addLast(top);
        }
        return s;
    }

    public UnoCard top() { return discardPile.peek(); }
    public UnoPlayer currentPlayer() { return players.get(currentPlayerIndex); }
    public int playerCount() { return players.size(); }
}
