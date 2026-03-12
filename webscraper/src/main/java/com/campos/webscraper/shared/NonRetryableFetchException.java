package com.campos.webscraper.shared;

/**
 * Signals a fetch failure that should not be retried.
 */
public class NonRetryableFetchException extends RuntimeException {

    public NonRetryableFetchException(String message) {
        super(message);
    }

    public NonRetryableFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
