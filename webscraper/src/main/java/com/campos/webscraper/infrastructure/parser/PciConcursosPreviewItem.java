package com.campos.webscraper.infrastructure.parser;

/**
 * Normalized preview of a single PCI Concursos listing entry.
 */
public record PciConcursosPreviewItem(
        String contestName,
        String organizer,
        String positionTitle,
        Integer numberOfVacancies,
        String educationLevel,
        String salaryDescription,
        String registrationStartDate,
        String registrationEndDate,
        String contestUrl,
        String detailUrl
) {
}
