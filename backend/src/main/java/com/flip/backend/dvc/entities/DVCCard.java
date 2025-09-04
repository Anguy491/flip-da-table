package com.flip.backend.dvc.entities;

import com.flip.backend.game.entities.Card;
import java.util.Objects;

/**
 * Da Vinci Code (DVC) card: two colors (BLACK/WHITE). Each color has numbers 0-11 and one Joker ("-").
 * A Joker is represented by a null number and flag. Ordering rules (for non-Joker): ascending number; if same number
 * then BLACK < WHITE. Joker can be placed anywhere in a player's sequence (runtime decides position).
 */
public class DVCCard extends Card {
    public enum Color { BLACK, WHITE }

    private final Color color;
    private final Integer number; // 0-11; null when joker
    private final boolean joker;
    // faceUp == true means card revealed (front side shown: color + number or joker '-').
    // faceUp == false means backside visible: show only color + " ≤" symbol (per requirement).
    private boolean faceUp = false;

    private DVCCard(Color color, Integer number, boolean joker) {
        this.color = Objects.requireNonNull(color, "color");
        this.number = number;
        this.joker = joker;
    }

    public static DVCCard number(Color color, int n) {
        if (n < 0 || n > 11) throw new IllegalArgumentException("Number must be 0-11");
        return new DVCCard(color, n, false);
    }

    public static DVCCard joker(Color color) { return new DVCCard(color, null, true); }

    public Color getColor() { return color; }
    public Integer getNumber() { return number; }
    public boolean isJoker() { return joker; }
    public boolean isFaceUp() { return faceUp; }
    public void reveal() { this.faceUp = true; }

    /** Front face display (only when faceUp). */
    public String frontDisplay() {
        if (joker) return color.name()+" -";
        return color.name()+" "+number;
    }

    /** Back face display (hidden info): color + " ≤". */
    public String backDisplay() { return color.name()+" ≤"; }

    /** Contextual display depending on face state. */
    @Override
    public String getDisplay() { return faceUp ? frontDisplay() : backDisplay(); }

    /** Stable card identifier for client ordering: B/W + (number or '_' for joker) + '≤'. */
    public String cardId() {
        char prefix = color == Color.BLACK ? 'B' : 'W';
        String val = joker ? "_" : String.valueOf(number);
        return prefix + val + "≤"; // always ends with ≤ per spec
    }

    /** Comparator semantics for automatic insertion (Jokers excluded). */
    public static int compareForOrder(DVCCard a, DVCCard b) {
        if (a == b) return 0;
        if (a.joker || b.joker) {
            // Jokers have no intrinsic order; caller should handle separately.
            return 0;
        }
        int numCmp = Integer.compare(a.number, b.number);
        if (numCmp != 0) return numCmp;
        // Same number: BLACK < WHITE
        if (a.color != b.color) {
            return a.color == Color.BLACK ? -1 : 1;
        }
        return 0;
    }

    @Override
    public String toString() { return getDisplay() + (faceUp ? " (R)" : ""); }
}
