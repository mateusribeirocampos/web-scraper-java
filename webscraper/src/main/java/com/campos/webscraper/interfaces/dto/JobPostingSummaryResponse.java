package com.campos.webscraper.interfaces.dto;

import java.time.LocalDate;

/**
 * Summary payload for private-sector job postings.
 */
public record JobPostingSummaryResponse(
        Long id,
        String title,
        String company,
        String canonicalUrl,
        LocalDate publishedAt
) {
}
