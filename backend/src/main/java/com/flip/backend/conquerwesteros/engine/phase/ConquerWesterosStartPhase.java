package com.flip.backend.conquerwesteros.engine.phase;

import com.flip.backend.api.dto.LobbyDtos.PlayerStartInfo;
import com.flip.backend.conquerwesteros.engine.Campaign;
import com.flip.backend.game.engine.phase.StartPhase;

import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class ConquerWesterosStartPhase extends StartPhase {
    private final List<PlayerStartInfo> players;
    private final Campaign campaign;
    private final Random random;
    private ConquerWesterosRuntimePhase runtime;

    public ConquerWesterosStartPhase(List<PlayerStartInfo> players, Campaign campaign) {
        this(players, campaign, new SecureRandom());
    }

    public ConquerWesterosStartPhase(List<PlayerStartInfo> players, Campaign campaign, Random random) {
        this.players = List.copyOf(players);
        this.campaign = Objects.requireNonNull(campaign, "campaign");
        this.random = Objects.requireNonNull(random, "random");
    }

    @Override
    public void enter() {
        if (runtime != null) throw new IllegalStateException("Conquer Westeros start phase already entered");
        runtime = ConquerWesterosRuntimePhase.newGame(players, campaign, random);
    }

    @Override
    public ConquerWesterosRuntimePhase transit() {
        if (runtime == null) throw new IllegalStateException("enter must be called before transit");
        return runtime;
    }
}
