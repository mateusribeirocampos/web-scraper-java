package com.campos.webscraper.shared;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Baseline circuit breaker policy for crawl job execution.
 */
@Configuration
public class CircuitBreakerConfiguration {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(
            @Value("${webscraper.circuit-breaker.failure-rate-threshold:50}") float failureRateThreshold,
            @Value("${webscraper.circuit-breaker.sliding-window-size:10}") int slidingWindowSize,
            @Value("${webscraper.circuit-breaker.minimum-number-of-calls:5}") int minimumNumberOfCalls,
            @Value("${webscraper.circuit-breaker.wait-duration-open-ms:60000}") long waitDurationOpenMs
    ) {
        return CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .waitDurationInOpenState(Duration.ofMillis(waitDurationOpenMs))
                .build());
    }
}
