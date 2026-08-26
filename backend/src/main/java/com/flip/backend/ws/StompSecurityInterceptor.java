package com.flip.backend.ws;

import com.flip.backend.persistence.UserRepository;
import com.flip.backend.security.EmailNormalizer;
import com.flip.backend.security.GameAccessService;
import com.flip.backend.security.JwtService;
import com.flip.backend.security.RateLimitExceededException;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

@Component
public class StompSecurityInterceptor implements ChannelInterceptor {
    static final int MAX_SESSIONS_PER_USER = 4;
    static final int MAX_SUBSCRIPTIONS_PER_SESSION = 16;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository users;
    private final GameAccessService access;
    private final Map<String, String> sessionUsers = new HashMap<>();
    private final Map<String, Set<String>> userSessions = new HashMap<>();
    private final Map<String, Set<String>> sessionSubscriptions = new HashMap<>();

    public StompSecurityInterceptor(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            UserRepository users,
            GameAccessService access
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.users = users;
        this.access = access;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor headers = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (headers == null || headers.getCommand() == null) return message;

        if (StompCommand.CONNECT.equals(headers.getCommand())) {
            Authentication authentication = authenticate(headers);
            headers.setUser(authentication);
            registerConnection(authentication, headers.getSessionId());
            return message;
        }

        if (StompCommand.DISCONNECT.equals(headers.getCommand())) {
            cleanup(headers.getSessionId());
        } else if (StompCommand.UNSUBSCRIBE.equals(headers.getCommand())) {
            requireAuthentication(headers);
            unregisterSubscription(headers.getSessionId(), headers.getSubscriptionId());
        } else if (StompCommand.SUBSCRIBE.equals(headers.getCommand())) {
            Authentication authentication = requireAuthentication(headers);
            authorizeSubscription(authentication, headers.getDestination());
            registerConnection(authentication, headers.getSessionId());
            registerSubscription(headers.getSessionId(), headers.getSubscriptionId());
        } else if (StompCommand.SEND.equals(headers.getCommand())) {
            requireAuthentication(headers);
            throw new AccessDeniedException("client messages are not supported");
        }
        return message;
    }

    private synchronized void registerConnection(Authentication authentication, String sessionId) {
        if (!StringUtils.hasText(sessionId)) throw new AccessDeniedException("missing session id");
        String username = authentication.getName();
        if (!StringUtils.hasText(username)) throw new AccessDeniedException("missing user identity");
        String existing = sessionUsers.get(sessionId);
        if (username.equals(existing)) return;
        if (existing != null) cleanup(sessionId);
        Set<String> sessions = userSessions.computeIfAbsent(username, ignored -> new HashSet<>());
        if (sessions.size() >= MAX_SESSIONS_PER_USER) {
            throw new RateLimitExceededException("too many websocket sessions", 60);
        }
        sessions.add(sessionId);
        sessionUsers.put(sessionId, username);
    }

    private synchronized void registerSubscription(String sessionId, String subscriptionId) {
        if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(subscriptionId)) {
            throw new AccessDeniedException("missing subscription identity");
        }
        Set<String> subscriptions = sessionSubscriptions.computeIfAbsent(sessionId, ignored -> new HashSet<>());
        if (subscriptions.contains(subscriptionId)) return;
        if (subscriptions.size() >= MAX_SUBSCRIPTIONS_PER_SESSION) {
            throw new RateLimitExceededException("too many websocket subscriptions", 60);
        }
        subscriptions.add(subscriptionId);
    }

    private synchronized void unregisterSubscription(String sessionId, String subscriptionId) {
        Set<String> subscriptions = sessionSubscriptions.get(sessionId);
        if (subscriptions == null) return;
        subscriptions.remove(subscriptionId);
        if (subscriptions.isEmpty()) sessionSubscriptions.remove(sessionId);
    }

    private synchronized void cleanup(String sessionId) {
        if (!StringUtils.hasText(sessionId)) return;
        sessionSubscriptions.remove(sessionId);
        String username = sessionUsers.remove(sessionId);
        if (username == null) return;
        Set<String> sessions = userSessions.get(username);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) userSessions.remove(username);
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        cleanup(event.getSessionId());
    }

    private Authentication authenticate(StompHeaderAccessor headers) {
        String authorization = headers.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization)) {
            authorization = headers.getFirstNativeHeader(HttpHeaders.AUTHORIZATION.toLowerCase());
        }
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new BadCredentialsException("missing bearer token");
        }
        try {
            Claims claims = jwtService.parse(authorization.substring(7)).getBody();
            String email = EmailNormalizer.normalize(claims.getSubject());
            var user = users.findByEmailIgnoreCase(email).orElseThrow();
            Object rawVersion = claims.get("ver");
            int tokenVersion = rawVersion instanceof Number number ? number.intValue() : 0;
            if (tokenVersion != user.getAuthVersion()) throw new IllegalArgumentException("stale token");
            var details = userDetailsService.loadUserByUsername(email);
            return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        } catch (RuntimeException ex) {
            throw new BadCredentialsException("invalid bearer token");
        }
    }

    private Authentication requireAuthentication(StompHeaderAccessor headers) {
        if (!(headers.getUser() instanceof Authentication authentication) || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("authentication required");
        }
        return authentication;
    }

    private void authorizeSubscription(Authentication authentication, String destination) {
        List<String> segments = destination == null
                ? List.of()
                : Arrays.stream(destination.split("/")).filter(segment -> !segment.isBlank()).toList();
        if (segments.size() >= 3 && "topic".equals(segments.get(0)) && "lobby".equals(segments.get(1))) {
            String sessionId = segments.get(2);
            access.requireSessionMember(authentication, sessionId);
            if (segments.size() == 3) return;
            if (segments.size() == 4) {
                try {
                    if (Long.parseLong(segments.get(3)) == access.requireUserId(authentication)) return;
                } catch (NumberFormatException ignored) {
                    // Denied below.
                }
            }
            throw new AccessDeniedException("forbidden destination");
        }
        if (segments.size() == 4 && "topic".equals(segments.get(0)) && "dvc".equals(segments.get(1))) {
            String gameId = segments.get(2);
            String playerId = access.requirePlayer(authentication, gameId);
            String destinationPlayer = segments.get(3);
            if ("public-reveals".equals(destinationPlayer) || playerId.equals(destinationPlayer)) return;
        }
        if (segments.size() == 4 && "topic".equals(segments.get(0)) && "las-vegas".equals(segments.get(1))) {
            String gameId = segments.get(2);
            String playerId = access.requirePlayer(authentication, gameId);
            String destinationPlayer = segments.get(3);
            if ("events".equals(destinationPlayer) || playerId.equals(destinationPlayer)) return;
        }
        if (segments.size() == 4 && "topic".equals(segments.get(0)) && "conquer-westeros".equals(segments.get(1))) {
            String gameId = segments.get(2);
            String playerId = access.requirePlayer(authentication, gameId);
            String destinationPlayer = segments.get(3);
            if ("events".equals(destinationPlayer) || playerId.equals(destinationPlayer)) return;
        }
        throw new AccessDeniedException("forbidden destination");
    }
}
