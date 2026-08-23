package com.flip.backend.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SessionMemberRepository extends JpaRepository<SessionMemberEntity, Long> {
    List<SessionMemberEntity> findBySessionId(String sessionId);
    List<SessionMemberEntity> findBySessionIdOrderByJoinedAtAscIdAsc(String sessionId);
    Optional<SessionMemberEntity> findBySessionIdAndUserId(String sessionId, Long userId);
    long countBySessionId(String sessionId);
}
