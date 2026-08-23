package com.flip.backend.service.game;

import com.flip.backend.api.dto.LobbyDtos.StartGameRequest;
import com.flip.backend.api.dto.LobbyDtos.StartGameResponse;
import com.flip.backend.persistence.GameEntity;
import com.flip.backend.persistence.GameRepository;
import com.flip.backend.persistence.SessionEntity;
import com.flip.backend.persistence.SessionRepository;
import com.flip.backend.security.GameStateConflictException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameServiceLifecycleTest {
    private final SessionRepository sessions = mock(SessionRepository.class);
    private final GameRepository games = mock(GameRepository.class);
    private final TestGameService service = new TestGameService(sessions, games);

    @Test
    void rejectsASecondInitialRoundWhileHoldingTheSessionBoundary() {
        var session = SessionEntity.builder().id("session-1").state("RUNNING").build();
        when(sessions.findByIdForUpdate("session-1")).thenReturn(Optional.of(session));
        when(games.findTopBySessionIdOrderByRoundIndexDesc("session-1"))
                .thenReturn(Optional.of(GameEntity.builder().id("game-1").roundIndex(1).build()));

        assertThrows(GameStateConflictException.class, () -> service.first("session-1"));
    }

    @Test
    void rejectsNextRoundUntilTheLatestRuntimeIsTerminal() {
        var previous = GameEntity.builder().id("game-1").roundIndex(1).state("RUNNING").build();
        when(sessions.findByIdForUpdate("session-1"))
                .thenReturn(Optional.of(SessionEntity.builder().id("session-1").build()));
        when(games.findTopBySessionIdOrderByRoundIndexDesc("session-1")).thenReturn(Optional.of(previous));

        service.finished = false;
        assertThrows(GameStateConflictException.class, () -> service.next("session-1"));
    }

    @Test
    void endsLatestRoundAndAllocatesExactlyTheFollowingIndex() {
        var session = SessionEntity.builder().id("session-1").build();
        var previous = GameEntity.builder().id("game-7").roundIndex(7).state("RUNNING").build();
        when(sessions.findByIdForUpdate("session-1")).thenReturn(Optional.of(session));
        when(games.findTopBySessionIdOrderByRoundIndexDesc("session-1")).thenReturn(Optional.of(previous));

        service.finished = true;
        var next = service.next("session-1");

        assertEquals(8, next.roundIndex());
        assertEquals("ENDED", previous.getState());
        verify(games).save(previous);
    }

    private static final class TestGameService extends GameService {
        private boolean finished;

        private TestGameService(SessionRepository sessions, GameRepository games) {
            super(sessions, games);
        }

        SessionEntity first(String sessionId) { return beginFirstRound(sessionId); }
        RoundStart next(String sessionId) { return beginNextRound(sessionId); }

        @Override public boolean supports(String gameType) { return true; }
        @Override public StartGameResponse startFirst(String sessionId, StartGameRequest req) { throw new UnsupportedOperationException(); }
        @Override public StartGameResponse startNext(String sessionId, StartGameRequest req) { throw new UnsupportedOperationException(); }
        @Override public Object viewFor(String gameId, String playerId) { throw new UnsupportedOperationException(); }
        @Override protected boolean isFinished(String gameId) { return finished; }
    }
}
