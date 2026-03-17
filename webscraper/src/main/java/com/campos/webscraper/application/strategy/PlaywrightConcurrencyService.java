package com.campos.webscraper.application.strategy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/**
 * Limits the number of concurrent Playwright jobs running inside the application.
 */
@Component
public class PlaywrightConcurrencyService {

    private final Semaphore permits;

    public PlaywrightConcurrencyService(@Value("${playwright.concurrent-jobs:2}") int maxConcurrentJobs) {
        if (maxConcurrentJobs <= 0) {
            throw new IllegalArgumentException("maxConcurrentJobs must be positive");
        }
        this.permits = new Semaphore(maxConcurrentJobs);
    }

    /**
     * Executes the provided supplier while holding a permit.
     */
    public <T> T execute(Supplier<T> work) {
        Objects.requireNonNull(work, "work must not be null");
        boolean acquired = false;
        try {
            permits.acquire();
            acquired = true;
            return work.get();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Playwright permit", interrupted);
        } finally {
            if (acquired) {
                permits.release();
            }
        }
    }
}
