package com.flip.backend.service.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flip.backend.api.dto.LobbyDtos.PlayerSpec;
import com.flip.backend.api.dto.LobbyDtos.StartGameRequest;
import com.flip.backend.conquerwesteros.ConquerWesterosTurnExecutor;
import com.flip.backend.conquerwesteros.engine.ConquerWesterosGameRegistry;
import com.flip.backend.conquerwesteros.engine.ConquerWesterosSnapshotCodec;
import com.flip.backend.persistence.GameEntity;
import com.flip.backend.persistence.GameRepository;
import com.flip.backend.persistence.SessionEntity;
import com.flip.backend.persistence.SessionRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConquerWesterosGameServiceTest {
    @Test
    void exposesMixedTableSingleGameCapabilitiesAndValidatesCampaignAndCounts() {
        Fixture fixture = fixture();
        assertEquals(new GameCapabilities(2, 6, true, false, 1), fixture.service.capabilities());
        assertThrows(IllegalArgumentException.class, () -> fixture.service.startFirst("session", request(1, 1, false, "WAR_OF_FIVE_KINGS")));
        assertThrows(IllegalArgumentException.class, () -> fixture.service.startFirst("session", request(7, 1, false, "WAR_OF_FIVE_KINGS")));
        assertThrows(IllegalArgumentException.class, () -> fixture.service.startFirst("session", request(2, 2, false, "WAR_OF_FIVE_KINGS")));
        assertThrows(IllegalArgumentException.class, () -> fixture.service.startFirst("session", allBots(2, "WAR_OF_FIVE_KINGS")));
        assertThrows(IllegalArgumentException.class, () -> fixture.service.startFirst("session", request(2, 1, false, null)));
        assertThrows(IllegalArgumentException.class, () -> fixture.service.startFirst("session", request(2, 1, false, "ROBERTS_REBELLION")));
        assertThrows(IllegalArgumentException.class, () -> fixture.service.startNext("session", request(2, 1, false, "WAR_OF_FIVE_KINGS")));
    }

    @Test
    void startsAndPersistsEachLegalCampaignWithoutMixingCards() {
        for (String campaign : List.of(
                "WAR_OF_FIVE_KINGS", "DANCE_OF_THE_DRAGONS", "WAR_OF_THE_USURPER", "AEGONS_CONQUEST")) {
            Fixture fixture = fixture();
            var response = fixture.service.startFirst("session", request(2, 1, false, campaign));

            assertEquals(1, response.roundIndex());
            assertEquals("P1", response.myPlayerId());
            assertEquals(List.of("P1", "P2"), response.players().stream().map(player -> player.playerId()).toList());
            var view = (com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.GameView) response.view();
            assertEquals(campaign, view.campaign());
            assertEquals(14, view.strongholds().size());
            GameEntity persisted = fixture.entities.get(response.gameId());
            assertNotNull(persisted.getStateJson());
            assertTrue(persisted.getStateJson().contains("\"schemaVersion\":2"));
            assertTrue(persisted.getStateJson().contains("\"campaign\":\"" + campaign + "\""));
            assertEquals("RUNNING", fixture.session.getState());
        }
    }

    @Test
    void seatsHumansBeforeStableBotIdsAndPersistsTheirIdentity() {
        Fixture fixture = fixture();
        var response = fixture.service.startFirst("session", new StartGameRequest(1, List.of(
                new PlayerSpec("Alice", false, true),
                new PlayerSpec("Requested Bot Name", true, true),
                new PlayerSpec("Bob", false, true),
                new PlayerSpec("Another Bot", true, true)
        ), Map.of("campaign", "WAR_OF_FIVE_KINGS")));

        assertEquals(List.of("P1", "P2", "BOT1", "BOT2"),
                response.players().stream().map(player -> player.playerId()).toList());
        assertEquals(List.of(false, false, true, true),
                response.players().stream().map(player -> player.bot()).toList());
        var view = (com.flip.backend.conquerwesteros.engine.view.ConquerWesterosView.GameView) response.view();
        assertEquals(List.of(false, false, true, true), view.players().stream().map(player -> player.bot()).toList());
        assertTrue(fixture.entities.get(response.gameId()).getStateJson().contains("\"bot\":true"));
    }

    @Test
    void supportsOneHumanWithFiveBotsAtMaximumCapacity() {
        Fixture fixture = fixture();
        var players = new ArrayList<PlayerSpec>();
        players.add(new PlayerSpec("Solo", false, true));
        for (int index = 1; index <= 5; index++) players.add(new PlayerSpec("Bot " + index, true, true));

        var response = fixture.service.startFirst("session", new StartGameRequest(
                1, players, Map.of("campaign", "DANCE_OF_THE_DRAGONS")));

        assertEquals(6, response.players().size());
        assertEquals(List.of("P1", "BOT1", "BOT2", "BOT3", "BOT4", "BOT5"),
                response.players().stream().map(player -> player.playerId()).toList());
    }

    private static StartGameRequest request(int count, int rounds, boolean lastBot, String campaign) {
        var players = new ArrayList<PlayerSpec>();
        for (int index = 1; index <= count; index++) {
            players.add(new PlayerSpec("Player " + index, lastBot && index == count, true));
        }
        Map<String, String> options = campaign == null ? Map.of() : Map.of("campaign", campaign);
        return new StartGameRequest(rounds, players, options);
    }

    private static StartGameRequest allBots(int count, String campaign) {
        var players = new ArrayList<PlayerSpec>();
        for (int index = 1; index <= count; index++) players.add(new PlayerSpec("Bot " + index, true, true));
        return new StartGameRequest(1, players, Map.of("campaign", campaign));
    }

    private static Fixture fixture() {
        var sessions = mock(SessionRepository.class);
        var games = mock(GameRepository.class);
        var registry = mock(ConquerWesterosGameRegistry.class);
        var codec = new ConquerWesterosSnapshotCodec(new ObjectMapper().findAndRegisterModules());
        var session = SessionEntity.builder()
                .id("session")
                .gameType("CONQUERWESTEROS")
                .state("LOBBY")
                .maxPlayers(6)
                .build();
        var entities = new LinkedHashMap<String, GameEntity>();
        when(sessions.findByIdForUpdate("session")).thenReturn(Optional.of(session));
        when(games.findTopBySessionIdOrderByRoundIndexDesc("session")).thenReturn(Optional.empty());
        when(games.save(any(GameEntity.class))).thenAnswer(invocation -> {
            GameEntity entity = invocation.getArgument(0);
            entities.put(entity.getId(), entity);
            return entity;
        });
        when(games.findById(any())).thenAnswer(invocation -> Optional.ofNullable(entities.get(invocation.<String>getArgument(0))));
        var service = new ConquerWesterosGameService(
                sessions, games, registry, codec, mock(ConquerWesterosTurnExecutor.class), ZeroRandom::new);
        return new Fixture(service, session, entities);
    }

    private record Fixture(
            ConquerWesterosGameService service,
            SessionEntity session,
            LinkedHashMap<String, GameEntity> entities
    ) {}

    private static final class ZeroRandom extends Random {
        @Override protected int next(int bits) { return 0; }
    }
}
