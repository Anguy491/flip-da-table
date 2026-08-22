package com.flip.backend.service;

import com.flip.backend.persistence.PasswordResetTokenEntity;
import com.flip.backend.persistence.PasswordResetTokenRepository;
import com.flip.backend.persistence.UserEntity;
import com.flip.backend.persistence.UserRepository;
import com.flip.backend.security.SecureTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "app.jwt.secret=01234567890123456789012345678901",
        "app.auth.password-reset.enabled=true",
        "app.auth.google.enabled=false",
        "spring.task.scheduling.enabled=false"
})
@Testcontainers(disabledWithoutDocker = true)
class PasswordResetConcurrencyIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired PasswordResetService service;
    @Autowired PasswordResetTokenRepository resetTokens;
    @Autowired UserRepository users;
    @Autowired SecureTokenService secureTokens;

    @Test
    void onlyOneConcurrentResetCanConsumeTheSameToken() throws Exception {
        UserEntity user = users.save(UserEntity.builder()
                .email("concurrent@example.com")
                .passwordHash("$2a$10$7EqJtq98hPqEX7fNZaFWoOhiLK7D6rW6d17OUYpEUDTLE2I6vL1Wa")
                .nickname("Concurrent Player")
                .roles("USER")
                .createdAt(Instant.now())
                .build());
        String rawToken = secureTokens.generate();
        resetTokens.save(PasswordResetTokenEntity.builder()
                .userId(user.getId())
                .tokenHash(secureTokens.hash(rawToken))
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(1800))
                .build());

        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> attempt = () -> {
            start.await(5, TimeUnit.SECONDS);
            try {
                service.resetPassword(rawToken, "new-password");
                return true;
            } catch (IllegalArgumentException ex) {
                return false;
            }
        };
        var pool = Executors.newFixedThreadPool(2);
        try {
            var futures = List.of(pool.submit(attempt), pool.submit(attempt));
            start.countDown();
            int successes = 0;
            for (var future : futures) if (future.get(10, TimeUnit.SECONDS)) successes++;

            assertEquals(1, successes);
            assertEquals(1, users.findById(user.getId()).orElseThrow().getAuthVersion());
        } finally {
            pool.shutdownNow();
        }
    }
}
