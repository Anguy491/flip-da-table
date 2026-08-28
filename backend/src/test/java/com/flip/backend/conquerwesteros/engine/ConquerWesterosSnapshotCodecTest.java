package com.flip.backend.conquerwesteros.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flip.backend.api.dto.LobbyDtos.PlayerStartInfo;
import com.flip.backend.conquerwesteros.engine.phase.ConquerWesterosRuntimePhase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ConquerWesterosSnapshotCodecTest {
    @Test
    void warOfTheUsurperRoundTripsThroughTheCurrentSnapshotSchema() {
        var codec = new ConquerWesterosSnapshotCodec(new ObjectMapper().findAndRegisterModules());
        var runtime = ConquerWesterosRuntimePhase.newGame(List.of(
                new PlayerStartInfo("P1", "Player 1", false, true),
                new PlayerStartInfo("P2", "Player 2", false, true)
        ), Campaign.WAR_OF_THE_USURPER, new Random(1));

        var restored = ConquerWesterosRuntimePhase.restore(codec.decode(codec.encode(runtime)), new Random(1));

        assertEquals(Campaign.WAR_OF_THE_USURPER, restored.campaign());
        assertEquals("War of the Usurper", restored.buildView("P1").campaignName());
        assertEquals("Stoney Sept", restored.buildView("P1").strongholds().get(0).name());
        assertEquals(2, restored.snapshot().schemaVersion());
    }

    @Test
    void aegonsConquestRoundTripsThroughTheCurrentSnapshotSchema() {
        var codec = new ConquerWesterosSnapshotCodec(new ObjectMapper().findAndRegisterModules());
        var runtime = ConquerWesterosRuntimePhase.newGame(List.of(
                new PlayerStartInfo("P1", "Player 1", false, true),
                new PlayerStartInfo("P2", "Player 2", false, true)
        ), Campaign.AEGONS_CONQUEST, new Random(1));

        var restored = ConquerWesterosRuntimePhase.restore(codec.decode(codec.encode(runtime)), new Random(1));

        assertEquals(Campaign.AEGONS_CONQUEST, restored.campaign());
        assertEquals("Aegon's Conquest", restored.buildView("P1").campaignName());
        assertEquals("Maidenpool", restored.buildView("P1").strongholds().get(0).name());
        assertEquals("Aegonfort", restored.buildView("P1").strongholds().get(9).name());
        assertEquals(2, restored.snapshot().schemaVersion());
    }

    @Test
    void readsARealV1PlayerWithoutABotFieldAndRewritesV2() {
        String json = """
                {
                  "schemaVersion": 1,
                  "campaign": "WAR_OF_FIVE_KINGS",
                  "phase": "WAITING_FOR_ROLL",
                  "turnCount": 0,
                  "stateVersion": 0,
                  "eventSequence": 0,
                  "currentPlayerId": "P1",
                  "ironThroneHolderId": null,
                  "centralStrongholds": ["T01","T02","T03","T04","T05","T06","T07","T08","T09","T10","T11","T12","T13","T14"],
                  "players": [
                    {"playerId":"P1","name":"Player 1","faceUpStrongholds":[],"completedClans":{}},
                    {"playerId":"P2","name":"Player 2","faceUpStrongholds":[],"completedClans":{}}
                  ],
                  "attempt": {"targetId":null,"targetOwnerId":null,"stealing":false,"committedLines":{},"lostDieIds":[]},
                  "currentRoll": [],
                  "actionLog": [],
                  "results": []
                }
                """;
        var codec = new ConquerWesterosSnapshotCodec(new ObjectMapper().findAndRegisterModules());

        ConquerWesterosSnapshot decoded = codec.decode(json);
        assertFalse(decoded.players().stream().anyMatch(player -> player.bot()));
        var restored = ConquerWesterosRuntimePhase.restore(decoded, new Random(1));
        assertEquals(2, codec.decode(codec.encode(restored)).schemaVersion());
        assertFalse(restored.buildView("P1").players().stream().anyMatch(player -> player.bot()));
    }
}
