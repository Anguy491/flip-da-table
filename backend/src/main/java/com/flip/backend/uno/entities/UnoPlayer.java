package com.flip.backend.uno.entities;

import com.flip.backend.game.entities.Player;

public class UnoPlayer extends Player {
	private final UnoHand hand;

	public UnoPlayer(String id) {
		super(id);
		this.hand = new UnoHand();
	}

	public UnoHand getHand() { return hand; }

	public int cardCount() { return hand.size(); }

	public void giveCard(UnoCard card) { hand.add(card); }

	public boolean playCard(UnoCard card) { return hand.remove(card); }

	public boolean hasPlayable(java.util.function.Predicate<UnoCard> predicate) {
		return hand.view().stream().anyMatch(predicate);
	}
}
