package com.campos.webscraper.infrastructure.parser;

import java.time.LocalDate;
import java.util.List;

/**
 * Normalized preview of a single contest extracted from Pouso Alegre municipal pages.
 */
public record PousoAlegreContestPreviewItem(
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
        List<PousoAlegreContestAttachment> attachments,
        List<String> pdfPositionTitles,
        List<String> pdfAnnexReferences
) {
}
