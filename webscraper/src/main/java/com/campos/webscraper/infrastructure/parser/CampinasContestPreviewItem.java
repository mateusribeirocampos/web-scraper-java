package com.campos.webscraper.infrastructure.parser;

import java.time.LocalDate;

/**
 * Normalized preview of one official Campinas public-contest alert exposed by the municipal portal.
 */
public record CampinasContestPreviewItem(
        String contestTitle,
        String organizer,
        String positionTitle,
        String contestCode,
        String officialSiteUrl,
        String sourceApiUrl,
        String editalUrl,
        LocalDate publishedAt,
        LocalDate registrationStartDate,
        LocalDate registrationEndDate
) {
}
