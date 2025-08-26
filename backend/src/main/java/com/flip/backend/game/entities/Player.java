package com.flip.backend.game.entities;

import java.util.Objects;

public abstract class Player {
	private final String id;

	protected Player(String id) {
		this.id = Objects.requireNonNull(id, "id");
	}

	public String getId() { return id; }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Player other)) return false;
		return id.equals(other.id);
	}

	@Override
	public int hashCode() { return id.hashCode(); }

	@Override
	public String toString() { return getClass().getSimpleName()+"{"+id+'}'; }
}
