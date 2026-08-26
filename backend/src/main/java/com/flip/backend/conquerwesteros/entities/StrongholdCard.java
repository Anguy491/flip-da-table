package com.flip.backend.conquerwesteros.entities;

import com.flip.backend.game.entities.Card;

import java.util.List;
import java.util.Objects;

public final class StrongholdCard extends Card {
    private final String id;
    private final String name;
    private final String clan;
    private final int points;
    private final List<BattleLine> lines;
    private final boolean kingsLanding;

    public StrongholdCard(String id, String name, String clan, int points, List<BattleLine> lines, boolean kingsLanding) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.clan = Objects.requireNonNull(clan, "clan");
        this.points = points;
        this.lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        this.kingsLanding = kingsLanding;
    }

    public String id() { return id; }
    public String name() { return name; }
    public String clan() { return clan; }
    public int points() { return points; }
    public List<BattleLine> lines() { return lines; }
    public boolean kingsLanding() { return kingsLanding; }

    @Override public String getDisplay() { return name; }
}
