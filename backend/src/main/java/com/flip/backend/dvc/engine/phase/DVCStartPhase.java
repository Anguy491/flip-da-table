package com.flip.backend.dvc.engine.phase;

import com.flip.backend.game.engine.phase.StartPhase;
import com.flip.backend.dvc.entities.*;
import com.flip.backend.dvc.engine.view.*;
import java.util.*;

/**
 * Revised Start phase for Da Vinci Code (DVC):
 *  - Deterministic color distribution at start (see below)
 *  - Players must locally reorder then mark themselves settled
 *  - View exposes awaiting = SETTLE_POSITION until all settled; then transit to runtime
 *
 * Distribution:
 *  - 2 or 3 players: each receives exactly 2 BLACK + 2 WHITE (4 cards)
 *  - 4 players: randomly choose two players to receive 2 BLACK + 1 WHITE, remaining two get 2 WHITE + 1 BLACK
 */
public class DVCStartPhase extends StartPhase {
    private final List<String> playerIds;
    private final DVCDeck deck = new DVCDeck();
    private final List<DVCPlayer> players = new ArrayList<>();
    private DVCBoard board;
    private final Set<String> settledSet = new HashSet<>();
    private boolean entered;

    public DVCStartPhase(List<String> playerIds) { this.playerIds = Objects.requireNonNull(playerIds); }

    public DVCDeck deck() { return deck; }
    public DVCBoard board() { return board; }
    public List<DVCPlayer> players() { return players; }

    @Override public void enter() {
        if (entered) return; entered = true;
        if (playerIds.size() < 2 || playerIds.size() > 4) throw new IllegalArgumentException("Players must be 2-4");
        deck.initialize();
        for (String id : playerIds) players.add(new DVCPlayer(id, id.toUpperCase().startsWith("BOT")));
        if (playerIds.size() == 4) {
            List<DVCPlayer> order = new ArrayList<>(players);
            Collections.shuffle(order, new Random());
            for (int i=0;i<order.size();i++) {
                DVCPlayer p = order.get(i);
                boolean groupA = i < 2; // first two players => 2B1W
                int blacks = groupA ? 2 : 1;
                int whites = groupA ? 1 : 2;
                dealColor(p, DVCCard.Color.BLACK, blacks);
                dealColor(p, DVCCard.Color.WHITE, whites);
            }
        } else { // 2 or 3 players
            for (DVCPlayer p : players) {
                dealColor(p, DVCCard.Color.BLACK, 2);
                dealColor(p, DVCCard.Color.WHITE, 2);
            }
        }
        board = new DVCBoard(players);
    }

    private void dealColor(DVCPlayer p, DVCCard.Color color, int count) {
        int given = 0; List<DVCCard> buffer = new ArrayList<>();
        while (given < count) {
            DVCCard c = deck.draw(); if (c == null) break; // safety
            if (c.getColor() == color) { p.dealRaw(c); given++; } else buffer.add(c);
        }
        if (!buffer.isEmpty()) {
            // Return off-color cards to bottom to preserve randomness for subsequent draws
            for (DVCCard b : buffer) deck.putBottom(b);
        }
    }

    /** Player finished arranging initial cards. */
    public void settled(String playerId) { if (players.stream().anyMatch(p->p.getId().equals(playerId))) settledSet.add(playerId); }
    public boolean allSettled() { return settledSet.size() == players.size(); }

    /** Reorder a player's hand using provided concatenated cardId string. */
    public boolean reorderHand(String playerId, String handString) {
        if (handString == null || handString.isBlank()) return false;
        DVCPlayer target = players.stream().filter(p->p.getId().equals(playerId)).findFirst().orElse(null);
        if (target == null) return false;
        var snapshot = new java.util.ArrayList<>(target.hand().snapshot());
        java.util.Map<String, java.util.Queue<DVCCard>> multimap = new java.util.HashMap<>();
        for (DVCCard c : snapshot) multimap.computeIfAbsent(c.cardId(), k->new java.util.ArrayDeque<>()).add(c);
        java.util.List<DVCCard> ordered = new java.util.ArrayList<>();
        int idx = 0; String s = handString.trim();
        while (idx < s.length()) {
            char colorChar = s.charAt(idx++);
            if (colorChar!='B' && colorChar!='W') return false;
            StringBuilder val = new StringBuilder();
            while (idx < s.length() && s.charAt(idx) != '≤') { val.append(s.charAt(idx++)); }
            if (idx >= s.length() || s.charAt(idx)!='≤') return false;
            idx++;
            String token = colorChar + val.toString() + "≤";
            var q = multimap.get(token); if (q==null || q.isEmpty()) return false;
            ordered.add(q.poll());
        }
        if (ordered.size() != snapshot.size()) return false;
        target.hand().setExactOrder(ordered);
        return true;
    }

    /** Build a view for start phase with awaiting=SETTLE_POSITION. */
    public DVCView buildView(String perspectivePlayerId) {
        if (board == null) return null;
        DVCBoardView boardView = new DVCBoardView("DVC", 0L, 1, 0, deck.remaining(), DVCRuntimePhase.Awaiting.SETTLE_POSITION.name(), null);
        List<DVCPlayerView> pviews = new ArrayList<>();
        for (var p : players) {
            boolean self = p.getId().equals(perspectivePlayerId);
            var snapshot = p.hand().snapshot();
            int hidden = (int) snapshot.stream().filter(c->!c.isFaceUp()).count();
            List<String> cards = self
                ? snapshot.stream().map(DVCCard::frontDisplay).toList()
                : snapshot.stream().map(DVCCard::backDisplay).toList();
            pviews.add(new DVCPlayerView(p.getId(), p.isBot(), snapshot.size(), hidden, cards));
        }
        return new DVCView(boardView, List.copyOf(pviews), perspectivePlayerId);
    }

    @Override public DVCRuntimePhase transit() {
        if (!allSettled()) throw new IllegalStateException("Not all players settled");
        return new DVCRuntimePhase(deck, board, players);
    }
}

