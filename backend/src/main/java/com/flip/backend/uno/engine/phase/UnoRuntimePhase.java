package com.flip.backend.uno.engine.phase;

import com.flip.backend.game.engine.phase.RuntimePhase;
import com.flip.backend.uno.entities.*;
import com.flip.backend.game.engine.event.EventQueue;
import com.flip.backend.uno.engine.event.*;
import java.util.List;

/** Prototype UNO runtime loop without special card effects. */
public class UnoRuntimePhase extends RuntimePhase {
	private final UnoDeck deck;
	private final UnoBoard board;
	private final EventQueue queue = new EventQueue();
	private String winnerId;
	private UnoEndingPhase endingPhase; // populated when winner decided
	private int pendingAdvanceSteps = 1;

	public UnoRuntimePhase(UnoDeck deck, UnoBoard board, List<UnoPlayer> players) {
		this.deck = deck; this.board = board; // players list not needed for now
	}

	@Override public void enter() { /* nothing extra for now */ }

	@Override
	public String run() {
		while (winnerId == null) {
			runSingleTurn();
		}
		return winnerId;
	}

	/** Execute exactly one player's turn (for tests / internal loop). */
	public void runSingleTurn() {
		if (winnerId != null) return;
		UnoPlayer current = (UnoPlayer) board.currentPlayer();
		planTurn(current);
		processQueue();
		if (current.cardCount() == 0) { winnerId = current.getId(); endingPhase = new UnoEndingPhase(current); return; }
		board.step(pendingAdvanceSteps); board.tickTurn();
		pendingAdvanceSteps = 1;
	}

	public UnoEndingPhase endingPhase() { return endingPhase; }

	private void planTurn(UnoPlayer player) {
		UnoCard top = board.lastPlayedCard();
		UnoCard.Color active = board.activeColor();
		UnoCard playable = player.getHand().view().stream().filter(c -> canPlay(c, top, active)).findFirst().orElse(null);
		if (playable != null) {
			queue.enqueue(new UnoPlayCardEvent(board, deck, player, playable));
		} else {
			UnoDrawCardEvent draw = new UnoDrawCardEvent(deck, player);
			queue.enqueue(draw);
			// After draw we may attempt play of that single card (lazy decision in processing after draw)
		}
	}

	private void processQueue() {
		while (!queue.isEmpty()) {
			var e = queue.poll();
			if (e.isValid()) {
				e.execute();
				// If it's a draw, see if drawn is playable -> enqueue play event
				if (e instanceof UnoDrawCardEvent d) {
					UnoCard drawn = d.drawnCard();
					UnoPlayer player = (UnoPlayer) d.source();
					if (drawn != null && canPlay(drawn, board.lastPlayedCard(), board.activeColor())) {
						queue.enqueue(new UnoPlayCardEvent(board, deck, player, drawn));
					}
				} else if (e instanceof UnoPlayCardEvent pce) {
					pendingAdvanceSteps = pce.getAdvanceSteps();
				}
			}
		}
	}

	private boolean canPlay(UnoCard card, UnoCard top, UnoCard.Color activeColor) {
		if (card.getType() == UnoCard.Type.WILD || card.getType() == UnoCard.Type.WILD_DRAW_FOUR) return true; // simplified
		if (top == null) return true;
		if (card.getColor() != UnoCard.Color.WILD && card.getColor() == activeColor) return true;
		if (card.getType() == top.getType() && card.getType() != UnoCard.Type.NUMBER) return true;
		return card.getType() == UnoCard.Type.NUMBER && top.getType() == UnoCard.Type.NUMBER && card.getNumber().equals(top.getNumber());
	}
}
