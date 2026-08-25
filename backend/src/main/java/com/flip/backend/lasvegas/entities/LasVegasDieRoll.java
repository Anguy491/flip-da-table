package com.flip.backend.lasvegas.entities;

/** One die in the currently public roll. */
public record LasVegasDieRoll(int face, boolean big) {
    public LasVegasDieRoll {
        if (face < 1 || face > 6) throw new IllegalArgumentException("die face must be 1-6");
    }
}
