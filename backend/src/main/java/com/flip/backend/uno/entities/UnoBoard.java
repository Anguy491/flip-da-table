package com.flip.backend.uno.entities;

import com.flip.backend.game.entities.Board;
import java.util.List;

public class UnoBoard extends Board<UnoPlayer> {
	private UnoCard lastPlayedCard;
	private UnoCard.Color activeColor; // may be null until a card is played

	public UnoBoard(List<UnoPlayer> players) { super(players); }
	protected UnoBoard() { super(); }

	public UnoCard lastPlayedCard() { return lastPlayedCard; }
	public UnoCard.Color activeColor() { return activeColor; }

	public void setLastPlayedCard(UnoCard card) { this.lastPlayedCard = card; }
	public void setActiveColor(UnoCard.Color color) { this.activeColor = color; }

	/**
	 * Convenience atomic apply: set top card and optionally override color (wild).
	 * If chosenColor is null and card not null & not wild, activeColor becomes card.color.
	 */
	public void applyTop(UnoCard card, UnoCard.Color chosenColor) {
		setLastPlayedCard(card);
		if (chosenColor != null) {
			setActiveColor(chosenColor);
		} else if (card != null && card.getColor() != UnoCard.Color.WILD) {
			setActiveColor(card.getColor());
		}
	}
}
