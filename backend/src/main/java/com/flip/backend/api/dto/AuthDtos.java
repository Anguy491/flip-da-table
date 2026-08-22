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
            @NotBlank @Size(max=64) String password
    ) {}

    public record AuthResponse(
            Long userId, String email, String nickname, String token
    ) {}

    public record ForgotPasswordRequest(
            @NotBlank @Size(max=254) String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min=6,max=64) String newPassword
    ) {}

    public record AuthCodeRequest(
            @NotBlank String code
    ) {}

    public record GoogleLinkRequest(
            @NotBlank String code,
            @NotBlank @Size(max=64) String password
    ) {}

    public record GoogleCapability(
            boolean enabled,
            String clientId,
            String loginUri
    ) {}

    public record CapabilitiesResponse(
            boolean passwordReset,
            String supportEmail,
            GoogleCapability google
    ) {}
}
