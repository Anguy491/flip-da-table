package com.flip.backend.lasvegas.entities;

import com.flip.backend.game.entities.Board;

import java.util.List;
import java.util.NoSuchElementException;

/** Circular turn board with a stable seat order independent of the active seat. */
public final class LasVegasBoard extends Board<LasVegasPlayer> {
    private final List<LasVegasPlayer> seats;

    public LasVegasBoard(List<LasVegasPlayer> players) {
        super(players);
        seats = List.copyOf(players);
    }

    public List<LasVegasPlayer> seats() { return seats; }

    public int seatIndex(String playerId) {
        for (int index = 0; index < seats.size(); index++) {
            if (seats.get(index).getId().equals(playerId)) return index;
        }
        return -1;
    }

    public void moveToPlayer(String playerId) {
        for (int steps = 0; steps < size(); steps++) {
            if (currentPlayer().getId().equals(playerId)) return;
            step(1);
        }
        throw new NoSuchElementException("player not seated: " + playerId);
    }

    /** Move clockwise to the next player who still has dice. */
    public boolean advanceToNextEligible() {
        for (int checked = 0; checked < size(); checked++) {
            step(1);
            if (currentPlayer().hasDiceRemaining()) return true;
        }
        return false;
    }

    public String nextSeatId(String playerId) {
        int index = seatIndex(playerId);
        if (index < 0) throw new NoSuchElementException("player not seated: " + playerId);
        return seats.get((index + 1) % seats.size()).getId();
    }

    public void restoreCurrentPlayer(String playerId, long turnCount) {
        moveToPlayer(playerId);
        restoreTurnCount(turnCount);
    }
}
