package com.flip.backend.conquerwesteros.entities;

import com.flip.backend.game.entities.Board;

import java.util.List;
import java.util.NoSuchElementException;

public final class ConquerWesterosBoard extends Board<ConquerWesterosPlayer> {
    private final List<ConquerWesterosPlayer> seats;

    public ConquerWesterosBoard(List<ConquerWesterosPlayer> players) {
        super(players);
        seats = List.copyOf(players);
    }

    public List<ConquerWesterosPlayer> seats() { return seats; }

    public void moveToPlayer(String playerId) {
        for (int steps = 0; steps < size(); steps++) {
            if (currentPlayer().getId().equals(playerId)) return;
            step(1);
        }
        throw new NoSuchElementException("player not seated: " + playerId);
    }

    public void restoreCurrentPlayer(String playerId, long turnCount) {
        moveToPlayer(playerId);
        restoreTurnCount(turnCount);
    }
}
