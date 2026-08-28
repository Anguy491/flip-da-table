package com.flip.backend.conquerwesteros.engine;

public enum Campaign {
    WAR_OF_FIVE_KINGS("War of the Five Kings"),
    DANCE_OF_THE_DRAGONS("Dance of the Dragons"),
    WAR_OF_THE_USURPER("War of the Usurper"),
    AEGONS_CONQUEST("Aegon's Conquest");

    private final String display;

    Campaign(String display) { this.display = display; }
    public String display() { return display; }

    public static Campaign parse(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("campaign is required");
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unsupported Conquer Westeros campaign");
        }
    }
}
