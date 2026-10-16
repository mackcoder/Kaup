package com.kaup.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthRateLimiterTest {
    @Test
    void limitsPerOriginAndReleasesTheWindow() {
        Clock clock = mock(Clock.class);
        Instant start = Instant.parse("2026-10-16T12:00:00Z");
        when(clock.instant()).thenReturn(start);
        var limiter = new AuthRateLimiter(clock, 2, Duration.ofMinutes(1));
        limiter.check("origin-a");
        limiter.check("origin-a");
        assertThatThrownBy(() -> limiter.check("origin-a"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(429));
        limiter.check("origin-b");
        when(clock.instant()).thenReturn(start.plusSeconds(60));
        assertThatCode(() -> limiter.check("origin-a")).doesNotThrowAnyException();
    }

    @Test
    void concurrentRequestsCannotExceedTheLimit() throws Exception {
        var limiter = new AuthRateLimiter(Clock.systemUTC(), 20, Duration.ofMinutes(1));
        var executor = Executors.newFixedThreadPool(8);
        var accepted = new AtomicInteger();
        var results = new ArrayList<Future<?>>();
        try {
            for (int i = 0; i < 80; i++) {
                results.add(executor.submit(() -> {
                    try { limiter.check("same-origin"); accepted.incrementAndGet(); }
                    catch (ResponseStatusException expected) { assertThat(expected.getStatusCode().value()).isEqualTo(429); }
                }));
            }
            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            for (var result : results) { result.get(); }
            assertThat(accepted.get()).isEqualTo(20);
        } finally { executor.shutdownNow(); }
    }
}
