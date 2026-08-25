package com.flip.backend.lasvegas.engine.phase;

import com.flip.backend.api.dto.LobbyDtos.PlayerStartInfo;
import com.flip.backend.game.engine.phase.StartPhase;

import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class LasVegasStartPhase extends StartPhase {
    private final List<PlayerStartInfo> players;
    private final Random random;
    private LasVegasRuntimePhase runtime;

    public LasVegasStartPhase(List<PlayerStartInfo> players) {
        this(players, new SecureRandom());
    }

    public LasVegasStartPhase(List<PlayerStartInfo> players, Random random) {
        this.players = List.copyOf(players);
        this.random = Objects.requireNonNull(random, "random");
    }

    @Override
    public void enter() {
        if (runtime != null) throw new IllegalStateException("Las Vegas start phase already entered");
        runtime = LasVegasRuntimePhase.newGame(players, random);
    }

    @Override
    public LasVegasRuntimePhase transit() {
        if (runtime == null) throw new IllegalStateException("enter must be called before transit");
        return runtime;
    }
}
