package com.flip.backend.ws;

import com.flip.backend.persistence.UserRepository;
import com.flip.backend.security.GameAccessService;
import com.flip.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StompSecurityInterceptorTest {
    private final AtomicInteger subscriptionIds = new AtomicInteger();
    @Test
    void rejectsAnonymousSubscriptions() {
        Fixture fixture = fixture();
        assertThrows(AccessDeniedException.class,
                () -> fixture.interceptor.preSend(subscribe("/topic/dvc/game-1/P1_ALICE", null), fixture.channel));
    }

    @Test
    void rejectsAnotherPlayersPrivateDestinationButAllowsTheBoundPlayer() {
        Fixture fixture = fixture();
        when(fixture.access.requirePlayer(fixture.authentication, "game-1")).thenReturn("P1_ALICE");

        assertThrows(AccessDeniedException.class,
                () -> fixture.interceptor.preSend(subscribe("/topic/dvc/game-1/P2_BOB", fixture.authentication), fixture.channel));
        assertDoesNotThrow(
                () -> fixture.interceptor.preSend(subscribe("/topic/dvc/game-1/P1_ALICE", fixture.authentication), fixture.channel));
    }

    @Test
    void allowsMemberLobbyTopicsButRejectsAnotherUsersPrivateLaunchTopic() {
        Fixture fixture = fixture();
        when(fixture.access.requireUserId(fixture.authentication)).thenReturn(7L);

        assertDoesNotThrow(
                () -> fixture.interceptor.preSend(subscribe("/topic/lobby/session-1", fixture.authentication), fixture.channel));
        assertDoesNotThrow(
                () -> fixture.interceptor.preSend(subscribe("/topic/lobby/session-1/7", fixture.authentication), fixture.channel));
        assertThrows(AccessDeniedException.class,
                () -> fixture.interceptor.preSend(subscribe("/topic/lobby/session-1/8", fixture.authentication), fixture.channel));
    }

    @Test
    void capsSubscriptionsAndReleasesCapacityOnUnsubscribe() {
        Fixture fixture = fixture();
        when(fixture.access.requirePlayer(fixture.authentication, "game-1")).thenReturn("P1_ALICE");
        for (int i = 0; i < StompSecurityInterceptor.MAX_SUBSCRIPTIONS_PER_SESSION; i++) {
            assertDoesNotThrow(() -> fixture.interceptor.preSend(
                    subscribe("/topic/dvc/game-1/P1_ALICE", fixture.authentication), fixture.channel));
        }
        assertThrows(com.flip.backend.security.RateLimitExceededException.class,
                () -> fixture.interceptor.preSend(
                        subscribe("/topic/dvc/game-1/P1_ALICE", fixture.authentication), fixture.channel));

        assertDoesNotThrow(() -> fixture.interceptor.preSend(
                unsubscribe("sub-1", fixture.authentication), fixture.channel));
        assertDoesNotThrow(() -> fixture.interceptor.preSend(
                subscribe("/topic/dvc/game-1/P1_ALICE", fixture.authentication), fixture.channel));
    }

    @Test
    void capsConcurrentSessionsPerAuthenticatedUser() {
        Fixture fixture = fixture();
        when(fixture.access.requirePlayer(fixture.authentication, "game-1")).thenReturn("P1_ALICE");
        for (int i = 0; i < StompSecurityInterceptor.MAX_SESSIONS_PER_USER; i++) {
            String sessionId = "session-" + i;
            assertDoesNotThrow(() -> fixture.interceptor.preSend(
                    subscribe("/topic/dvc/game-1/P1_ALICE", fixture.authentication, sessionId), fixture.channel));
        }
        assertThrows(com.flip.backend.security.RateLimitExceededException.class,
                () -> fixture.interceptor.preSend(
                        subscribe("/topic/dvc/game-1/P1_ALICE", fixture.authentication, "session-over-limit"), fixture.channel));
    }

    private Fixture fixture() {
        var access = mock(GameAccessService.class);
        var authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("alice@example.com");
        var interceptor = new StompSecurityInterceptor(
                mock(JwtService.class),
                mock(UserDetailsService.class),
                mock(UserRepository.class),
                access
        );
        return new Fixture(interceptor, access, authentication, mock(MessageChannel.class));
    }

    private Message<byte[]> subscribe(String destination, Authentication authentication) {
        return subscribe(destination, authentication, "session-1");
    }

    private Message<byte[]> subscribe(String destination, Authentication authentication, String sessionId) {
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        headers.setDestination(destination);
        headers.setUser(authentication);
        headers.setSessionId(sessionId);
        headers.setSubscriptionId("sub-" + subscriptionIds.incrementAndGet());
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }

    private Message<byte[]> unsubscribe(String subscriptionId, Authentication authentication) {
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        headers.setUser(authentication);
        headers.setSessionId("session-1");
        headers.setSubscriptionId(subscriptionId);
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }

    private record Fixture(
            StompSecurityInterceptor interceptor,
            GameAccessService access,
            Authentication authentication,
            MessageChannel channel
    ) {}
}
