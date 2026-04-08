package com.campos.webscraper.infrastructure.parser;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record CamaraItajubaContestPreviewItem(
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
        LocalDate examDate,
        Integer numberOfVacancies,
        String salaryDescription,
        List<CamaraItajubaContestAttachment> attachments
) {

    public CamaraItajubaContestPreviewItem {
        Objects.requireNonNull(contestTitle, "contestTitle must not be null");
        Objects.requireNonNull(organizer, "organizer must not be null");
        Objects.requireNonNull(positionTitle, "positionTitle must not be null");
        Objects.requireNonNull(educationLevel, "educationLevel must not be null");
        Objects.requireNonNull(contestUrl, "contestUrl must not be null");
        Objects.requireNonNull(attachments, "attachments must not be null");
    }
}
