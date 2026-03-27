package com.campos.webscraper.infrastructure.parser;

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
        Integer editalYear,
        String contestUrl,
        String editalUrl,
        List<InconfidentesContestAttachment> attachments
) {
}
