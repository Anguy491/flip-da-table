package com.flip.backend.persistence;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="games")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GameEntity {
    @Id
    @Column(nullable=false, updatable=false)
    private String id;

    @Column(name="session_id", nullable=false)
    private String sessionId;

    @Column(name="round_index", nullable=false)
    private Integer roundIndex;

    @Column(name="game_type", nullable=false, length=64)
    private String gameType;

    @Column(nullable=false, length=32)
    private String state; // lifecycle: CREATED | RUNNING | ENDED

    // Serialized full state JSON (UNO / other engines). Null until RUNNING.
    @Column(name="state_json")
    private String stateJson;

    @Column(name="created_at", nullable=false)
    private Instant createdAt;
}
