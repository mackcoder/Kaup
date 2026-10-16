package com.kaup.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthRateLimiter {
    private static final int MAX_ORIGINS = 4096;
    private final Map<String, Window> windows = new HashMap<>();
    private final Clock clock;
    private final int maxAttempts;
    private final Duration duration;

    public AuthRateLimiter(Clock clock, @Value("${auth.max-attempts:20}") int maxAttempts,
                          @Value("${auth.attempt-window:PT1M}") Duration duration) {
        if (maxAttempts < 1 || duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("Authentication limits must be positive");
        }
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.duration = duration;
    }

    public synchronized void check(String origin) {
        Instant now = clock.instant();
        windows.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        Window window = windows.get(origin);
        if ((window == null && windows.size() >= MAX_ORIGINS)
                || (window != null && window.attempts() >= maxAttempts)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        }
        windows.put(origin, window == null ? new Window(now.plus(duration), 1)
                : new Window(window.expiresAt(), window.attempts() + 1));
    }

    private record Window(Instant expiresAt, int attempts) { }
}
