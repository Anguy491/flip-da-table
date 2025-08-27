package com.flip.backend.game.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Directional circular seating board. Provides only primitive seat navigation & turn counting.
 */
public class Board<P extends Player> {

	protected static class Node<P extends Player> {
		P player;
		Node<P> next;
		Node<P> prev;
		Node(P player) { this.player = player; }
	}

	private Node<P> current; // current seat
	private int size;
	private int direction = 1; // +1 clockwise, -1 counter-clockwise
	private long turnCount = 0;

	public Board(List<P> players) {
		Objects.requireNonNull(players, "players");
		if (players.size() < 2) throw new IllegalArgumentException("Board requires at least 2 players");
		build(players);
	}

	protected Board() { /* for subclass no-arg then init via initSeats */ }

	protected void initSeats(List<P> players) {
		if (current != null) throw new IllegalStateException("Already initialized");
		if (players.size() < 2) throw new IllegalArgumentException("Board requires at least 2 players");
		build(players);
	}

	private void build(List<P> players) {
		Node<P> first = null; Node<P> prev = null;
		for (P p : players) {
			Node<P> n = new Node<>(p);
			if (first == null) first = n;
			if (prev != null) { prev.next = n; n.prev = prev; }
			prev = n;
			size++;
		}
		// close ring (lists size >=2 already validated)
		if (first != null && prev != null) {
			first.prev = prev;
			prev.next = first;
			current = first;
		}
	}

	public P currentPlayer() { return current.player; }
	public int direction() { return direction; }
	public long turnCount() { return turnCount; }
	public int size() { return size; }

	/** Peek the next player according to current direction without advancing. */
	public P peekNext() { return direction == 1 ? current.next.player : current.prev.player; }

	public void step(int k) {
		if (k < 1) throw new IllegalArgumentException("k must be >=1");
		for (int i = 0; i < k; i++) {
			current = direction == 1 ? current.next : current.prev;
		}
	}

	public void reverse() { direction *= -1; }
	public void tickTurn() { turnCount++; }

	public List<P> snapshotOrder() {
		List<P> list = new ArrayList<>(size);
		Node<P> n = current;
		for (int i = 0; i < size; i++) { list.add(n.player); n = n.next; }
		return list;
	}

	public boolean remove(String playerId) {
		Node<P> n = current;
		for (int i=0;i<size;i++) {
			if (n.player.getId().equals(playerId)) {
				if (size == 2) throw new IllegalStateException("Cannot shrink below 2 players");
				n.prev.next = n.next;
				n.next.prev = n.prev;
				if (n == current) current = n.next; // advance if removing current
				size--;
				return true;
			}
			n = n.next;
		}
		return false;
	}

	public void insertAfter(String afterPlayerId, P newPlayer) {
		Objects.requireNonNull(newPlayer);
		Node<P> n = current;
		for (int i=0;i<size;i++) {
			if (n.player.getId().equals(afterPlayerId)) {
				Node<P> add = new Node<>(newPlayer);
				Node<P> nxt = n.next;
				n.next = add; add.prev = n; add.next = nxt; nxt.prev = add;
				size++;
				return;
			}
			n = n.next;
		}
		throw new NoSuchElementException("Player id not found: "+afterPlayerId);
	}
}
