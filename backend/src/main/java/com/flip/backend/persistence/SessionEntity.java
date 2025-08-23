package com.flip.backend.persistence;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SessionEntity {
    @Id
    @Column(name="id", nullable=false, updatable=false, length=36)
    private String id; // UUID

    @Column(nullable=false)
    private Long ownerId;

    @Column(nullable=false, length=64)
    private String gameType; // "DAVINCI" | "UNO" | "BOUNTY"

    @Column(nullable=false)
    private Integer maxPlayers;

    @Column(nullable=false, length=32)
    private String state; // "LOBBY"

    @Column(nullable=false, name="created_at")
    private Instant createdAt;
}
