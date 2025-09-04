package com.flip.backend.dvc.entities;

import com.flip.backend.game.entities.Hand;
import java.util.List;
import java.util.Collections;

/**
 * DVC hand: maintains ordered sequence except Jokers which can be placed anywhere.
 * We store cards in a list preserving chosen order. Insert logic: when adding a non-Joker via auto placement,
 * find first position where compareForOrder(new, existing) < 0 (skipping jokers) and insert before; if none append.
 * Jokers default appended (caller can reposition later if UI allows). For initial dealing we auto-place sequentially.
 */
public class DVCHand extends Hand<DVCCard> {

	/** Add a card respecting ordering (used when card identity still hidden to others). */
	public void addOrdered(DVCCard card) {
		if (card == null) return;
		if (card.isJoker()) { cards.add(card); return; }
		for (int i=0;i<cards.size();i++) {
			DVCCard existing = cards.get(i);
			if (existing.isJoker()) continue; // jokers don't constrain position
			if (DVCCard.compareForOrder(card, existing) < 0) {
				cards.add(i, card); return;
			}
		}
		cards.add(card); // largest so far
	}

	/** Reveal a specific card instance. */
	public void reveal(DVCCard card) { if (card != null && cards.contains(card)) card.reveal(); }

	/** List of context-sensitive displays: if includeHiddenPlaceholder we still show back face for hidden. */
	public List<String> display(boolean includeHiddenPlaceholder) {
		return cards.stream().map(c -> c.getDisplay()).toList(); // getDisplay already handles face state
	}

	/** Immutable snapshot of internal order. */
	public List<DVCCard> snapshot() { return Collections.unmodifiableList(cards); }

	/** Add without ordering (used during initial deal before player manual arrangement). */
	public void addRaw(DVCCard c) { if (c != null) cards.add(c); }

	/** Replace internal order exactly with provided sequence (cards must match set). */
	public void setExactOrder(List<DVCCard> ordered) {
		if (ordered == null) return;
		if (ordered.size() != cards.size()) throw new IllegalArgumentException("size mismatch");
		// Validate multiset equality
		java.util.Map<DVCCard, Integer> counts = new java.util.HashMap<>();
		for (DVCCard c : cards) counts.merge(c, 1, Integer::sum);
		for (DVCCard c : ordered) {
			Integer left = counts.get(c); if (left == null || left == 0) throw new IllegalArgumentException("invalid card set");
			counts.put(c, left-1);
		}
		cards.clear();
		cards.addAll(ordered);
	}
}
