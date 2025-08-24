package com.flip.backend.game.uno;

import java.util.ArrayList;
import java.util.List;

/** Player state with mutable hand list (managed within engine purely). */
public class UnoPlayer {
    private String id;
    private boolean bot;
    private List<UnoCard> hand = new ArrayList<>();

    public UnoPlayer() { /* for Jackson */ }
    public UnoPlayer(String id, boolean bot) { this.id = id; this.bot = bot; }

    public String id() { return id; }
    public boolean bot() { return bot; }
    public List<UnoCard> hand() { return hand; }

    // Bean-style getters/setters for Jackson
    public String getId() { return id; }
    public boolean isBot() { return bot; }
    public List<UnoCard> getHand() { return hand; }
    public void setId(String id) { this.id = id; }
    public void setBot(boolean bot) { this.bot = bot; }
    public void setHand(List<UnoCard> hand) { this.hand = hand != null ? hand : new ArrayList<>(); }
}
