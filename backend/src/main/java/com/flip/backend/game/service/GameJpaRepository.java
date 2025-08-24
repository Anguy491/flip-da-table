package com.flip.backend.game.service;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameJpaRepository extends JpaRepository<GameEngineEntity, String> {
}
