package com.flip.backend.game.service;

import com.flip.backend.api.dto.LobbyDtos.StartGameRequest;
import com.flip.backend.game.engine.GameType;
import com.flip.backend.persistence.SessionEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/** Placeholder initializer for BOUNTY game (not yet implemented). */
@Component
public class BountyGameInitializer implements GameInitializer {
    @Override
    public GameType supports() { return GameType.BOUNTY; }

    @Override
    public GameInitializationResult initialize(SessionEntity session, StartGameRequest request) {
        // Basic validation: at least 2 players
        long count = request.players().stream().filter(p -> p.name()!=null && !p.name().isBlank()).count();
        if (count < 2) throw new IllegalArgumentException("players must be >=2 for Bounty (placeholder)");
        return new GameInitializationResult("CREATED", null, List.of());
    }
}
