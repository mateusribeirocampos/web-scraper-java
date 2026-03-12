package com.campos.webscraper.shared;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Baseline retry policy for page/API fetch operations.
 */
@Configuration
public class FetchRetryConfiguration {

    @Bean
    public Retry jobFetcherRetry(
            @Value("${webscraper.fetch.retry.max-attempts:3}") int maxAttempts,
            @Value("${webscraper.fetch.retry.wait-duration-ms:200}") long waitDurationMs
    ) {
        return Retry.of("jobFetcher", RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.ofMillis(waitDurationMs))
                .retryExceptions(RetryableFetchException.class)
                .failAfterMaxAttempts(false)
                .build());
    }
}
