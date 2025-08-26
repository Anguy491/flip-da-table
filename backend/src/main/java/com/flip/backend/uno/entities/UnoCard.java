package com.flip.backend.uno.entities;

import com.flip.backend.game.entities.Card;
import java.util.Objects;

public class UnoCard extends Card {
	public enum Color { RED, YELLOW, GREEN, BLUE, WILD }
	public enum Type { NUMBER, SKIP, REVERSE, DRAW_TWO, WILD, WILD_DRAW_FOUR }

	private final Color color; // WILD for wild variants
	private final Type type;
	private final Integer number; // 0-9 if type == NUMBER else null

	private UnoCard(Color color, Type type, Integer number) {
		this.color = Objects.requireNonNull(color);
		this.type = Objects.requireNonNull(type);
		this.number = number;
	}

	public static UnoCard number(Color color, int num) {
		if (color == Color.WILD) throw new IllegalArgumentException("Wild color not allowed for number card");
		if (num < 0 || num > 9) throw new IllegalArgumentException("Number must be 0-9");
		return new UnoCard(color, Type.NUMBER, num);
	}
	public static UnoCard skip(Color color) { return new UnoCard(color, Type.SKIP, null); }
	public static UnoCard reverse(Color color) { return new UnoCard(color, Type.REVERSE, null); }
	public static UnoCard drawTwo(Color color) { return new UnoCard(color, Type.DRAW_TWO, null); }
	public static UnoCard wild() { return new UnoCard(Color.WILD, Type.WILD, null); }
	public static UnoCard wildDrawFour() { return new UnoCard(Color.WILD, Type.WILD_DRAW_FOUR, null); }

	public Color getColor() { return color; }
	public Type getType() { return type; }
	public Integer getNumber() { return number; }

	@Override
	public String getDisplay() {
		if (type == Type.NUMBER) return color + " " + number;
		if (type == Type.WILD || type == Type.WILD_DRAW_FOUR) return type.name();
		return color + " " + type.name();
	}

	@Override
	public String toString() { return getDisplay(); }
}
