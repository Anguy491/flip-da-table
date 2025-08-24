package com.flip.backend.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/** Default JSON serializer using the global ObjectMapper. */
@Component
public class JacksonGameStateSerializer implements GameStateSerializer {
    private final ObjectMapper objectMapper;

    public JacksonGameStateSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <S> String serialize(S state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (Exception e) {
            throw new IllegalStateException("Serialize state failed", e);
        }
    }

    @Override
    public <S> S deserialize(String json, Class<S> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new IllegalStateException("Deserialize state failed", e);
        }
    }
}
