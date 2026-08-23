package com.flip.backend.service;

import com.flip.backend.api.dto.AuthDtos.*;
import com.flip.backend.persistence.UserEntity;
import com.flip.backend.persistence.UserRepository;
import com.flip.backend.security.EmailNormalizer;
import com.flip.backend.security.JwtService;
import com.flip.backend.security.LoginAttemptLimiter;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class AuthService {
    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final LoginAttemptLimiter loginAttempts;

    public AuthService(UserRepository repo, PasswordEncoder encoder,
                       AuthenticationManager authManager, JwtService jwt, LoginAttemptLimiter loginAttempts) {
        this.repo = repo;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwt = jwt;
        this.loginAttempts = loginAttempts;
    }

    public AuthResponse register(RegisterRequest r) {
        String email = EmailNormalizer.normalize(r.email());
        repo.findByEmailIgnoreCase(email).ifPresent(u -> { throw new IllegalArgumentException("email exists"); });
        var user = UserEntity.builder()
                .email(email)
                .passwordHash(encoder.encode(r.password()))
                .nickname(r.nickname())
                .roles("USER")
                .createdAt(Instant.now())
                .build();
        user = repo.save(user);
        return issue(user);
    }

    public AuthResponse login(LoginRequest r) {
        return login(r, "unknown");
    }

    public AuthResponse login(LoginRequest r, String source) {
        String email = EmailNormalizer.normalize(r.email());
        loginAttempts.check(email, source);
        Authentication auth = new UsernamePasswordAuthenticationToken(email, r.password());
        try {
            authManager.authenticate(auth);
        } catch (org.springframework.security.core.AuthenticationException ex) {
            loginAttempts.recordFailure(email, source);
            throw new org.springframework.security.authentication.BadCredentialsException("bad credentials");
        }
        var user = repo.findByEmailIgnoreCase(email).orElseGet(() -> {
            loginAttempts.recordFailure(email, source);
            throw new org.springframework.security.authentication.BadCredentialsException("bad credentials");
        });
        loginAttempts.recordSuccess(email);
        return issue(user);
    }

    public AuthResponse issue(UserEntity user) {
        String token = jwt.generate(user.getEmail(), Map.of(
                "uid", user.getId(),
                "nick", user.getNickname(),
                "roles", user.getRoles(),
                "ver", user.getAuthVersion()
        ));
        return new AuthResponse(user.getId(), user.getEmail(), user.getNickname(), token);
    }
}
