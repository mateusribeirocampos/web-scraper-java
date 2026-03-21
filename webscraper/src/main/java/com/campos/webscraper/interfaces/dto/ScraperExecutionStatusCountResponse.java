package com.campos.webscraper.interfaces.dto;

/**
 * Aggregate execution count by lifecycle status.
 */
public record ScraperExecutionStatusCountResponse(
        String status,
        long count
) {
}
