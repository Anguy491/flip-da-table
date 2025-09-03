package com.flip.backend.dvc.engine.phase;

import com.flip.backend.game.engine.phase.StartPhase;
import com.flip.backend.dvc.entities.*;
import java.util.*;

/**
 * Start phase for Da Vinci Code (DVC):
 * - Initialize deck
 * - Create players (bot heuristic: id starts with BOT)
 * - Deal initial cards (2-3 players: 4 each; 4 players: 3 each) WITHOUT ordering
 * - Build board
 * - Wait for all players to confirm ready (manual joker placement / reordering). Frontend calls ready(playerId).
 * - When all ready, transit() returns DVCRuntimePhase (otherwise transit() throws IllegalState if not all ready)
 */
public class DVCStartPhase extends StartPhase {
    private final List<String> playerIds;
    private final DVCDeck deck = new DVCDeck();
    private final List<DVCPlayer> players = new ArrayList<>();
    private DVCBoard board;
    private final Set<String> readySet = new HashSet<>();
    private boolean entered;

    public DVCStartPhase(List<String> playerIds) { this.playerIds = Objects.requireNonNull(playerIds); }

    public DVCDeck deck() { return deck; }
    public DVCBoard board() { return board; }
    public List<DVCPlayer> players() { return players; }

    @Override public void enter() {
        if (entered) return; entered = true;
        if (playerIds.size() < 2 || playerIds.size() > 4) throw new IllegalArgumentException("Players must be 2-4");
        deck.initialize();
        // create players
        for (String id : playerIds) {
            players.add(new DVCPlayer(id, id.toUpperCase().startsWith("BOT")));
        }
        // deal initial cards raw
        int dealCount = playerIds.size() == 4 ? 3 : 4;
        for (int r=0;r<dealCount;r++) {
            for (DVCPlayer p : players) {
                var c = deck.draw();
                if (c != null) p.dealRaw(c);
            }
        }
        board = new DVCBoard(players);
    }

    /** Mark a player ready after manual reordering on client side. */
    public void ready(String playerId) { if (players.stream().anyMatch(p->p.getId().equals(playerId))) readySet.add(playerId); }
    public boolean allReady() { return readySet.size() == players.size(); }

    @Override public DVCRuntimePhase transit() {
        if (!allReady()) throw new IllegalStateException("Not all players ready");
        return new DVCRuntimePhase(deck, board, players);
    }
}

