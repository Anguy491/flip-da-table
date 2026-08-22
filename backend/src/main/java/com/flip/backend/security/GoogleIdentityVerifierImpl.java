package com.flip.backend.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GoogleIdentityVerifierImpl implements GoogleIdentityVerifier {
    private final AuthFeatureProperties properties;
    private final GoogleIdTokenVerifier verifier;

    public GoogleIdentityVerifierImpl(AuthFeatureProperties properties) throws Exception {
        this.properties = properties;
        this.verifier = properties.googleClientId().isBlank() ? null : new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance()
        ).setAudience(List.of(properties.googleClientId())).build();
    }

    @Override
    public VerifiedGoogleIdentity verify(String credential) {
        if (!properties.googleEnabled() || verifier == null || credential == null || credential.isBlank()) {
            throw new IllegalArgumentException("GOOGLE_AUTH_UNAVAILABLE");
        }
        try {
            var token = verifier.verify(credential);
            if (token == null) throw new IllegalArgumentException("GOOGLE_ID_TOKEN_INVALID");
            var payload = token.getPayload();
            boolean verified = Boolean.TRUE.equals(payload.getEmailVerified());
            if (!verified || payload.getSubject() == null || payload.getEmail() == null) {
                throw new IllegalArgumentException("GOOGLE_EMAIL_NOT_VERIFIED");
            }
            return new VerifiedGoogleIdentity(
                    payload.getSubject(),
                    EmailNormalizer.normalize(payload.getEmail()),
                    true,
                    payload.getHostedDomain(),
                    (String) payload.get("name")
            );
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("GOOGLE_ID_TOKEN_INVALID");
        }
    }
}
