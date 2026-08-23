package com.flip.backend.service;

import com.flip.backend.api.dto.AuthDtos.AuthResponse;
import com.flip.backend.persistence.*;
import com.flip.backend.security.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class GoogleAuthService {
    public static final String LOGIN_PURPOSE = "LOGIN";
    public static final String LINK_PURPOSE = "LINK";
    private static final String GOOGLE_PROVIDER = "GOOGLE";
    private static final Duration LOGIN_HANDOFF_TTL = Duration.ofSeconds(60);
    private static final Duration LINK_HANDOFF_TTL = Duration.ofMinutes(5);
    private static final int MAX_LINK_ATTEMPTS = 5;

    private final GoogleIdentityVerifier verifier;
    private final SecureTokenService secureTokens;
    private final UserRepository users;
    private final UserIdentityRepository identities;
    private final AuthHandoffCodeRepository handoffs;
    private final PasswordEncoder encoder;
    private final AuthService auth;
    private final AuthFeatureProperties properties;
    private final MeterRegistry meters;

    public GoogleAuthService(
            GoogleIdentityVerifier verifier,
            SecureTokenService secureTokens,
            UserRepository users,
            UserIdentityRepository identities,
            AuthHandoffCodeRepository handoffs,
            PasswordEncoder encoder,
            AuthService auth,
            AuthFeatureProperties properties,
            MeterRegistry meters
    ) {
        this.verifier = verifier;
        this.secureTokens = secureTokens;
        this.users = users;
        this.identities = identities;
        this.handoffs = handoffs;
        this.encoder = encoder;
        this.auth = auth;
        this.properties = properties;
        this.meters = meters;
    }

    @Transactional
    public Handoff begin(String credential, String csrfCookie, String csrfBody) {
        requireEnabled();
        if (!secureTokens.constantTimeEquals(csrfCookie, csrfBody)) {
            meters.counter("auth.google.login", "result", "csrf_failed").increment();
            throw new IllegalArgumentException("GOOGLE_CSRF_INVALID");
        }
        VerifiedGoogleIdentity google;
        try {
            google = verifier.verify(credential);
        } catch (RuntimeException ex) {
            meters.counter("auth.google.login", "result", "invalid_credential").increment();
            throw ex;
        }
        var existingIdentity = identities.findByProviderAndSubject(GOOGLE_PROVIDER, google.subject()).orElse(null);
        if (existingIdentity != null) {
            var user = users.findById(existingIdentity.getUserId()).orElseThrow();
            meters.counter("auth.google.login", "result", "returning").increment();
            return newLoginHandoff(user);
        }

        var matchingUser = users.findByEmailIgnoreCase(google.email()).orElse(null);
        if (matchingUser != null) {
            meters.counter("auth.google.login", "result", "link_required").increment();
            return newLinkHandoff(matchingUser, google);
        }

        String nickname = googleNickname(google);
        UserEntity user = users.save(UserEntity.builder()
                .email(google.email())
                .passwordHash(encoder.encode(secureTokens.generate()))
                .nickname(nickname)
                .roles("USER")
                .createdAt(Instant.now())
                .build());
        identities.save(newIdentity(user, google));
        meters.counter("auth.google.login", "result", "created").increment();
        return newLoginHandoff(user);
    }

    @Transactional
    public AuthResponse exchange(String rawCode) {
        requireEnabled();
        var handoff = validHandoff(rawCode, LOGIN_PURPOSE);
        handoff.setConsumedAt(Instant.now());
        var user = users.findById(handoff.getUserId()).orElseThrow(this::invalidHandoff);
        meters.counter("auth.google.exchange", "result", "success").increment();
        return auth.issue(user);
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthResponse link(String rawCode, String password) {
        requireEnabled();
        var handoff = validHandoff(rawCode, LINK_PURPOSE);
        if (handoff.getFailedAttempts() >= MAX_LINK_ATTEMPTS) throw invalidHandoff();
        var user = users.findById(handoff.getUserId()).orElseThrow(this::invalidHandoff);

        boolean passwordMatches;
        try {
            passwordMatches = encoder.matches(password, user.getPasswordHash());
        } catch (RuntimeException ex) {
            passwordMatches = false;
        }
        if (!passwordMatches) {
            handoff.setFailedAttempts(handoff.getFailedAttempts() + 1);
            meters.counter("auth.google.link", "result", "bad_password").increment();
            throw new BadCredentialsException("bad credentials");
        }

        var bySubject = identities.findByProviderAndSubject(GOOGLE_PROVIDER, handoff.getProviderSubject()).orElse(null);
        if (bySubject != null && !bySubject.getUserId().equals(user.getId())) throw invalidHandoff();
        if (bySubject == null) {
            ensureProviderSlotAvailable(user.getId());
            identities.save(UserIdentityEntity.builder()
                    .userId(user.getId())
                    .provider(GOOGLE_PROVIDER)
                    .subject(handoff.getProviderSubject())
                    .emailAtLink(handoff.getProviderEmail())
                    .createdAt(Instant.now())
                    .build());
        }
        handoff.setConsumedAt(Instant.now());
        meters.counter("auth.google.link", "result", "success").increment();
        return auth.issue(user);
    }

    private AuthHandoffCodeEntity validHandoff(String rawCode, String purpose) {
        var handoff = handoffs.findByCodeHashForUpdate(secureTokens.hash(rawCode)).orElseThrow(this::invalidHandoff);
        if (!purpose.equals(handoff.getPurpose())
                || handoff.getConsumedAt() != null
                || !handoff.getExpiresAt().isAfter(Instant.now())) {
            throw invalidHandoff();
        }
        return handoff;
    }

    private Handoff newLoginHandoff(UserEntity user) {
        String rawCode = secureTokens.generate();
        Instant now = Instant.now();
        handoffs.save(AuthHandoffCodeEntity.builder()
                .userId(user.getId())
                .codeHash(secureTokens.hash(rawCode))
                .purpose(LOGIN_PURPOSE)
                .createdAt(now)
                .expiresAt(now.plus(LOGIN_HANDOFF_TTL))
                .build());
        return new Handoff(LOGIN_PURPOSE, rawCode);
    }

    private Handoff newLinkHandoff(UserEntity user, VerifiedGoogleIdentity google) {
        ensureProviderSlotAvailable(user.getId());
        String rawCode = secureTokens.generate();
        Instant now = Instant.now();
        handoffs.save(AuthHandoffCodeEntity.builder()
                .userId(user.getId())
                .codeHash(secureTokens.hash(rawCode))
                .purpose(LINK_PURPOSE)
                .providerSubject(google.subject())
                .providerEmail(google.email())
                .createdAt(now)
                .expiresAt(now.plus(LINK_HANDOFF_TTL))
                .build());
        return new Handoff(LINK_PURPOSE, rawCode);
    }

    private UserIdentityEntity newIdentity(UserEntity user, VerifiedGoogleIdentity google) {
        return UserIdentityEntity.builder()
                .userId(user.getId())
                .provider(GOOGLE_PROVIDER)
                .subject(google.subject())
                .emailAtLink(google.email())
                .createdAt(Instant.now())
                .build();
    }

    private void ensureProviderSlotAvailable(Long userId) {
        if (identities.findByUserIdAndProvider(userId, GOOGLE_PROVIDER).isPresent()) {
            throw new IllegalArgumentException("GOOGLE_ACCOUNT_ALREADY_LINKED");
        }
    }

    private static String googleNickname(VerifiedGoogleIdentity google) {
        String candidate = google.name();
        if (candidate == null || candidate.isBlank()) candidate = google.email().split("@", 2)[0];
        candidate = candidate.trim();
        if (candidate.length() > 32) candidate = candidate.substring(0, 32);
        return candidate.length() >= 2 ? candidate : "Player";
    }

    private IllegalArgumentException invalidHandoff() {
        meters.counter("auth.google.exchange", "result", "invalid").increment();
        return new IllegalArgumentException("GOOGLE_HANDOFF_INVALID_OR_EXPIRED");
    }

    private void requireEnabled() {
        if (!properties.googleEnabled()) throw new IllegalArgumentException("GOOGLE_AUTH_UNAVAILABLE");
    }

    public record Handoff(String purpose, String code) {}
}
