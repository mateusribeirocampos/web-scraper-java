package com.campos.webscraper.infrastructure.parser;

import java.time.LocalDate;
import java.util.List;

/**
 * Normalized preview of a single contest extracted from Extrema municipal pages.
 */
public record ExtremaContestPreviewItem(
        String contestTitle,
        String organizer,
        String positionTitle,
        String educationLevel,
        String formationRequirements,
        String contestNumber,
        Integer editalYear,
        String contestUrl,
        String editalUrl,
        LocalDate publishedAt,
        LocalDate registrationStartDate,
        LocalDate registrationEndDate,
        LocalDate examDate,
        List<ExtremaContestAttachment> attachments,
        List<String> pdfPositionTitles,
        List<String> pdfAnnexReferences
) {
}
