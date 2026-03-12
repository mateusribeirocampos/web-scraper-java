package com.campos.webscraper.shared;

import com.campos.webscraper.infrastructure.http.HttpJobFetcher;
import com.campos.webscraper.infrastructure.http.JobFetcher;
import com.campos.webscraper.infrastructure.http.RateLimitedJobFetcher;
import com.campos.webscraper.infrastructure.http.RetryableJobFetcher;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Baseline rate limiting policy for page/API fetch operations.
 */
@Configuration
public class FetchRateLimitConfiguration {

    @Bean
    public RateLimiterRegistry rateLimiterRegistry(
            @Value("${webscraper.fetch.rate-limit.limit-for-period:10}") int limitForPeriod,
            @Value("${webscraper.fetch.rate-limit.refresh-period-ms:60000}") long refreshPeriodMs,
            @Value("${webscraper.fetch.rate-limit.timeout-duration-ms:0}") long timeoutDurationMs
    ) {
        return RateLimiterRegistry.of(RateLimiterConfig.custom()
                .limitForPeriod(limitForPeriod)
                .limitRefreshPeriod(Duration.ofMillis(refreshPeriodMs))
                .timeoutDuration(Duration.ofMillis(timeoutDurationMs))
                .build());
    }

    @Bean
    public JobFetcher jobFetcher(RateLimiterRegistry rateLimiterRegistry, Retry jobFetcherRetry) {
        JobFetcher httpJobFetcher = new HttpJobFetcher();
        JobFetcher rateLimitedJobFetcher = new RateLimitedJobFetcher(httpJobFetcher, rateLimiterRegistry);
        return new RetryableJobFetcher(rateLimitedJobFetcher, jobFetcherRetry);
    }
}
