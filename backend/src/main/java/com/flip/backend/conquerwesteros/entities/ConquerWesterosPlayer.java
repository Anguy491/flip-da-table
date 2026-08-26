package com.flip.backend.conquerwesteros.entities;

import com.flip.backend.game.entities.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ConquerWesterosPlayer extends Player {
    private final String name;
    private final Set<String> faceUpStrongholds = new LinkedHashSet<>();
    private final Map<String, List<String>> completedClans = new LinkedHashMap<>();

    public ConquerWesterosPlayer(String id, String name) {
        super(id, false);
        this.name = Objects.requireNonNull(name, "name");
    }

    public String name() { return name; }
    public Set<String> faceUpStrongholds() { return Set.copyOf(faceUpStrongholds); }
    public Map<String, List<String>> completedClans() { return Map.copyOf(completedClans); }
    public int strongholdCount() {
        return faceUpStrongholds.size() + completedClans.values().stream().mapToInt(List::size).sum();
    }

    public boolean ownsFaceUp(String strongholdId) { return faceUpStrongholds.contains(strongholdId); }
    public boolean ownsLocked(String strongholdId) {
        return completedClans.values().stream().anyMatch(cards -> cards.contains(strongholdId));
    }

    public void addFaceUp(String strongholdId) {
        if (!faceUpStrongholds.add(Objects.requireNonNull(strongholdId, "strongholdId"))) {
            throw new IllegalStateException("stronghold already owned");
        }
    }

    public void removeFaceUp(String strongholdId) {
        if (!faceUpStrongholds.remove(strongholdId)) throw new IllegalStateException("stronghold is not face up");
    }

    public void completeClan(String clan, List<String> strongholdIds) {
        if (completedClans.containsKey(clan)) throw new IllegalStateException("clan already completed");
        var cards = List.copyOf(strongholdIds);
        if (!faceUpStrongholds.containsAll(cards)) throw new IllegalStateException("clan strongholds are not all owned");
        faceUpStrongholds.removeAll(cards);
        completedClans.put(clan, cards);
    }

    public void restore(Set<String> faceUp, Map<String, List<String>> completed) {
        faceUpStrongholds.clear();
        completedClans.clear();
        if (faceUp != null) faceUpStrongholds.addAll(faceUp);
        if (completed != null) completed.forEach((clan, ids) -> completedClans.put(clan, List.copyOf(ids)));
    }

    public List<String> allStrongholds() {
        var result = new ArrayList<>(faceUpStrongholds);
        completedClans.values().forEach(result::addAll);
        return List.copyOf(result);
    }
}
