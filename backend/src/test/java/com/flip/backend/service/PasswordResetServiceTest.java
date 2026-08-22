package com.flip.backend.service;

import com.flip.backend.persistence.*;
import com.flip.backend.security.AuthFeatureProperties;
import com.flip.backend.security.SecureTokenService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {
    @Test
    void storesOnlyTheResetTokenHashAndPublishesTheRawTokenAfterRequest() {
        var users = mock(UserRepository.class);
        var resetTokens = mock(PasswordResetTokenRepository.class);
        var properties = mock(AuthFeatureProperties.class);
        var events = mock(ApplicationEventPublisher.class);
        var secureTokens = new SecureTokenService();
        var user = player();
        when(properties.passwordResetEnabled()).thenReturn(true);
        when(users.findByEmailForUpdate("player@example.com")).thenReturn(Optional.of(user));
        when(resetTokens.findTopByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.empty());
        when(resetTokens.findByUserIdAndUsedAtIsNull(user.getId())).thenReturn(List.of());
        var service = new PasswordResetService(users, resetTokens, new BCryptPasswordEncoder(), secureTokens,
                properties, events, new SimpleMeterRegistry());

        service.requestReset(" PLAYER@example.com ");

        var tokenCaptor = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        var eventCaptor = ArgumentCaptor.forClass(PasswordResetMailEvent.class);
        verify(resetTokens).save(tokenCaptor.capture());
        verify(events).publishEvent(eventCaptor.capture());
        assertEquals(secureTokens.hash(eventCaptor.getValue().token()), tokenCaptor.getValue().getTokenHash());
        assertNotEquals(eventCaptor.getValue().token(), tokenCaptor.getValue().getTokenHash());
    }

    @Test
    void consumesAllOutstandingTokensAndRevokesExistingJwtVersionsOnReset() {
        var users = mock(UserRepository.class);
        var resetTokens = mock(PasswordResetTokenRepository.class);
        var properties = mock(AuthFeatureProperties.class);
        var secureTokens = new SecureTokenService();
        var user = player();
        String raw = secureTokens.generate();
        var token = PasswordResetTokenEntity.builder()
                .id(4L)
                .userId(user.getId())
                .tokenHash(secureTokens.hash(raw))
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        when(properties.passwordResetEnabled()).thenReturn(true);
        when(resetTokens.findByTokenHashForUpdate(token.getTokenHash())).thenReturn(Optional.of(token));
        when(resetTokens.findByUserIdAndUsedAtIsNull(user.getId())).thenReturn(List.of(token));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        var service = new PasswordResetService(users, resetTokens, new BCryptPasswordEncoder(), secureTokens,
                properties, mock(ApplicationEventPublisher.class), new SimpleMeterRegistry());

        service.resetPassword(raw, "new-password");

        assertEquals(1, user.getAuthVersion());
        assertNotNull(token.getUsedAt());
        assertTrue(new BCryptPasswordEncoder().matches("new-password", user.getPasswordHash()));
    }

    @Test
    void rejectsExpiredAndAlreadyConsumedTokensWithoutChangingThePassword() {
        var users = mock(UserRepository.class);
        var resetTokens = mock(PasswordResetTokenRepository.class);
        var properties = mock(AuthFeatureProperties.class);
        var secureTokens = new SecureTokenService();
        var user = player();
        String originalHash = user.getPasswordHash();
        String raw = secureTokens.generate();
        var expired = PasswordResetTokenEntity.builder()
                .userId(user.getId())
                .tokenHash(secureTokens.hash(raw))
                .createdAt(Instant.now().minusSeconds(3600))
                .expiresAt(Instant.now().minusSeconds(1))
                .build();
        when(properties.passwordResetEnabled()).thenReturn(true);
        when(resetTokens.findByTokenHashForUpdate(expired.getTokenHash())).thenReturn(Optional.of(expired));
        var service = new PasswordResetService(users, resetTokens, new BCryptPasswordEncoder(), secureTokens,
                properties, mock(ApplicationEventPublisher.class), new SimpleMeterRegistry());

        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(raw, "new-password"));
        assertEquals(originalHash, user.getPasswordHash());
        verify(users, never()).save(any());

        expired.setExpiresAt(Instant.now().plusSeconds(60));
        expired.setUsedAt(Instant.now());
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(raw, "new-password"));
        verify(users, never()).save(any());
    }

    @Test
    void enforcesPerAccountCooldownWithoutPublishingAnotherEmail() {
        var users = mock(UserRepository.class);
        var resetTokens = mock(PasswordResetTokenRepository.class);
        var properties = mock(AuthFeatureProperties.class);
        var events = mock(ApplicationEventPublisher.class);
        var user = player();
        when(properties.passwordResetEnabled()).thenReturn(true);
        when(users.findByEmailForUpdate(user.getEmail())).thenReturn(Optional.of(user));
        when(resetTokens.findTopByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.of(
                PasswordResetTokenEntity.builder()
                        .userId(user.getId())
                        .tokenHash("a".repeat(64))
                        .createdAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(1800))
                        .build()));
        var service = new PasswordResetService(users, resetTokens, new BCryptPasswordEncoder(),
                new SecureTokenService(), properties, events, new SimpleMeterRegistry());

        service.requestReset(user.getEmail());

        verify(resetTokens, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void unknownEmailDoesNotCreateARecoveryRecordOrEmail() {
        var users = mock(UserRepository.class);
        var resetTokens = mock(PasswordResetTokenRepository.class);
        var properties = mock(AuthFeatureProperties.class);
        var events = mock(ApplicationEventPublisher.class);
        when(properties.passwordResetEnabled()).thenReturn(true);
        when(users.findByEmailForUpdate("missing@example.com")).thenReturn(Optional.empty());
        var service = new PasswordResetService(users, resetTokens, new BCryptPasswordEncoder(),
                new SecureTokenService(), properties, events, new SimpleMeterRegistry());

        service.requestReset("Missing@Example.com");

        verifyNoInteractions(resetTokens, events);
    }

    private UserEntity player() {
        return UserEntity.builder()
                .id(7L)
                .email("player@example.com")
                .passwordHash(new BCryptPasswordEncoder().encode("old-password"))
                .nickname("Pixel Pilot")
                .roles("USER")
                .createdAt(Instant.now())
                .build();
    }
}
