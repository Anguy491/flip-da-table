package com.flip.backend.game.service;

/** Abstraction to (de)serialize a game state. */
public interface GameStateSerializer {
    <S> String serialize(S state);
    <S> S deserialize(String json, Class<S> clazz);
}
