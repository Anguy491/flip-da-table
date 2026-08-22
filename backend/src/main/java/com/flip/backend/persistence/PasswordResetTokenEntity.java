package com.flip.backend.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity @Table(name = "password_reset_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PasswordResetTokenEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "user_id")
    private Long userId;

    @Column(nullable = false, unique = true, name = "token_hash", length = 64)
    private String tokenHash;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;

    @Column(nullable = false, name = "expires_at")
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;
}
