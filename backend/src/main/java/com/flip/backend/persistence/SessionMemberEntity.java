package com.flip.backend.persistence;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "session_members",
    uniqueConstraints = @UniqueConstraint(name="uk_session_user", columnNames = {"session_id","user_id"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SessionMemberEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="session_id", nullable=false, length=36)
    private String sessionId;

    @Column(name="user_id", nullable=false)
    private Long userId;

    @Column(nullable=false)
    private String nickname;

    @Column(name="joined_at", nullable=false)
    private Instant joinedAt;
}
