package com.flip.backend.game.entities;

import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Generic deck abstraction backed by a deque.
 */
public abstract class Deck<C extends Card> {

	private final Deque<C> drawPile = new ArrayDeque<>();
	private final Deque<C> discardPile = new ArrayDeque<>();
	private final SecureRandom random = new SecureRandom();

	/**
	 * Populate the deck with its initial set of cards.
	 */
	protected abstract List<C> buildInitialCards();

	/**
	 * Initialize / reset the deck to a fresh state.
	 */
	public void initialize() {
		drawPile.clear();
		discardPile.clear();
		List<C> cards = new ArrayList<>(Objects.requireNonNull(buildInitialCards(), "cards"));
		Collections.shuffle(cards, random);
		drawPile.addAll(cards);
	}

	public int remaining() { return drawPile.size(); }
	public int discards() { return discardPile.size(); }
	public int total() { return drawPile.size() + discardPile.size(); }

	// Aliases for view layer clarity
	public int remainingDraw() { return remaining(); }
	public int discardSize() { return discards(); }

	/**
	 * Draw a single card, reshuffling the discard pile into the draw pile if needed.
	 */
	public C draw() {
		if (drawPile.isEmpty()) {
			reshuffleFromDiscards();
		}
		return drawPile.pollFirst();
	}

	/**
	 * Discard a card into the discard pile.
	 */
	public void discard(C card) {
		if (card != null) discardPile.addFirst(card);
	}

	/**
	 * Return a card (typically just drawn) to the bottom of the draw pile without shuffling.
	 */
	public void putBottom(C card) {
		if (card != null) drawPile.addLast(card);
	}

	/**
	 * Moves all but the top-most discard back into draw pile and shuffles.
	 * (In many games you keep the last played card visible.)
	 */
	protected void reshuffleFromDiscards() {
		if (discardPile.isEmpty()) return;
		C top = discardPile.pollFirst();
		List<C> toShuffle = new ArrayList<>(discardPile);
		discardPile.clear();
		discardPile.addFirst(top); // keep top visible
		Collections.shuffle(toShuffle, random);
		drawPile.addAll(toShuffle);
	}
}
