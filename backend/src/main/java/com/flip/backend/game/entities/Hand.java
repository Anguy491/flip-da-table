package com.flip.backend.game.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generic hand abstraction. Thread-unsafe simple list operations.
 */
public abstract class Hand<C extends Card> {
	protected final List<C> cards = new ArrayList<>();

	public void add(C card) { if (card != null) cards.add(card); }
	public void addAll(List<C> more) { if (more != null) cards.addAll(more); }
	public boolean remove(C card) { return cards.remove(card); }
	public int size() { return cards.size(); }
	public List<C> view() { return Collections.unmodifiableList(cards); }
	public void clear() { cards.clear(); }
}
