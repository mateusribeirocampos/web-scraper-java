package com.campos.webscraper.interfaces.dto;

import java.time.LocalDate;

/**
 * Summary payload for public contests.
 */
public record PublicContestSummaryResponse(
        Long id,
        String contestName,
        String organizer,
        String positionTitle,
        String canonicalUrl,
        LocalDate publishedAt,
        LocalDate registrationEndDate
) {
}
