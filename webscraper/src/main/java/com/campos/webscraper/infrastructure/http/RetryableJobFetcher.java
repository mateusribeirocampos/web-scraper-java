package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.FetchedPage;
import io.github.resilience4j.retry.Retry;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Resilience4j-based retry decorator for fetch operations.
 */
public class RetryableJobFetcher implements JobFetcher {

    private final JobFetcher delegate;
    private final Retry retry;

    public RetryableJobFetcher(JobFetcher delegate, Retry retry) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.retry = Objects.requireNonNull(retry, "retry must not be null");
    }

    @Override
    public FetchedPage fetch(FetchRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Supplier<FetchedPage> supplier = Retry.decorateSupplier(retry, () -> delegate.fetch(request));
        return supplier.get();
    }
}
