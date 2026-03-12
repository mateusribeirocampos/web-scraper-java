package com.campos.webscraper.shared;

/**
 * Signals a transient fetch failure that can be retried safely.
 */
public class RetryableFetchException extends RuntimeException {

    public RetryableFetchException(String message) {
        super(message);
    }

    public RetryableFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
