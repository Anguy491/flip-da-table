package com.flip.backend.conquerwesteros.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flip.backend.conquerwesteros.engine.phase.ConquerWesterosRuntimePhase;
import org.springframework.stereotype.Component;

@Component
public class ConquerWesterosSnapshotCodec {
    private final ObjectMapper objectMapper;

    public ConquerWesterosSnapshotCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encode(ConquerWesterosRuntimePhase runtime) {
        try {
            return objectMapper.writeValueAsString(runtime.snapshot());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize Conquer Westeros snapshot", exception);
        }
    }

    public ConquerWesterosSnapshot decode(String json) {
        if (json == null || json.isBlank()) throw new IllegalArgumentException("Conquer Westeros snapshot is missing");
        try {
            return objectMapper.readValue(json, ConquerWesterosSnapshot.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to deserialize Conquer Westeros snapshot", exception);
        }
    }
}
