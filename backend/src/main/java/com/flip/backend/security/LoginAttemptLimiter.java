package com.flip.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class LoginAttemptLimiter {
    private static final int MAX_TRACKED_KEYS = 10_000;

    private final int maxFailures;
    private final Duration window;
    private final Clock clock;
    private final Map<String, FailureWindow> failures = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, FailureWindow> eldest) {
            return size() > MAX_TRACKED_KEYS;
        }
    };

    @Autowired
    public LoginAttemptLimiter(
            @Value("${app.auth.login.max-failures:5}") int maxFailures,
            @Value("${app.auth.login.window-seconds:900}") long windowSeconds
    ) {
        this(maxFailures, Duration.ofSeconds(windowSeconds), Clock.systemUTC());
    }

    LoginAttemptLimiter(int maxFailures, Duration window, Clock clock) {
        if (maxFailures < 1 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("invalid login rate-limit configuration");
        }
        this.maxFailures = maxFailures;
        this.window = window;
        this.clock = clock;
    }

    public synchronized void check(String email, String source) {
        Instant now = clock.instant();
        checkKey(accountKey(email), now);
        checkKey(sourceKey(source), now);
    }

    public synchronized void recordFailure(String email, String source) {
        Instant now = clock.instant();
        increment(accountKey(email), now);
        increment(sourceKey(source), now);
    }

    public synchronized void recordSuccess(String email) {
        failures.remove(accountKey(email));
    }

    private void checkKey(String key, Instant now) {
        FailureWindow current = activeWindow(key, now);
        if (current != null && current.count >= maxFailures) {
            long retryAfter = Duration.between(now, current.started.plus(window)).toSeconds();
            throw new RateLimitExceededException("too many login attempts", retryAfter);
        }
    }

    private void increment(String key, Instant now) {
        FailureWindow current = activeWindow(key, now);
        if (current == null) failures.put(key, new FailureWindow(now, 1));
        else current.count++;
    }

    private FailureWindow activeWindow(String key, Instant now) {
        FailureWindow current = failures.get(key);
        if (current != null && !now.isBefore(current.started.plus(window))) {
            failures.remove(key);
            return null;
        }
        return current;
    }

    private String accountKey(String email) {
        return "account:" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
    }

    private String sourceKey(String source) {
        return "source:" + (source == null || source.isBlank() ? "unknown" : source.trim());
    }

    private static final class FailureWindow {
        private final Instant started;
        private int count;

        private FailureWindow(Instant started, int count) {
            this.started = started;
            this.count = count;
        }
    }
}
