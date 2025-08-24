package com.flip.backend.game.service;

import com.flip.backend.api.dto.LobbyDtos.PlayerInfo;
import com.flip.backend.api.dto.LobbyDtos.StartGameRequest;
import com.flip.backend.game.engine.GameType;
import com.flip.backend.game.uno.UnoState;
import com.flip.backend.persistence.SessionEntity;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class UnoGameInitializer implements GameInitializer {
    private final GameStateSerializer serializer;

    public UnoGameInitializer(GameStateSerializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public GameType supports() { return GameType.UNO; }

    @Override
    public GameInitializationResult initialize(SessionEntity session, StartGameRequest request) {
        // validate players count >=2 (upper bound by session.maxPlayers if present)
        long count = request.players().stream().filter(p -> p.name()!=null && !p.name().isBlank()).count();
        if (count < 2) throw new IllegalArgumentException("players must be >=2 for UNO");
        List<PlayerInfo> infos = new ArrayList<>();
        List<UnoState.PlayerInitSpec> specs = new ArrayList<>();
        for (var p : request.players()) {
            if (p.name()==null || p.name().isBlank()) continue;
            String pid = UUID.randomUUID().toString();
            infos.add(new PlayerInfo(pid, p.name(), p.bot()));
            specs.add(new UnoState.PlayerInitSpec(pid, p.bot()));
        }
        UnoState state = UnoState.initial(specs);
        String json = serializer.serialize(state);
        return new GameInitializationResult("RUNNING", json, infos);
    }
}
