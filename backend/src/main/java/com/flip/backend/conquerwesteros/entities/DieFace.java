package com.flip.backend.conquerwesteros.entities;

public enum DieFace {
    MILITARY_1(1, "Military 1"),
    MILITARY_2(2, "Military 2"),
    MILITARY_3(3, "Military 3"),
    RAVEN(0, "Raven"),
    KNIGHT(0, "Knight"),
    CROWN(0, "Crown");

    private final int militaryStrength;
    private final String display;

    DieFace(int militaryStrength, String display) {
        this.militaryStrength = militaryStrength;
        this.display = display;
    }

    public int militaryStrength() { return militaryStrength; }
    public boolean isMilitary() { return militaryStrength > 0; }
    public String display() { return display; }
}
