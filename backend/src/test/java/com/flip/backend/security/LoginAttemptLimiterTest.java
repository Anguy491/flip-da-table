package com.flip.backend.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptLimiterTest {
    @Test
    void blocksBothAccountAndSourceAfterTheConfiguredFailures() {
        var limiter = new LoginAttemptLimiter(3, Duration.ofMinutes(15), Clock.systemUTC());
        for (int attempt = 0; attempt < 3; attempt++) {
            limiter.check("user@example.com", "203.0.113.4");
            limiter.recordFailure("user@example.com", "203.0.113.4");
        }

        assertThrows(RateLimitExceededException.class,
                () -> limiter.check("user@example.com", "198.51.100.8"));
        assertThrows(RateLimitExceededException.class,
                () -> limiter.check("other@example.com", "203.0.113.4"));
    }

    @Test
    void successfulLoginClearsTheAccountBucketAndExpiredWindowsReset() {
        var clock = new MutableClock(Instant.parse("2026-08-23T00:00:00Z"));
        var limiter = new LoginAttemptLimiter(2, Duration.ofMinutes(1), clock);
        limiter.recordFailure("user@example.com", "source-1");
        limiter.recordFailure("user@example.com", "source-2");
        limiter.recordSuccess("user@example.com");
        assertDoesNotThrow(() -> limiter.check("user@example.com", "new-source"));

        limiter.recordFailure("other@example.com", "shared-source");
        limiter.recordFailure("third@example.com", "shared-source");
        assertThrows(RateLimitExceededException.class,
                () -> limiter.check("fresh@example.com", "shared-source"));
        clock.advance(Duration.ofMinutes(1));
        assertDoesNotThrow(() -> limiter.check("fresh@example.com", "shared-source"));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) { this.instant = instant; }
        private void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
