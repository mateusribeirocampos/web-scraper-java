package com.campos.webscraper.interfaces.dto;

/**
 * Aggregate queue message count by queue and status.
 */
public record ScraperQueueStatusCountResponse(
        String queueName,
        String status,
        long count
) {
}
