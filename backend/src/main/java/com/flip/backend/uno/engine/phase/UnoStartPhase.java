package com.flip.backend.uno.engine.phase;

import com.flip.backend.game.engine.phase.StartPhase;
import com.flip.backend.uno.entities.*;
import java.util.ArrayList;
import java.util.List;

/** Initializes UNO game: deck, players, initial hands, board, first discard. */
public class UnoStartPhase extends StartPhase {
	private final List<String> playerIds; // ordered, may contain markers for bots e.g. prefixed
	private UnoDeck deck;
	private UnoBoard board;
	private List<UnoPlayer> players;

	public UnoStartPhase(List<String> playerIds) { this.playerIds = playerIds; }

	public UnoDeck deck() { return deck; }
	public UnoBoard board() { return board; }
	public List<UnoPlayer> players() { return players; }

	@Override
	public void enter() {
		// 1. Create and init deck
		deck = new UnoDeck();
		deck.initialize();

		// 2. Create players (BOT id heuristic: id starts with "BOT" )
		players = new ArrayList<>(playerIds.size());
		for (String id : playerIds) {
			UnoPlayer p = id.toUpperCase().startsWith("BOT") ? new UnoBot(id) : new UnoPlayer(id);
			players.add(p);
		}

		// 3. Deal 7 cards to each in order (round-robin simple sequential draws sufficient)
		for (int round = 0; round < 7; round++) {
			for (UnoPlayer p : players) {
				p.giveCard(deck.draw());
			}
		}

		// 4. Initialize board with players, current = index 0, direction = +1 already default
		board = new UnoBoard(players);

		// 5. Flip initial top card: ensure colored (non wild) by drawing until non-wild, putting wilds to bottom
		UnoCard starter = drawFirstColored();
		deck.discard(starter);
		board.applyTop(starter, null);
	}

	@Override
	public UnoRuntimePhase transit() {
		UnoRuntimePhase runtime = new UnoRuntimePhase(deck, board, players);
		runtime.enter();
		return runtime;
	}

	private UnoCard drawFirstColored() {
		UnoCard c;
		int safety = 20; // avoid infinite loop improbable
		while ((c = deck.draw()) != null && safety-- > 0) {
			if (c.getColor() != UnoCard.Color.WILD) return c;
			deck.putBottom(c); // move wild to bottom and continue
		}
		return c; // fallback (could be null or wild)
	}
}
