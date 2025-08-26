package com.flip.backend.uno.entities;

import com.flip.backend.game.entities.Deck;
import java.util.ArrayList;
import java.util.List;

public class UnoDeck extends Deck<UnoCard> {

	@Override
	protected List<UnoCard> buildInitialCards() {
		List<UnoCard> cards = new ArrayList<>(108);
		// For each color add number cards: one 0, two of 1-9, two each of skip, reverse, draw two
		for (UnoCard.Color color : List.of(UnoCard.Color.RED, UnoCard.Color.YELLOW, UnoCard.Color.GREEN, UnoCard.Color.BLUE)) {
			cards.add(UnoCard.number(color, 0));
			for (int i = 1; i <= 9; i++) {
				cards.add(UnoCard.number(color, i));
				cards.add(UnoCard.number(color, i));
			}
			// action cards (2 each per color)
			for (int i = 0; i < 2; i++) {
				cards.add(UnoCard.skip(color));
				cards.add(UnoCard.reverse(color));
				cards.add(UnoCard.drawTwo(color));
			}
		}
		// 4 Wild and 4 Wild Draw Four
		for (int i = 0; i < 4; i++) {
			cards.add(UnoCard.wild());
			cards.add(UnoCard.wildDrawFour());
		}
		return cards;
	}
}
