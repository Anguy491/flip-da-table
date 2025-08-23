package com.flip.backend.service;

import com.flip.backend.api.dto.LobbyDtos.*;
import com.flip.backend.persistence.*;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class GameService {
    private final SessionRepository sessions;
    private final GameRepository games;

    public GameService(SessionRepository sessions, GameRepository games) {
        this.sessions = sessions;
        this.games = games;
    }

    public StartGameResponse startFirst(String sessionId, StartGameRequest req) {
        var s = sessions.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("session not found"));

        int playerCount = (int) req.players().stream().filter(p -> p.name()!=null && !p.name().isBlank()).count();

        // 仅以 DaVinci 为例：2-4 人
        if ("DAVINCI".equalsIgnoreCase(s.getGameType())) {
            if (playerCount < 2 || playerCount > 4) throw new IllegalArgumentException("players must be 2-4 for DaVinci");
        }

        // 生成 gameId：<sessionId>:<gameType>:r<roundIndex>
        int roundIndex = 1;
        String gameId = s.getId() + ":" + s.getGameType().toUpperCase() + ":r" + roundIndex;

        var g = GameEntity.builder()
                .id(gameId)
                .sessionId(s.getId())
                .roundIndex(roundIndex)
                .gameType(s.getGameType().toUpperCase())
                .state("CREATED")
                .createdAt(Instant.now())
                .build();
        games.save(g);

        return new StartGameResponse(gameId, roundIndex);
    }
}
