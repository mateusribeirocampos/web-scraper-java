package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.FetchedPage;
import com.campos.webscraper.shared.RateLimitDeniedException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Resilience4j-based rate limiter decorator for fetch operations keyed by site.
 */
public class RateLimitedJobFetcher implements JobFetcher {

    private final JobFetcher delegate;
    private final RateLimiterRegistry rateLimiterRegistry;

    public RateLimitedJobFetcher(JobFetcher delegate, RateLimiterRegistry rateLimiterRegistry) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.rateLimiterRegistry = Objects.requireNonNull(rateLimiterRegistry, "rateLimiterRegistry must not be null");
    }

    @Override
    public FetchedPage fetch(FetchRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(request.rateLimitKey());
        Supplier<FetchedPage> supplier = RateLimiter.decorateSupplier(rateLimiter, () -> delegate.fetch(request));

        try {
            return supplier.get();
        } catch (RequestNotPermitted exception) {
            throw new RateLimitDeniedException("Rate limit denied for site: " + request.rateLimitKey());
        }
    }
}
