package com.campos.webscraper.shared;

/**
 * Raised when a manual execution references a crawl job id that does not exist.
 */
public class CrawlJobNotFoundException extends RuntimeException {

    public CrawlJobNotFoundException(Long jobId) {
        super("Crawl job not found: " + jobId);
    }
}
