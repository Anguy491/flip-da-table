package com.flip.backend.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRepository extends JpaRepository<GameEntity, String> {
	@Query("select max(g.roundIndex) from GameEntity g where g.sessionId = :sid")
	Integer findMaxRoundIndexBySessionId(@Param("sid") String sessionId);
}
