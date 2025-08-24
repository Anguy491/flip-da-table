package com.flip.backend.game.service;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** JPA entity mapping the games table. */
@Entity
@Table(name = "games")
public class GameEngineEntity {
    @Id
    private String id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "round_index", nullable = false)
    private int roundIndex;

    @Column(name = "game_type", nullable = false)
    private String gameType;

    /** Serialized JSON of the full authoritative state. */
    @Column(name = "state", nullable = false, columnDefinition = "TEXT")
    private String state;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected GameEngineEntity() { }

    public GameEngineEntity(String id, String sessionId, int roundIndex, String gameType, String state, OffsetDateTime createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.roundIndex = roundIndex;
        this.gameType = gameType;
        this.state = state;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getSessionId() { return sessionId; }
    public int getRoundIndex() { return roundIndex; }
    public String getGameType() { return gameType; }
    public String getState() { return state; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setState(String state) { this.state = state; }
}