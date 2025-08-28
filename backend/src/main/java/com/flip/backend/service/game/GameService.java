package com.flip.backend.service.game;

import com.flip.backend.api.dto.LobbyDtos.StartGameRequest;
import com.flip.backend.api.dto.LobbyDtos.StartGameResponse;
import com.flip.backend.persistence.GameEntity;
import com.flip.backend.persistence.GameRepository;
import com.flip.backend.persistence.SessionEntity;
import com.flip.backend.persistence.SessionRepository;

import java.time.Instant;

/**
 * Abstract game service - concrete game types (UNO, DAVINCI, etc.) implement validation & startup specifics.
 */
public abstract class GameService {
    protected final SessionRepository sessions;
    protected final GameRepository games;

    protected GameService(SessionRepository sessions, GameRepository games) {
        this.sessions = sessions;
        this.games = games;
    }

    /** Return true if this service supports given gameType (normalized upper-case). */
    public abstract boolean supports(String gameType);

    /** Start first round for a session. */
    public abstract StartGameResponse startFirst(String sessionId, StartGameRequest req);

    /** Start next round (roundIndex auto-increment). */
    public abstract StartGameResponse startNext(String sessionId, StartGameRequest req);

    protected StartGameResponse persistRound(SessionEntity session, int roundIndex) {
        String gameType = session.getGameType().toUpperCase();
        String gameId = session.getId() + ":" + gameType + ":r" + roundIndex;
        var g = GameEntity.builder()
                .id(gameId)
                .sessionId(session.getId())
                .roundIndex(roundIndex)
                .gameType(gameType)
                .state("CREATED")
                .createdAt(Instant.now())
                .build();
        games.save(g);
        return new StartGameResponse(gameId, roundIndex, null, java.util.List.of(), null);
    }

    protected int nextRoundIndex(String sessionId) {
        Integer max = games.findMaxRoundIndexBySessionId(sessionId);
        return max == null ? 1 : max + 1;
    }

    protected int countValidPlayers(StartGameRequest req) {
        return (int) req.players().stream().filter(p -> p.name()!=null && !p.name().isBlank()).count();
    }
}
