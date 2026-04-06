package com.campos.webscraper.infrastructure.parser;

import java.time.LocalDate;
import java.util.List;

/**
 * Normalized preview of one public contest block extracted from the Câmara page.
 */
public record CamaraSantaRitaContestPreviewItem(
        String contestTitle,
        String organizer,
        String positionTitle,
        String educationLevel,
        String contestNumber,
        Integer editalYear,
        String contestUrl,
        String editalUrl,
        LocalDate publishedAt,
        LocalDate registrationStartDate,
        LocalDate registrationEndDate,
        List<CamaraSantaRitaContestAttachment> attachments
) {
}
