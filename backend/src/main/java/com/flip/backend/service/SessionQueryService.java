package com.flip.backend.service;

import com.flip.backend.api.dto.LobbyDtos.SessionView;
import com.flip.backend.persistence.SessionEntity;
import com.flip.backend.persistence.SessionRepository;
import org.springframework.stereotype.Service;

@Service
public class SessionQueryService {
    private final SessionRepository repo;
    public SessionQueryService(SessionRepository repo) { this.repo = repo; }

    public SessionView get(String id) {
        SessionEntity s = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("session not found"));
        return new SessionView(s.getId(), s.getOwnerId(), s.getGameType(), s.getMaxPlayers());
    }
}
