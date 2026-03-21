package com.campos.webscraper.interfaces.dto;

import java.time.Instant;

/**
 * Recent crawl execution item for operational health visibility.
 */
public record RecentCrawlExecutionResponse(
        Long executionId,
        Long crawlJobId,
        String siteCode,
        String status,
        int itemsFound,
        Instant startedAt,
        Instant finishedAt,
        String errorMessage
) {
}
