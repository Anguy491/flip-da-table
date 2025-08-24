package com.flip.backend.game.service;

import com.flip.backend.api.dto.LobbyDtos.StartGameRequest;
import com.flip.backend.game.engine.GameType;
import com.flip.backend.persistence.SessionEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/** Placeholder: only validates player count (2-4) and leaves state uninitialized (CREATED). */
@Component
public class DavinciGameInitializer implements GameInitializer {
    @Override
    public GameType supports() { return GameType.DAVINCI; }

    @Override
    public GameInitializationResult initialize(SessionEntity session, StartGameRequest request) {
        long count = request.players().stream().filter(p -> p.name()!=null && !p.name().isBlank()).count();
        if (count < 2 || count > 4) throw new IllegalArgumentException("players must be 2-4 for DaVinci");
        return new GameInitializationResult("CREATED", null, List.of());
    }
}
