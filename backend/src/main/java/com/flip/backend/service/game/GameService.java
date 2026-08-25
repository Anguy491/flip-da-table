package com.flip.backend.service.game;

import com.flip.backend.api.dto.LobbyDtos.StartGameRequest;
import com.flip.backend.api.dto.LobbyDtos.StartGameResponse;
import com.flip.backend.persistence.GameEntity;
import com.flip.backend.persistence.GameRepository;
import com.flip.backend.persistence.SessionEntity;
import com.flip.backend.persistence.SessionRepository;
import com.flip.backend.security.GameStateConflictException;

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

    /** Server-authoritative lobby and lifecycle capabilities for this game. */
    public GameCapabilities capabilities() {
        return new GameCapabilities(2, 10, true, true, 1);
    }

    /** Start first round for a session. */
    public abstract StartGameResponse startFirst(String sessionId, StartGameRequest req);

    /** Start next round (roundIndex auto-increment). */
    public abstract StartGameResponse startNext(String sessionId, StartGameRequest req);

    /** Build a view for the already-authorized player. */
    public abstract Object viewFor(String gameId, String playerId);

    /** Return whether the in-memory runtime for this round has reached a terminal state. */
    protected abstract boolean isFinished(String gameId);

    protected SessionEntity beginFirstRound(String sessionId) {
        var session = sessions.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found"));
        if (games.findTopBySessionIdOrderByRoundIndexDesc(sessionId).isPresent()) {
            throw new GameStateConflictException("first round already started");
        }
        session.setState("RUNNING");
        return session;
    }

    protected RoundStart beginNextRound(String sessionId) {
        var session = sessions.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found"));
        var previous = games.findTopBySessionIdOrderByRoundIndexDesc(sessionId)
                .orElseThrow(() -> new GameStateConflictException("no previous round"));
        if (!isFinished(previous.getId())) {
            throw new GameStateConflictException("previous round is not finished");
        }
        previous.setState("ENDED");
        games.save(previous);
        return new RoundStart(session, previous.getRoundIndex() + 1);
    }

    protected StartGameResponse persistRound(SessionEntity session, int roundIndex) {
        String gameType = session.getGameType().toUpperCase();
        String gameId = session.getId() + ":" + gameType + ":r" + roundIndex;
        var g = GameEntity.builder()
                .id(gameId)
                .sessionId(session.getId())
                .roundIndex(roundIndex)
                .gameType(gameType)
                .state("RUNNING")
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

    protected record RoundStart(SessionEntity session, int roundIndex) {}
}
