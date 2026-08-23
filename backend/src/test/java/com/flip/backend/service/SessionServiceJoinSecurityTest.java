package com.flip.backend.service;

import com.flip.backend.persistence.SessionEntity;
import com.flip.backend.persistence.SessionMemberEntity;
import com.flip.backend.persistence.SessionMemberRepository;
import com.flip.backend.persistence.SessionRepository;
import com.flip.backend.persistence.UserEntity;
import com.flip.backend.persistence.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionServiceJoinSecurityTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsANewMemberWhenTheLockedRoomIsFull() {
        Fixture fixture = fixture("LOBBY");
        when(fixture.members.findBySessionIdAndUserId("session-1", 3L)).thenReturn(Optional.empty());
        when(fixture.members.countBySessionId("session-1")).thenReturn(2L);

        assertThrows(IllegalArgumentException.class, () -> fixture.service.join("session-1"));
        verify(fixture.members, never()).save(any());
    }

    @Test
    void rejectsANewMemberAfterStartButKeepsExistingMembershipIdempotent() {
        Fixture fixture = fixture("RUNNING");
        when(fixture.members.findBySessionIdAndUserId("session-1", 3L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> fixture.service.join("session-1"));

        var existing = SessionMemberEntity.builder().sessionId("session-1").userId(3L).nickname("Carol").build();
        when(fixture.members.findBySessionIdAndUserId("session-1", 3L)).thenReturn(Optional.of(existing));
        when(fixture.members.findBySessionId("session-1")).thenReturn(List.of(existing));
        assertDoesNotThrow(() -> fixture.service.join("session-1"));
        verify(fixture.members, never()).save(any());
    }

    private Fixture fixture(String state) {
        var sessions = mock(SessionRepository.class);
        var users = mock(UserRepository.class);
        var members = mock(SessionMemberRepository.class);
        var messaging = mock(SimpMessagingTemplate.class);
        var user = UserEntity.builder().id(3L).email("carol@example.com").nickname("Carol").build();
        var session = SessionEntity.builder()
                .id("session-1")
                .ownerId(1L)
                .gameType("UNO")
                .maxPlayers(2)
                .state(state)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("carol@example.com", null, List.of())
        );
        when(users.findByEmailIgnoreCase("carol@example.com")).thenReturn(Optional.of(user));
        when(sessions.findByIdForUpdate("session-1")).thenReturn(Optional.of(session));
        return new Fixture(new SessionService(sessions, users, members, messaging), members);
    }

    private record Fixture(SessionService service, SessionMemberRepository members) {}
}
