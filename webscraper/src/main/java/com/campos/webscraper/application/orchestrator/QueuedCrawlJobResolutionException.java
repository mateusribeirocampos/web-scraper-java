package com.campos.webscraper.application.orchestrator;

/**
 * Raised when a queued job cannot be resolved into a runnable crawl job due to invalid persisted references.
 */
public class QueuedCrawlJobResolutionException extends RuntimeException {

    public QueuedCrawlJobResolutionException(String message) {
        super(message);
    }
}
