package com.flip.backend.lasvegas.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flip.backend.lasvegas.engine.phase.LasVegasRuntimePhase;
import org.springframework.stereotype.Component;

@Component
public class LasVegasSnapshotCodec {
    private final ObjectMapper objectMapper;

    public LasVegasSnapshotCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encode(LasVegasRuntimePhase runtime) {
        try {
            return objectMapper.writeValueAsString(runtime.snapshot());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize Las Vegas snapshot", exception);
        }
    }

    public LasVegasSnapshot decode(String json) {
        if (json == null || json.isBlank()) throw new IllegalArgumentException("Las Vegas snapshot is missing");
        try {
            return objectMapper.readValue(json, LasVegasSnapshot.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to deserialize Las Vegas snapshot", exception);
        }
    }
}
