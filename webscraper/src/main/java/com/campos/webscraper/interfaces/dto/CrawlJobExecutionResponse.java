package com.campos.webscraper.interfaces.dto;

/**
 * Response returned after a manual crawl job trigger request is accepted.
 */
public record CrawlJobExecutionResponse(
        Long jobId,
        String status
) {
}
