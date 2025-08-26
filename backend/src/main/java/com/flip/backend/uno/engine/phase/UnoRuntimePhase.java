package com.flip.backend.uno.engine.phase;

import com.flip.backend.game.engine.phase.RuntimePhase;
import com.flip.backend.uno.entities.*;
import java.util.List;

/** Prototype UNO runtime loop without special card effects. */
public class UnoRuntimePhase extends RuntimePhase {
	private final UnoDeck deck;
	private final UnoBoard board;
	private String winnerId;

	public UnoRuntimePhase(UnoDeck deck, UnoBoard board, List<UnoPlayer> players) {
		this.deck = deck; this.board = board; // players list not needed for now
	}

	@Override public void enter() { /* nothing extra for now */ }

	@Override
	public String run() {
		while (winnerId == null) {
			UnoPlayer current = (UnoPlayer) board.currentPlayer();
			playTurn(current);
			if (current.cardCount() == 0) {
				winnerId = current.getId();
				break;
			}
			board.step(1);
			board.tickTurn();
		}
		return winnerId;
	}

	private void playTurn(UnoPlayer player) {
		UnoCard top = board.lastPlayedCard();
		UnoCard.Color activeColor = board.activeColor();
		UnoCard playable = player.getHand().view().stream().filter(c -> canPlay(c, top, activeColor)).findFirst().orElse(null);
		if (playable == null) {
			UnoCard drawn = deck.draw();
			if (drawn != null) {
				player.giveCard(drawn);
				if (canPlay(drawn, top, activeColor)) playable = drawn;
			}
		}
		if (playable != null) {
			player.playCard(playable);
			deck.discard(playable);
			// For wild choose first non-wild color from hand else RED
			UnoCard.Color chosen = null;
			if (playable.getColor() == UnoCard.Color.WILD) {
				chosen = player.getHand().view().stream()
						.filter(c -> c.getColor() != UnoCard.Color.WILD)
						.map(UnoCard::getColor)
						.findFirst().orElse(UnoCard.Color.RED);
			}
			board.applyTop(playable, chosen);
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
