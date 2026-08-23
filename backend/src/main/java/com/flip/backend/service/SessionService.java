package com.flip.backend.service;

import com.flip.backend.api.dto.SessionDtos.*;
import com.flip.backend.api.dto.LobbyDtos.*;
import com.flip.backend.persistence.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class SessionService {
    private final SessionRepository sessions;
    private final UserRepository users;
    private final SessionMemberRepository members;
    private final SimpMessagingTemplate messaging;

    public SessionService(SessionRepository sessions, UserRepository users, SessionMemberRepository members, SimpMessagingTemplate messaging) {
        this.sessions = sessions;
        this.users = users;
        this.members = members;
        this.messaging = messaging;
    }

    public CreateSessionResponse create(CreateSessionRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        var user = users.findByEmail(auth.getName()).orElseThrow();

        String id = UUID.randomUUID().toString();
        var entity = SessionEntity.builder()
                .id(id)
                .ownerId(user.getId())
                .gameType(req.gameType())
                .maxPlayers(req.maxPlayers())
                .state("LOBBY")
                .createdAt(Instant.now())
                .build();
        sessions.save(entity);
        return new CreateSessionResponse(id);
    }

    @Transactional
    public SessionView join(String sessionId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        var user = users.findByEmailIgnoreCase(auth.getName()).orElseThrow();
        var session = sessions.findByIdForUpdate(sessionId).orElseThrow(() -> new IllegalArgumentException("session not found"));
        var existing = members.findBySessionIdAndUserId(sessionId, user.getId());
        if (existing.isEmpty()) {
            if (!"LOBBY".equals(session.getState())) {
                throw new IllegalArgumentException("session is no longer joinable");
            }
            if (members.countBySessionId(sessionId) >= session.getMaxPlayers()) {
                throw new IllegalArgumentException("session is full");
            }
            var m = SessionMemberEntity.builder()
                    .sessionId(sessionId)
                    .userId(user.getId())
                    .nickname(user.getNickname())
                    .joinedAt(Instant.now())
                    .build();
            members.save(m);
        }
        var list = members.findBySessionId(sessionId).stream()
                .map(member -> new LobbyPlayer(member.getUserId(), member.getNickname()))
                .toList();
        var view = new SessionView(session.getId(), session.getOwnerId(), session.getGameType(), session.getMaxPlayers(), list);
        messaging.convertAndSend("/topic/lobby/" + sessionId, view);
        return view;
    }
}
