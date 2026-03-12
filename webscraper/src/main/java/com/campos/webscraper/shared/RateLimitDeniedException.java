package com.campos.webscraper.shared;

/**
 * Signals that a fetch request was denied by the configured rate limiter.
 */
public class RateLimitDeniedException extends NonRetryableFetchException {

    public RateLimitDeniedException(String message) {
        super(message);
    }
}
