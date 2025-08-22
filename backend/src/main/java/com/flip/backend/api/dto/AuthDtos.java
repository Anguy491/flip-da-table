package com.flip.backend.api.dto;

import jakarta.validation.constraints.*;

public class AuthDtos {
    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min=6,max=64) String password,
            @NotBlank @Size(min=2,max=32) String nickname
    ) {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record AuthResponse(
            Long userId, String email, String nickname, String token
    ) {}
}
