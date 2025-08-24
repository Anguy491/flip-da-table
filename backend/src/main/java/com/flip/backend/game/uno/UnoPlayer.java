package com.flip.backend.game.uno;

import java.util.ArrayList;
import java.util.List;

/** Player state with mutable hand list (managed within engine purely). */
public class UnoPlayer {
    private final String id;
    private final List<UnoCard> hand = new ArrayList<>();

    public UnoPlayer(String id) { this.id = id; }
    public String id() { return id; }
    public List<UnoCard> hand() { return hand; }
}
