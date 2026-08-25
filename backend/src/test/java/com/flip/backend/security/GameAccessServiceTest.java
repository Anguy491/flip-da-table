package com.flip.backend.security;

import com.flip.backend.api.dto.LobbyDtos.PlayerStartInfo;
import com.flip.backend.persistence.GameEntity;
import com.flip.backend.persistence.GameRepository;
import com.flip.backend.persistence.GamePlayerEntity;
import com.flip.backend.persistence.GamePlayerRepository;
import com.flip.backend.persistence.SessionEntity;
import com.flip.backend.persistence.SessionMemberEntity;
import com.flip.backend.persistence.SessionMemberRepository;
import com.flip.backend.persistence.SessionRepository;
import com.flip.backend.persistence.UserEntity;
import com.flip.backend.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameAccessServiceTest {
    @Test
    void bindsTheAuthenticatedMemberToOnlyTheirRegisteredPlayer() {
        Fixture fixture = fixture();
        fixture.access.registerPlayers(
                "game-1",
                List.of(fixture.member),
                List.of(new PlayerStartInfo("P1_ALICE", "Alice", false, true))
        );

        assertEquals("P1_ALICE", fixture.access.requireClaimedPlayer(fixture.authentication, "game-1", "P1_ALICE"));
        assertThrows(AccessDeniedException.class,
                () -> fixture.access.requireClaimedPlayer(fixture.authentication, "game-1", "P2_BOB"));
    }

    @Test
    void rejectsAnAuthenticatedUserWhoIsNotAGameSessionMember() {
        Fixture fixture = fixture();
        when(fixture.members.findBySessionIdAndUserId("session-1", fixture.user.getId())).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> fixture.access.requirePlayer(fixture.authentication, "game-1"));
    }

    @Test
    void restoresPlayerIdentityFromPersistentSeatMappingsAfterMemoryLoss() {
        var users = mock(UserRepository.class);
        var sessions = mock(SessionRepository.class);
        var members = mock(SessionMemberRepository.class);
        var games = mock(GameRepository.class);
        var persistentPlayers = mock(GamePlayerRepository.class);
        var authentication = mock(Authentication.class);
        var user = UserEntity.builder().id(7L).email("alice@example.com").nickname("Alice").build();
        var member = SessionMemberEntity.builder().sessionId("session-1").userId(7L).nickname("Alice").build();
        var game = GameEntity.builder().id("game-1").sessionId("session-1").build();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("alice@example.com");
        when(users.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(games.findById("game-1")).thenReturn(Optional.of(game));
        when(members.findBySessionIdAndUserId("session-1", 7L)).thenReturn(Optional.of(member));
        when(persistentPlayers.findByGameIdOrderBySeatIndexAsc("game-1")).thenReturn(List.of(
                GamePlayerEntity.builder().gameId("game-1").userId(7L).playerId("P1").seatIndex(0).build(),
                GamePlayerEntity.builder().gameId("game-1").userId(8L).playerId("P2").seatIndex(1).build()
        ));

        var access = new GameAccessService(users, sessions, members, games, new GamePlayerRegistry(), persistentPlayers);
        assertEquals("P1", access.requirePlayer(authentication, "game-1"));
        assertEquals("P1", access.playerIdForUser("game-1", 7L));
    }

    private Fixture fixture() {
        var users = mock(UserRepository.class);
        var sessions = mock(SessionRepository.class);
        var members = mock(SessionMemberRepository.class);
        var games = mock(GameRepository.class);
        var authentication = mock(Authentication.class);
        var user = UserEntity.builder().id(7L).email("alice@example.com").nickname("Alice").authVersion(0).build();
        var member = SessionMemberEntity.builder().id(11L).sessionId("session-1").userId(7L).nickname("Alice").build();
        var game = GameEntity.builder().id("game-1").sessionId("session-1").build();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("alice@example.com");
        when(users.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(games.findById("game-1")).thenReturn(Optional.of(game));
        when(members.findBySessionIdAndUserId("session-1", 7L)).thenReturn(Optional.of(member));

        var access = new GameAccessService(users, sessions, members, games, new GamePlayerRegistry());
        return new Fixture(access, members, authentication, user, member);
    }

    private record Fixture(
            GameAccessService access,
            SessionMemberRepository members,
            Authentication authentication,
            UserEntity user,
            SessionMemberEntity member
    ) {}
}
