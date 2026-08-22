package com.flip.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthFeatureProperties {
    private final String publicUrl;
    private final String supportEmail;
    private final String mailFrom;
    private final boolean passwordResetEnabled;
    private final boolean googleEnabled;
    private final String googleClientId;

    public AuthFeatureProperties(
            @Value("${app.public-url:http://localhost:5173}") String publicUrl,
            @Value("${app.support-email:support@anguy.dev}") String supportEmail,
            @Value("${app.mail.from:Flip Da Table <no-reply@mail.anguy.dev>}") String mailFrom,
            @Value("${app.auth.password-reset.enabled:false}") boolean passwordResetEnabled,
            @Value("${app.auth.google.enabled:false}") boolean googleEnabled,
            @Value("${app.auth.google.client-id:}") String googleClientId
    ) {
        this.publicUrl = stripTrailingSlash(publicUrl);
        this.supportEmail = supportEmail;
        this.mailFrom = mailFrom;
        this.passwordResetEnabled = passwordResetEnabled;
        this.googleEnabled = googleEnabled;
        this.googleClientId = googleClientId == null ? "" : googleClientId.trim();
    }

    private static String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "http://localhost:5173";
        String result = value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    public String publicUrl() { return publicUrl; }
    public String supportEmail() { return supportEmail; }
    public String mailFrom() { return mailFrom; }
    public boolean passwordResetEnabled() { return passwordResetEnabled; }
    public boolean googleEnabled() { return googleEnabled && !googleClientId.isBlank(); }
    public String googleClientId() { return googleClientId; }
    public String googleLoginUri() { return publicUrl + "/api/auth/google/callback"; }
}
