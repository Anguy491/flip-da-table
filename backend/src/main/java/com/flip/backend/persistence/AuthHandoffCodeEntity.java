package com.flip.backend.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity @Table(name = "auth_handoff_codes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthHandoffCodeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "user_id")
    private Long userId;

    @Column(nullable = false, unique = true, name = "code_hash", length = 64)
    private String codeHash;

    @Column(nullable = false, length = 16)
    private String purpose;

    @Column(name = "provider_subject", length = 255)
    private String providerSubject;

    @Column(name = "provider_email")
    private String providerEmail;

    @Column(nullable = false, name = "failed_attempts")
    @Builder.Default
    private int failedAttempts = 0;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;

    @Column(nullable = false, name = "expires_at")
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;
}
