package com.flip.backend.game.entities;

import java.util.Objects;

public abstract class Player {
	private final String id;
	private final boolean bot;

	protected Player(String id) { this(id, false); }
	protected Player(String id, boolean isBot) {
		this.id = Objects.requireNonNull(id, "id");
		this.bot = isBot;
	}

	public String getId() { return id; }
	public boolean isBot() { return bot; }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Player other)) return false;
		return id.equals(other.id);
	}

	@Override
	public int hashCode() { return id.hashCode(); }

	@Override
	public String toString() { return getClass().getSimpleName()+"{"+id+", bot="+bot+'}'; }
}
