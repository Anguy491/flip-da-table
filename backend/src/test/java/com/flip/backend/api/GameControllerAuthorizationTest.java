package com.flip.backend.api;

import com.flip.backend.api.dto.LobbyDtos.PlayerSpec;
import com.flip.backend.api.dto.LobbyDtos.PlayerStartInfo;
import com.flip.backend.api.dto.LobbyDtos.StartGameRequest;
import com.flip.backend.api.dto.LobbyDtos.StartGameResponse;
import com.flip.backend.persistence.SessionEntity;
import com.flip.backend.persistence.SessionMemberEntity;
import com.flip.backend.persistence.SessionMemberRepository;
import com.flip.backend.security.GameAccessService;
import com.flip.backend.service.game.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameControllerAuthorizationTest {
    @Test
    void rejectsANonOwnerBeforeStartingAnyGameService() {
        var service = mock(GameService.class);
        var access = mock(GameAccessService.class);
        var authentication = mock(Authentication.class);
        when(access.requireOwner(authentication, "session-1")).thenThrow(new AccessDeniedException("forbidden"));
        var controller = new GameController(List.of(service), mock(SessionMemberRepository.class), mock(SimpMessagingTemplate.class), access);

        assertThrows(AccessDeniedException.class, () -> controller.startFirst(
                "session-1",
                new StartGameRequest(1, List.of(new PlayerSpec("Mallory", false, true))),
                authentication
        ));
        verify(service, never()).startFirst(any(), any());
    }

    @Test
    void derivesHumanPlayersFromMembershipAndKeepsRequestedBots() {
        var service = mock(GameService.class);
        var access = mock(GameAccessService.class);
        var members = mock(SessionMemberRepository.class);
        var messaging = mock(SimpMessagingTemplate.class);
        var authentication = mock(Authentication.class);
        var session = SessionEntity.builder().id("session-1").ownerId(1L).gameType("UNO").maxPlayers(3).build();
        var alice = SessionMemberEntity.builder().id(1L).sessionId("session-1").userId(1L).nickname("Alice").build();
        var bob = SessionMemberEntity.builder().id(2L).sessionId("session-1").userId(2L).nickname("Bob").build();
        var startedPlayers = List.of(
                new PlayerStartInfo("P1_ALICE", "Alice", false, true),
                new PlayerStartInfo("P2_BOB", "Bob", false, true),
                new PlayerStartInfo("BOT1", "Bot 1", true, true)
        );
        var started = new StartGameResponse("game-1", 1, null, startedPlayers, null);

        when(access.requireOwner(authentication, "session-1")).thenReturn(session);
        when(members.findBySessionIdOrderByJoinedAtAscIdAsc("session-1")).thenReturn(List.of(alice, bob));
        when(service.supports("UNO")).thenReturn(true);
        when(service.startFirst(eq("session-1"), any())).thenReturn(started);
        when(access.playerIdForUser("game-1", 1L)).thenReturn("P1_ALICE");
        when(access.playerIdForUser("game-1", 2L)).thenReturn("P2_BOB");
        when(access.requirePlayer(authentication, "game-1")).thenReturn("P1_ALICE");
        when(service.viewFor(eq("game-1"), any())).thenAnswer(invocation -> "view-for-" + invocation.getArgument(1));
        var controller = new GameController(List.of(service), members, messaging, access);

        var response = controller.startFirst(
                "session-1",
                new StartGameRequest(1, List.of(
                        new PlayerSpec("Mallory", false, true),
                        new PlayerSpec("Forged victim", false, true),
                        new PlayerSpec("Attacker bot name", true, true)
                ), Map.of("campaign", "WAR_OF_FIVE_KINGS")),
                authentication
        );

        var requestCaptor = org.mockito.ArgumentCaptor.forClass(StartGameRequest.class);
        verify(service).startFirst(eq("session-1"), requestCaptor.capture());
        assertEquals(List.of("Alice", "Bob", "Bot 1"),
                requestCaptor.getValue().players().stream().map(PlayerSpec::name).toList());
        assertEquals(Map.of("campaign", "WAR_OF_FIVE_KINGS"), requestCaptor.getValue().options());
        assertEquals("P1_ALICE", response.getBody().myPlayerId());
        assertEquals("view-for-P1_ALICE", response.getBody().view());
        verify(access).registerPlayers("game-1", List.of(alice, bob), startedPlayers);
        verify(service, times(2)).viewFor("game-1", "P1_ALICE");
        verify(service).viewFor("game-1", "P2_BOB");
    }
}
