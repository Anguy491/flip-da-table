package com.flip.backend.security;

public record VerifiedGoogleIdentity(
        String subject,
        String email,
        boolean emailVerified,
        String hostedDomain,
        String name
) {}
