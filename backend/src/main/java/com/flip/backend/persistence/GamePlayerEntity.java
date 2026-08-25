package com.flip.backend.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "game_players", uniqueConstraints = {
        @UniqueConstraint(name = "uk_game_players_game_user", columnNames = {"game_id", "user_id"}),
        @UniqueConstraint(name = "uk_game_players_game_player", columnNames = {"game_id", "player_id"}),
        @UniqueConstraint(name = "uk_game_players_game_seat", columnNames = {"game_id", "seat_index"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GamePlayerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false)
    private String gameId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "player_id", nullable = false)
    private String playerId;

    @Column(name = "seat_index", nullable = false)
    private Integer seatIndex;
}
