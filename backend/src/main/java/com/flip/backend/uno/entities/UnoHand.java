package com.flip.backend.uno.entities;

import com.flip.backend.game.entities.Hand;

public class UnoHand extends Hand<UnoCard> {
	public boolean removeByDisplay(String display) {
		return cards.removeIf(c -> c.getDisplay().equals(display));
	}
}
