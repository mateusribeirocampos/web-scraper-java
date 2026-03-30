package com.campos.webscraper.infrastructure.parser;

import java.time.LocalDate;
import java.util.List;

/**
 * Normalized preview of a single contest block extracted from the Inconfidentes municipal page.
 */
public record InconfidentesContestPreviewItem(
        String department,
        String contestTitle,
        String organizer,
        String positionTitle,
        String educationLevel,
        String formationRequirements,
        Integer editalYear,
        String contestUrl,
        String editalUrl,
        LocalDate registrationStartDate,
        LocalDate registrationEndDate,
        LocalDate examDate,
        List<InconfidentesContestAttachment> attachments,
        List<String> pdfPositionTitles,
        List<String> pdfAnnexReferences
) {
}
