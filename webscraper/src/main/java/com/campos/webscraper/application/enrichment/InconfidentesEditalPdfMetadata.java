package com.campos.webscraper.application.enrichment;

import java.time.LocalDate;

/**
 * Metadata extracted from the main Inconfidentes edital PDF.
 */
public record InconfidentesEditalPdfMetadata(
        String positionTitle,
        String educationLevel,
        String formationRequirements,
        LocalDate registrationStartDate,
        LocalDate registrationEndDate,
        LocalDate examDate
) {
}
