package com.flip.backend.game.uno;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flip.backend.api.dto.LobbyDtos.PlayerSpec;
import com.flip.backend.api.dto.LobbyDtos.StartGameRequest;
import com.flip.backend.game.engine.GameType;
import com.flip.backend.game.service.GameInitializationResult;
import com.flip.backend.game.service.JacksonGameStateSerializer;
import com.flip.backend.game.service.UnoGameInitializer;
import com.flip.backend.persistence.SessionEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies UNO game initialization & state serialization invariants.
 * Aimed to catch the "Non wild card needs color" issue by asserting every non-wild card has a color after (de)serialization.
 */
public class UnoInitializationTest {

    private ObjectMapper mapper() {
        return new ObjectMapper();
    }

    private UnoGameInitializer newInitializer() {
        return new UnoGameInitializer(new JacksonGameStateSerializer(mapper()));
    }

    private SessionEntity newSession() {
        SessionEntity s = new SessionEntity();
        s.setId("sess-test");
        s.setOwnerId(1L);
        s.setGameType(GameType.UNO.name());
        s.setMaxPlayers(4);
        s.setState("LOBBY");
        s.setCreatedAt(Instant.now());
        return s;
    }

    @Test
    @DisplayName("UNO initialization produces valid state (all non-wild cards colored)")
    void unoInitializationStateValid() {
        var req = new StartGameRequest(1, List.of(
                new PlayerSpec("Host", false, true),
                new PlayerSpec("Bot1", true, true)
        ));
        var init = newInitializer();
        GameInitializationResult result = init.initialize(newSession(), req);
        assertEquals("RUNNING", result.lifecycleState());
        assertFalse(result.players().isEmpty());

        // Deserialize produced JSON
    var serializer = new JacksonGameStateSerializer(mapper());
        UnoState state = serializer.deserialize(result.stateJson(), UnoState.class);
        assertNotNull(state);

        // Collect all cards across piles & hands
        Stream<UnoCard> stream = Stream.concat(
                state.players.stream().flatMap(p -> p.hand().stream()),
                Stream.concat(state.drawPile.stream(), state.discardPile.stream())
        );
        long violations = stream.filter(c -> !c.isWild() && c.color() == null).count();
        assertEquals(0, violations, "Found non-wild cards with null color after initialization");
    }

    @Test
    @DisplayName("Round-trip serialization preserves card color invariants")
    void roundTripSerialization() {
        var req = new StartGameRequest(1, List.of(
                new PlayerSpec("Host", false, true),
                new PlayerSpec("Bot1", true, true)
        ));
        var init = newInitializer();
        GameInitializationResult result = init.initialize(newSession(), req);
    var mapperSerializer = new JacksonGameStateSerializer(mapper());
        UnoState state = mapperSerializer.deserialize(result.stateJson(), UnoState.class);

        String again = mapperSerializer.serialize(state);
        UnoState state2 = mapperSerializer.deserialize(again, UnoState.class);

        long violations = Stream.concat(
                state2.players.stream().flatMap(p -> p.hand().stream()),
                Stream.concat(state2.drawPile.stream(), state2.discardPile.stream())
        ).filter(c -> !c.isWild() && c.color() == null).count();
        assertEquals(0, violations, "Round-trip produced non-wild null-color cards");
    }
}
