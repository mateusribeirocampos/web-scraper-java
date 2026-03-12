package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.FetchedPage;
import com.campos.webscraper.shared.NonRetryableFetchException;
import com.campos.webscraper.shared.RetryableFetchException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for Resilience4j retry around JobFetcher.
 *
 * TDD RED: written before the retry decorator exists.
 */
@Tag("unit")
@DisplayName("RetryableJobFetcher")
class RetryableJobFetcherTest {

    @Test
    @DisplayName("should retry retryable failures and eventually return success")
    void shouldRetryRetryableFailuresAndEventuallyReturnSuccess() {
        AtomicInteger attempts = new AtomicInteger();
        JobFetcher delegate = request -> {
            int currentAttempt = attempts.incrementAndGet();
            if (currentAttempt < 3) {
                throw new RetryableFetchException("temporary timeout");
            }
            return new FetchedPage(request.url(), "<html>ok</html>", 200, "text/html", LocalDateTime.now());
        };

        Retry retry = Retry.of("jobFetcher", RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ZERO)
                .retryExceptions(RetryableFetchException.class)
                .failAfterMaxAttempts(true)
                .build());

        RetryableJobFetcher fetcher = new RetryableJobFetcher(delegate, retry);

        FetchedPage page = fetcher.fetch(FetchRequest.of("https://example.com/jobs"));

        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("should not retry non retryable failures")
    void shouldNotRetryNonRetryableFailures() {
        AtomicInteger attempts = new AtomicInteger();
        JobFetcher delegate = request -> {
            attempts.incrementAndGet();
            throw new NonRetryableFetchException("blocked by robots");
        };

        Retry retry = Retry.of("jobFetcher", RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ZERO)
                .retryExceptions(RetryableFetchException.class)
                .failAfterMaxAttempts(true)
                .build());

        RetryableJobFetcher fetcher = new RetryableJobFetcher(delegate, retry);

        assertThatThrownBy(() -> fetcher.fetch(FetchRequest.of("https://example.com/jobs")))
                .isInstanceOf(NonRetryableFetchException.class)
                .hasMessage("blocked by robots");

        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("should preserve retryable exception type after max attempts are exhausted")
    void shouldPreserveRetryableExceptionTypeAfterMaxAttemptsAreExhausted() {
        AtomicInteger attempts = new AtomicInteger();
        JobFetcher delegate = request -> {
            attempts.incrementAndGet();
            throw new RetryableFetchException("temporary timeout");
        };

        Retry retry = Retry.of("jobFetcher", RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ZERO)
                .retryExceptions(RetryableFetchException.class)
                .failAfterMaxAttempts(false)
                .build());

        RetryableJobFetcher fetcher = new RetryableJobFetcher(delegate, retry);

        assertThatThrownBy(() -> fetcher.fetch(FetchRequest.of("https://example.com/jobs")))
                .isInstanceOf(RetryableFetchException.class)
                .hasMessage("temporary timeout");

        assertThat(attempts.get()).isEqualTo(3);
    }
}
