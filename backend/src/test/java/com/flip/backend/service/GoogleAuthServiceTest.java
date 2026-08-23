package com.flip.backend.service;

import com.flip.backend.api.dto.AuthDtos.AuthResponse;
import com.flip.backend.persistence.*;
import com.flip.backend.security.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleAuthServiceTest {
    @Test
    void featureRollbackAlsoDisablesExistingHandoffExchange() {
        Fixture fixture = fixture(new VerifiedGoogleIdentity("google-sub", "player@gmail.com", true, null, "Player"));
        when(fixture.properties.googleEnabled()).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> fixture.service.exchange("handoff"));

        verifyNoInteractions(fixture.handoffs);
    }

    @Test
    void rejectsCsrfMismatchBeforeVerifyingTheGoogleCredential() {
        Fixture fixture = fixture(new VerifiedGoogleIdentity("google-sub", "player@gmail.com", true, null, "Player"));

        assertThrows(IllegalArgumentException.class,
                () -> fixture.service.begin("credential", "cookie-token", "form-token"));

        verify(fixture.verifier, never()).verify(any());
        verifyNoInteractions(fixture.users, fixture.identities, fixture.handoffs);
    }

    @Test
    void rejectsCredentialsThatTheGoogleVerifierFlagsForAudienceOrIssuer() {
        Fixture fixture = fixture(new VerifiedGoogleIdentity("google-sub", "player@gmail.com", true, null, "Player"));
        when(fixture.verifier.verify("credential")).thenThrow(new IllegalArgumentException("GOOGLE_ID_TOKEN_INVALID"));

        assertThrows(IllegalArgumentException.class,
                () -> fixture.service.begin("credential", "csrf", "csrf"));

        verifyNoInteractions(fixture.users, fixture.identities, fixture.handoffs);
    }

    @Test
    void signsInByStableSubjectWithoutUsingTheCurrentGoogleEmailAsIdentity() {
        Fixture fixture = fixture(new VerifiedGoogleIdentity("known-sub", "new-address@example.com", true, null, "Player"));
        var player = fixture.player("old-address@example.com");
        when(fixture.identities.findByProviderAndSubject("GOOGLE", "known-sub")).thenReturn(Optional.of(
                UserIdentityEntity.builder().userId(player.getId()).provider("GOOGLE").subject("known-sub").build()));
        when(fixture.users.findById(player.getId())).thenReturn(Optional.of(player));

        var result = fixture.service.begin("credential", "csrf", "csrf");

        assertEquals(GoogleAuthService.LOGIN_PURPOSE, result.purpose());
        verify(fixture.users, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    void requiresTheOriginalPasswordForAGmailAddressCollision() {
        Fixture fixture = fixture(new VerifiedGoogleIdentity("google-sub", "player@gmail.com", true, null, "Google Player"));
        var player = fixture.player("player@gmail.com");
        when(fixture.identities.findByProviderAndSubject("GOOGLE", "google-sub")).thenReturn(Optional.empty());
        when(fixture.identities.findByUserIdAndProvider(player.getId(), "GOOGLE")).thenReturn(Optional.empty());
        when(fixture.users.findByEmailIgnoreCase("player@gmail.com")).thenReturn(Optional.of(player));

        var result = fixture.service.begin("credential", "csrf", "csrf");

        assertEquals(GoogleAuthService.LINK_PURPOSE, result.purpose());
        verify(fixture.identities, never()).save(any());
        verify(fixture.handoffs).save(argThat(code -> code.getProviderSubject().equals("google-sub")
                && code.getPurpose().equals(GoogleAuthService.LINK_PURPOSE)));
    }

    @Test
    void requiresTheOriginalPasswordForAThirdPartyGoogleEmailCollision() {
        Fixture fixture = fixture(new VerifiedGoogleIdentity("google-sub", "player@example.com", true, null, "Google Player"));
        var player = fixture.player("player@example.com");
        when(fixture.identities.findByProviderAndSubject("GOOGLE", "google-sub")).thenReturn(Optional.empty());
        when(fixture.identities.findByUserIdAndProvider(player.getId(), "GOOGLE")).thenReturn(Optional.empty());
        when(fixture.users.findByEmailIgnoreCase("player@example.com")).thenReturn(Optional.of(player));

        var result = fixture.service.begin("credential", "csrf", "csrf");

        assertEquals(GoogleAuthService.LINK_PURPOSE, result.purpose());
        verify(fixture.identities, never()).save(any());
        verify(fixture.handoffs).save(argThat(code -> code.getProviderSubject().equals("google-sub")
                && code.getPurpose().equals(GoogleAuthService.LINK_PURPOSE)));
    }

    @Test
    void requiresTheOriginalPasswordForAVerifiedWorkspaceAddressCollision() {
        Fixture fixture = fixture(new VerifiedGoogleIdentity(
                "workspace-sub", "player@company.example", true, "company.example", "Workspace Player"));
        var player = fixture.player("player@company.example");
        when(fixture.identities.findByProviderAndSubject("GOOGLE", "workspace-sub")).thenReturn(Optional.empty());
        when(fixture.identities.findByUserIdAndProvider(player.getId(), "GOOGLE")).thenReturn(Optional.empty());
        when(fixture.users.findByEmailIgnoreCase(player.getEmail())).thenReturn(Optional.of(player));

        var result = fixture.service.begin("credential", "csrf", "csrf");

        assertEquals(GoogleAuthService.LINK_PURPOSE, result.purpose());
        verify(fixture.identities, never()).save(any());
        verify(fixture.handoffs).save(argThat(code -> code.getProviderSubject().equals("workspace-sub")
                && code.getPurpose().equals(GoogleAuthService.LINK_PURPOSE)));
    }

    @Test
    void createsANewPlayerAndTruncatesTheGoogleNickname() {
        Fixture fixture = fixture(new VerifiedGoogleIdentity(
                "new-sub", "new-player@example.com", true, null, "A nickname longer than thirty two visible characters"));
        when(fixture.identities.findByProviderAndSubject("GOOGLE", "new-sub")).thenReturn(Optional.empty());
        when(fixture.users.findByEmailIgnoreCase("new-player@example.com")).thenReturn(Optional.empty());

        var result = fixture.service.begin("credential", "csrf", "csrf");

        assertEquals(GoogleAuthService.LOGIN_PURPOSE, result.purpose());
        verify(fixture.users).save(argThat(user -> user.getEmail().equals("new-player@example.com")
                && user.getNickname().length() == 32
                && user.getPasswordHash().startsWith("$2")));
        verify(fixture.identities).save(argThat(identity -> identity.getSubject().equals("new-sub")
                && identity.getUserId().equals(42L)));
    }

    @Test
    void recordsFailedLinkPasswordAttemptsWithoutBindingTheIdentity() {
        Fixture fixture = fixture(new VerifiedGoogleIdentity("google-sub", "player@example.com", true, null, "Google Player"));
        var player = fixture.player("player@example.com");
        String rawCode = fixture.secureTokens.generate();
        var handoff = AuthHandoffCodeEntity.builder()
                .userId(player.getId())
                .codeHash(fixture.secureTokens.hash(rawCode))
                .purpose(GoogleAuthService.LINK_PURPOSE)
                .providerSubject("google-sub")
                .providerEmail("player@example.com")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        when(fixture.handoffs.findByCodeHashForUpdate(handoff.getCodeHash())).thenReturn(Optional.of(handoff));
        when(fixture.users.findById(player.getId())).thenReturn(Optional.of(player));

        assertThrows(Exception.class, () -> fixture.service.link(rawCode, "wrong-password"));

        assertEquals(1, handoff.getFailedAttempts());
        verify(fixture.identities, never()).save(any());
    }

    @Test
    void linksAThirdPartyGoogleIdentityAfterTheOriginalPasswordMatches() {
        Fixture fixture = fixture(new VerifiedGoogleIdentity("google-sub", "player@example.com", true, null, "Player"));
        var player = fixture.player("player@example.com");
        String rawCode = fixture.secureTokens.generate();
        var handoff = fixture.linkHandoff(player, rawCode, 0);
        when(fixture.handoffs.findByCodeHashForUpdate(handoff.getCodeHash())).thenReturn(Optional.of(handoff));
        when(fixture.users.findById(player.getId())).thenReturn(Optional.of(player));
        when(fixture.identities.findByProviderAndSubject("GOOGLE", "google-sub")).thenReturn(Optional.empty());
        when(fixture.identities.findByUserIdAndProvider(player.getId(), "GOOGLE")).thenReturn(Optional.empty());
        var response = new AuthResponse(player.getId(), player.getEmail(), player.getNickname(), "jwt");
        when(fixture.auth.issue(player)).thenReturn(response);

        assertSame(response, fixture.service.link(rawCode, "correct-password"));
        assertNotNull(handoff.getConsumedAt());
        verify(fixture.identities).save(argThat(identity -> identity.getUserId().equals(player.getId())
                && identity.getSubject().equals("google-sub")));
    }

    @Test
    void refusesFurtherLinkChecksAfterFiveFailedPasswords() {
        Fixture fixture = fixture(new VerifiedGoogleIdentity("google-sub", "player@example.com", true, null, "Player"));
        var player = fixture.player("player@example.com");
        String rawCode = fixture.secureTokens.generate();
        var handoff = fixture.linkHandoff(player, rawCode, 5);
        when(fixture.handoffs.findByCodeHashForUpdate(handoff.getCodeHash())).thenReturn(Optional.of(handoff));

        assertThrows(IllegalArgumentException.class, () -> fixture.service.link(rawCode, "correct-password"));
        verify(fixture.users, never()).findById(anyLong());
        verify(fixture.identities, never()).save(any());
    }

    @Test
    void consumesLoginHandoffOnlyOnce() {
        Fixture fixture = fixture(new VerifiedGoogleIdentity("google-sub", "player@example.com", true, null, "Player"));
        var player = fixture.player("player@example.com");
        String rawCode = fixture.secureTokens.generate();
        var handoff = AuthHandoffCodeEntity.builder()
                .userId(player.getId())
                .codeHash(fixture.secureTokens.hash(rawCode))
                .purpose(GoogleAuthService.LOGIN_PURPOSE)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        when(fixture.handoffs.findByCodeHashForUpdate(handoff.getCodeHash())).thenReturn(Optional.of(handoff));
        when(fixture.users.findById(player.getId())).thenReturn(Optional.of(player));
        when(fixture.auth.issue(player)).thenReturn(new AuthResponse(
                player.getId(), player.getEmail(), player.getNickname(), "jwt"));

        fixture.service.exchange(rawCode);

        assertNotNull(handoff.getConsumedAt());
        assertThrows(IllegalArgumentException.class, () -> fixture.service.exchange(rawCode));
        verify(fixture.auth, times(1)).issue(player);
    }

    private Fixture fixture(VerifiedGoogleIdentity identity) {
        var verifier = mock(GoogleIdentityVerifier.class);
        when(verifier.verify("credential")).thenReturn(identity);
        var users = mock(UserRepository.class);
        var identities = mock(UserIdentityRepository.class);
        var handoffs = mock(AuthHandoffCodeRepository.class);
        var secureTokens = new SecureTokenService();
        var encoder = new BCryptPasswordEncoder();
        var auth = mock(AuthService.class);
        var properties = mock(AuthFeatureProperties.class);
        when(properties.googleEnabled()).thenReturn(true);
        when(users.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            if (user.getId() == null) user.setId(42L);
            return user;
        });
        var service = new GoogleAuthService(verifier, secureTokens, users, identities, handoffs, encoder, auth, properties,
                new SimpleMeterRegistry());
        return new Fixture(service, secureTokens, verifier, users, identities, handoffs, encoder, auth, properties);
    }

    private record Fixture(
            GoogleAuthService service,
            SecureTokenService secureTokens,
            GoogleIdentityVerifier verifier,
            UserRepository users,
            UserIdentityRepository identities,
            AuthHandoffCodeRepository handoffs,
            BCryptPasswordEncoder encoder,
            AuthService auth,
            AuthFeatureProperties properties
    ) {
        UserEntity player(String email) {
            return UserEntity.builder()
                    .id(12L)
                    .email(email)
                    .passwordHash(encoder.encode("correct-password"))
                    .nickname("Existing Player")
                    .roles("USER")
                    .createdAt(Instant.now())
                    .build();
        }


        AuthHandoffCodeEntity linkHandoff(UserEntity player, String rawCode, int failedAttempts) {
            return AuthHandoffCodeEntity.builder()
                    .userId(player.getId())
                    .codeHash(secureTokens.hash(rawCode))
                    .purpose(GoogleAuthService.LINK_PURPOSE)
                    .providerSubject("google-sub")
                    .providerEmail(player.getEmail())
                    .failedAttempts(failedAttempts)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(60))
                    .build();
        }
    }
}
