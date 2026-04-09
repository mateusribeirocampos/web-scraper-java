package com.campos.webscraper.infrastructure.parser;

import java.time.LocalDate;
import java.util.Objects;

public record PocosCaldasContestPreviewItem(
        String contestTitle,
        String organizer,
        String positionTitle,
        String educationLevel,
        String contestNumber,
        Integer editalYear,
        String editalUrl,
        LocalDate publishedAt,
        LocalDate registrationStartDate,
        LocalDate registrationEndDate,
        LocalDate examDate,
        Integer numberOfVacancies,
        String salaryDescription,
        String sourceExcerpt
) {

    public PocosCaldasContestPreviewItem {
        Objects.requireNonNull(contestTitle, "contestTitle must not be null");
        Objects.requireNonNull(organizer, "organizer must not be null");
        Objects.requireNonNull(positionTitle, "positionTitle must not be null");
        Objects.requireNonNull(educationLevel, "educationLevel must not be null");
        Objects.requireNonNull(editalUrl, "editalUrl must not be null");
        Objects.requireNonNull(sourceExcerpt, "sourceExcerpt must not be null");
    }
}
