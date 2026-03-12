package com.campos.webscraper.interfaces.dto;

/**
 * DTO representing a single Indeed MCP job response payload.
 */
public record IndeedApiResponse(
        String jobId,
        String title,
        String company,
        String location,
        String postedAt,
        String applyUrl
) {
}
