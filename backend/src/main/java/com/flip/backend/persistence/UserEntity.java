package com.flip.backend.persistence;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true)
    private String email;

    @Column(nullable=false, name="password_hash")
    private String passwordHash;

    @Column(nullable=false)
    private String nickname;

    @Column(nullable=false)
    private String roles;

    @Column(nullable=false, name="created_at")
    private Instant createdAt;
}