package com.flip.backend.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GamePlayerRepository extends JpaRepository<GamePlayerEntity, Long> {
    Optional<GamePlayerEntity> findByGameIdAndUserId(String gameId, Long userId);
    List<GamePlayerEntity> findByGameIdOrderBySeatIndexAsc(String gameId);
}
