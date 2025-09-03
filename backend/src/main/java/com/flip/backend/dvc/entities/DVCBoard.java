package com.flip.backend.dvc.entities;

import com.flip.backend.game.entities.Board;
import java.util.*;

/**
 * DVC board: extends generic circular turn board; adds helper to check elimination (all cards revealed) and
 * determine survivors. Elimination criterion: player.hiddenCount()==0.
 */
public class DVCBoard extends Board<DVCPlayer> {
	public DVCBoard(List<DVCPlayer> players) { super(players); }
	protected DVCBoard() { super(); }

	public boolean isEliminated(DVCPlayer p) { return p.hiddenCount() == 0; }

	/** Count players still in game (with at least one hidden). */
	public long activePlayerCount() {
		return snapshotOrder().stream().filter(pl -> pl.hiddenCount() > 0).count();
	}

	// Pending drawn card (not yet settled into hand) keyed by playerId
	private final Map<String, DVCCard> pending = new HashMap<>();

	public void setPending(String playerId, DVCCard card) { pending.put(playerId, card); }
	public DVCCard getPending(String playerId) { return pending.get(playerId); }
	public DVCCard removePending(String playerId) { return pending.remove(playerId); }
	public boolean hasPending(String playerId) { return pending.containsKey(playerId); }
}
