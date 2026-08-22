package com.flip.backend.security;

import com.flip.backend.persistence.UserEntity;
import com.flip.backend.persistence.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthFilterVersionTest {
    private final JwtService jwt = new JwtService("01234567890123456789012345678901", 60_000);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsAValidlySignedTokenAfterTheUsersAuthVersionChanges() throws Exception {
        var users = mock(UserRepository.class);
        var details = mock(UserDetailsService.class);
        var user = player(2);
        when(users.findByEmailIgnoreCase("player@example.com")).thenReturn(Optional.of(user));
        when(details.loadUserByUsername(anyString())).thenReturn(User.withUsername(user.getEmail()).password("hash").roles("USER").build());
        var filter = new JwtAuthFilter(jwt, details, users);
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwt.generate(user.getEmail(), Map.of("ver", 1)));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(any(), any());
    }

    @Test
    void acceptsLegacyTokensWithoutAVersionForAnUnchangedUser() throws Exception {
        var users = mock(UserRepository.class);
        var details = mock(UserDetailsService.class);
        var user = player(0);
        when(users.findByEmailIgnoreCase("player@example.com")).thenReturn(Optional.of(user));
        when(details.loadUserByUsername(user.getEmail())).thenReturn(User.withUsername(user.getEmail()).password("hash").roles("USER").build());
        var filter = new JwtAuthFilter(jwt, details, users);
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwt.generate(user.getEmail(), Map.of("uid", 1)));

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private UserEntity player(int authVersion) {
        return UserEntity.builder()
                .id(1L)
                .email("player@example.com")
                .passwordHash("hash")
                .nickname("Player")
                .roles("USER")
                .createdAt(Instant.now())
                .authVersion(authVersion)
                .build();
    }
}
