package com.campos.webscraper.application.queue;

/**
 * Tracks scheduler-managed crawl jobs that are already enqueued or executing in the current process.
 */
public interface InFlightCrawlJobRegistry {

    boolean tryClaim(Long crawlJobId);

    void release(Long crawlJobId);
}
