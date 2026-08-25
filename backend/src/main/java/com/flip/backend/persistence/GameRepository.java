package com.flip.backend.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<GameEntity, String> {
	@Query("select max(g.roundIndex) from GameEntity g where g.sessionId = :sid")
	Integer findMaxRoundIndexBySessionId(@Param("sid") String sessionId);

    Optional<GameEntity> findTopBySessionIdOrderByRoundIndexDesc(String sessionId);

    Optional<GameEntity> findTopBySessionIdAndStateInOrderByRoundIndexDesc(String sessionId, Collection<String> states);

    List<GameEntity> findByGameTypeIgnoreCaseAndState(String gameType, String state);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GameEntity g where g.id = :id")
    Optional<GameEntity> findByIdForUpdate(@Param("id") String id);
}
