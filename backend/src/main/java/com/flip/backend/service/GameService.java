package com.flip.backend.service;

import com.flip.backend.api.dto.LobbyDtos.*;
import com.flip.backend.persistence.*;
import com.flip.backend.game.engine.GameType;
import com.flip.backend.game.service.GameInitializer;
import com.flip.backend.game.service.GameInitializationResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service("gameService")
public class GameService {
    private final SessionRepository sessions;
    private final GameRepository games;
    private final Map<GameType, GameInitializer> initializerMap;

    public GameService(SessionRepository sessions, GameRepository games, java.util.List<GameInitializer> initializers) {
        this.sessions = sessions;
        this.games = games;
        this.initializerMap = new EnumMap<>(GameType.class);
        for (GameInitializer gi : initializers) {
            this.initializerMap.put(gi.supports(), gi);
        }
    }

    public StartGameResponse startFirst(String sessionId, StartGameRequest req) {
        var s = sessions.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("session not found"));

    GameType type = GameType.valueOf(s.getGameType().toUpperCase());
    GameInitializer initializer = initializerMap.get(type);
    if (initializer == null) throw new IllegalStateException("No initializer for game type " + type);
    GameInitializationResult initResult = initializer.initialize(s, req);

        // 生成 gameId：<sessionId>:<gameType>:r<roundIndex>
        int roundIndex = 1;
        String gameId = s.getId() + ":" + s.getGameType().toUpperCase() + ":r" + roundIndex;

        var builder = GameEntity.builder()
                .id(gameId)
                .sessionId(s.getId())
                .roundIndex(roundIndex)
                .gameType(s.getGameType().toUpperCase())
                .createdAt(Instant.now());

    builder.state(initResult.lifecycleState()).stateJson(initResult.stateJson());
    games.save(builder.build());
    // 简单策略：选择第一个非 bot 玩家作为启动调用者的 playerId（后续可根据认证用户映射）
    String myPlayerId = initResult.players().stream().filter(p -> !p.bot()).map(p -> p.playerId()).findFirst()
        .orElseGet(() -> initResult.players().isEmpty() ? null : initResult.players().get(0).playerId());
    return new StartGameResponse(gameId, roundIndex, initResult.players(), type.name(), myPlayerId);
    }
}
