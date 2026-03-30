package com.campos.webscraper.application.enrichment;

import java.time.LocalDate;
import java.util.List;

/**
 * Metadata extracted from the main Inconfidentes edital PDF.
 */
public record InconfidentesEditalPdfMetadata(
        String positionTitle,
        List<String> positionTitles,
        String educationLevel,
        String formationRequirements,
        LocalDate registrationStartDate,
        LocalDate registrationEndDate,
        LocalDate examDate,
        List<String> annexReferences
) {
}
