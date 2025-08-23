package com.flip.backend.service;

import com.flip.backend.api.dto.SessionDtos.*;
import com.flip.backend.persistence.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class SessionService {
    private final SessionRepository sessions;
    private final UserRepository users;

    public SessionService(SessionRepository sessions, UserRepository users) {
        this.sessions = sessions;
        this.users = users;
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
}
