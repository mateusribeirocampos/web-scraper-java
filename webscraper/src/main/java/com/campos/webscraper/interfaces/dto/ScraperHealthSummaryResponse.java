package com.campos.webscraper.interfaces.dto;

import java.time.Instant;
import java.util.List;

/**
 * Operational health summary for the scraper runtime.
 */
public record ScraperHealthSummaryResponse(
        Instant generatedAt,
        List<ScraperExecutionStatusCountResponse> executionCounts,
        List<ScraperQueueStatusCountResponse> queueCounts,
        List<RecentCrawlExecutionResponse> recentExecutions
) {
}
