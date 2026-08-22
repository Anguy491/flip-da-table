package com.flip.backend.service;

import com.flip.backend.persistence.PasswordResetTokenEntity;
import com.flip.backend.persistence.PasswordResetTokenRepository;
import com.flip.backend.persistence.UserRepository;
import com.flip.backend.security.AuthFeatureProperties;
import com.flip.backend.security.EmailNormalizer;
import com.flip.backend.security.SecureTokenService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class PasswordResetService {
    private static final Duration RESET_TTL = Duration.ofMinutes(30);
    private static final Duration REQUEST_COOLDOWN = Duration.ofMinutes(1);
    private static final Duration REQUEST_WINDOW = Duration.ofHours(1);
    private static final long MAX_REQUESTS_PER_WINDOW = 5;

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final SecureTokenService secureTokens;
    private final AuthFeatureProperties properties;
    private final ApplicationEventPublisher events;
    private final MeterRegistry meters;

    public PasswordResetService(
            UserRepository users,
            PasswordResetTokenRepository tokens,
            PasswordEncoder encoder,
            SecureTokenService secureTokens,
            AuthFeatureProperties properties,
            ApplicationEventPublisher events,
            MeterRegistry meters
    ) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.secureTokens = secureTokens;
        this.properties = properties;
        this.events = events;
        this.meters = meters;
    }

    @Transactional
    public void requestReset(String emailInput) {
        meters.counter("auth.password_reset.requested").increment();
        if (!properties.passwordResetEnabled()) return;

        // Serialize requests for one account so parallel calls cannot bypass the
        // cooldown or leave more than one usable token.
        var user = users.findByEmailForUpdate(EmailNormalizer.normalize(emailInput)).orElse(null);
        if (user == null) return;

        Instant now = Instant.now();
        var latest = tokens.findTopByUserIdOrderByCreatedAtDesc(user.getId()).orElse(null);
        if (latest != null && latest.getCreatedAt().isAfter(now.minus(REQUEST_COOLDOWN))) return;
        if (tokens.countByUserIdAndCreatedAtAfter(user.getId(), now.minus(REQUEST_WINDOW)) >= MAX_REQUESTS_PER_WINDOW) return;

        for (var existing : tokens.findByUserIdAndUsedAtIsNull(user.getId())) {
            existing.setUsedAt(now);
        }

        String rawToken = secureTokens.generate();
        var token = PasswordResetTokenEntity.builder()
                .userId(user.getId())
                .tokenHash(secureTokens.hash(rawToken))
                .createdAt(now)
                .expiresAt(now.plus(RESET_TTL))
                .build();
        tokens.save(token);
        events.publishEvent(new PasswordResetMailEvent(
                user.getEmail(),
                user.getNickname(),
                rawToken,
                "password-reset/" + token.getTokenHash()
        ));
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (!properties.passwordResetEnabled()) throw invalidToken();
        Instant now = Instant.now();
        var token = tokens.findByTokenHashForUpdate(secureTokens.hash(rawToken)).orElseThrow(this::invalidToken);
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) throw invalidToken();

        var user = users.findById(token.getUserId()).orElseThrow(this::invalidToken);
        user.setPasswordHash(encoder.encode(newPassword));
        user.setAuthVersion(user.getAuthVersion() + 1);
        users.save(user);

        for (var outstanding : tokens.findByUserIdAndUsedAtIsNull(user.getId())) {
            outstanding.setUsedAt(now);
        }
        meters.counter("auth.password_reset.completed", "result", "success").increment();
    }

    private IllegalArgumentException invalidToken() {
        meters.counter("auth.password_reset.completed", "result", "invalid").increment();
        return new IllegalArgumentException("RESET_TOKEN_INVALID_OR_EXPIRED");
    }
}
