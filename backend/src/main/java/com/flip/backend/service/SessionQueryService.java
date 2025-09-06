package com.flip.backend.service;

import com.flip.backend.api.dto.LobbyDtos.*;
import com.flip.backend.persistence.SessionEntity;
import com.flip.backend.persistence.SessionRepository;
import com.flip.backend.persistence.SessionMemberRepository;
import org.springframework.stereotype.Service;

@Service
public class SessionQueryService {
    private final SessionRepository sessions;
    private final SessionMemberRepository members;
    public SessionQueryService(SessionRepository sessions, SessionMemberRepository members) { this.sessions = sessions; this.members = members; }

    public SessionView get(String id) {
        SessionEntity s = sessions.findById(id).orElseThrow(() -> new IllegalArgumentException("session not found"));
        var list = members.findBySessionId(id).stream()
                .map(m -> new LobbyPlayer(m.getUserId(), m.getNickname()))
                .toList();
        return new SessionView(s.getId(), s.getOwnerId(), s.getGameType(), s.getMaxPlayers(), list);
    }
}
