package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.FetchedPage;
import com.campos.webscraper.shared.RateLimitDeniedException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for rate limiting around JobFetcher.
 *
 * TDD RED: written before the rate limiter decorator exists.
 */
@Tag("unit")
@DisplayName("RateLimitedJobFetcher")
class RateLimitedJobFetcherTest {

    @Test
    @DisplayName("should allow request when permit is available")
    void shouldAllowRequestWhenPermitIsAvailable() {
        AtomicInteger attempts = new AtomicInteger();
        JobFetcher delegate = request -> {
            attempts.incrementAndGet();
            return new FetchedPage(request.url(), "<html>ok</html>", 200, "text/html", LocalDateTime.now());
        };

        RateLimiterRegistry registry = RateLimiterRegistry.of(RateLimiterConfig.custom()
                .limitForPeriod(1)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ZERO)
                .build());

        RateLimitedJobFetcher fetcher = new RateLimitedJobFetcher(delegate, registry);

        FetchedPage page = fetcher.fetch(FetchRequest.of("https://example.com/jobs", "indeed-br"));

        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("should deny request when site rate limit has no available permits")
    void shouldDenyRequestWhenSiteRateLimitHasNoAvailablePermits() {
        AtomicInteger attempts = new AtomicInteger();
        JobFetcher delegate = request -> {
            attempts.incrementAndGet();
            return new FetchedPage(request.url(), "<html>ok</html>", 200, "text/html", LocalDateTime.now());
        };

        RateLimiterRegistry registry = RateLimiterRegistry.of(RateLimiterConfig.custom()
                .limitForPeriod(1)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ZERO)
                .build());

        RateLimitedJobFetcher fetcher = new RateLimitedJobFetcher(delegate, registry);
        FetchRequest request = FetchRequest.of("https://example.com/jobs", "indeed-br");

        fetcher.fetch(request);

        assertThatThrownBy(() -> fetcher.fetch(request))
                .isInstanceOf(RateLimitDeniedException.class)
                .hasMessageContaining("indeed-br");

        assertThat(attempts.get()).isEqualTo(1);
    }
}
