package com.flip.backend.conquerwesteros.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flip.backend.conquerwesteros.engine.phase.ConquerWesterosRuntimePhase;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ConquerWesterosSnapshotCodecTest {
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
