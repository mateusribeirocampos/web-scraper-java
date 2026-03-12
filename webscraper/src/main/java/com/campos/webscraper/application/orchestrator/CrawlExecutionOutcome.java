package com.campos.webscraper.application.orchestrator;

/**
 * Execution counters produced by a crawl run.
 */
public record CrawlExecutionOutcome(
        int pagesVisited,
        int itemsFound
) {
}
