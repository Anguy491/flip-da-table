package com.flip.backend.security;

public interface GoogleIdentityVerifier {
    VerifiedGoogleIdentity verify(String credential);
}
