package com.flip.backend.service;

public record PasswordResetMailEvent(
        String email,
        String nickname,
        String token,
        String idempotencyKey
) {}
