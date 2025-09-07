package com.flip.backend.game.engine.event;

import java.util.ArrayDeque;
import java.util.Queue;

public class EventQueue {
	private final Queue<GameEvent> q = new ArrayDeque<>();

	public void enqueue(GameEvent e) { if (e != null) q.add(e); }
	public GameEvent poll() { return q.poll(); }
	public GameEvent peek() { return q.peek(); }
	public boolean isEmpty() { return q.isEmpty(); }
	public int size() { return q.size(); }
	public void clear() { q.clear(); }
}
