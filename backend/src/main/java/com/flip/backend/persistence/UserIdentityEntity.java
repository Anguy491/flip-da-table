package com.flip.backend.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_identities", uniqueConstraints = {
        @UniqueConstraint(name = "uk_identity_provider_subject", columnNames = {"provider", "subject"}),
        @UniqueConstraint(name = "uk_identity_user_provider", columnNames = {"user_id", "provider"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserIdentityEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, name = "email_at_link")
    private String emailAtLink;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;
}
