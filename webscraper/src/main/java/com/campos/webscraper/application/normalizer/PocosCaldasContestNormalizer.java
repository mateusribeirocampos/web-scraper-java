package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.domain.enums.EducationLevel;
import com.campos.webscraper.domain.enums.GovernmentLevel;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.infrastructure.parser.PocosCaldasContestPreviewItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class PocosCaldasContestNormalizer {

    private final ObjectMapper objectMapper;

    public PocosCaldasContestNormalizer() {
        this(new ObjectMapper().findAndRegisterModules());
    }

    PocosCaldasContestNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public PublicContestPostingEntity normalize(PocosCaldasContestPreviewItem item, LocalDateTime fetchedAt) {
        Objects.requireNonNull(item, "item must not be null");
        Objects.requireNonNull(fetchedAt, "fetchedAt must not be null");

        return PublicContestPostingEntity.builder()
                .externalId("municipal_pocos_caldas:processo-seletivo-" + slugContestNumber(item.contestNumber()))
                .canonicalUrl(item.editalUrl())
                .contestName(item.contestTitle())
                .organizer(item.organizer())
                .positionTitle(item.positionTitle())
                .governmentLevel(GovernmentLevel.MUNICIPAL)
                .state("MG")
                .educationLevel(EducationLevel.valueOf(item.educationLevel()))
                .numberOfVacancies(item.numberOfVacancies())
                .salaryDescription(item.salaryDescription())
                .editalUrl(item.editalUrl())
                .publishedAt(resolvePublishedAt(item))
                .registrationStartDate(item.registrationStartDate())
                .registrationEndDate(item.registrationEndDate())
                .examDate(item.examDate())
                .contestStatus(resolveContestStatus(item, fetchedAt))
                .payloadJson(toJson(item))
                .createdAt(Instant.now())
                .build();
    }

    private LocalDate resolvePublishedAt(PocosCaldasContestPreviewItem item) {
        if (item.publishedAt() != null) {
            return item.publishedAt();
        }
        if (item.registrationStartDate() != null) {
            return item.registrationStartDate();
        }
        if (item.editalYear() != null) {
            return LocalDate.of(item.editalYear(), 1, 1);
        }
        throw new IllegalStateException("Poços de Caldas contest import requires a stable publishedAt fallback");
    }

    private ContestStatus resolveContestStatus(PocosCaldasContestPreviewItem item, LocalDateTime fetchedAt) {
        LocalDate today = fetchedAt.toLocalDate();
        if (item.registrationEndDate() != null) {
            return item.registrationEndDate().isBefore(today)
                    ? ContestStatus.REGISTRATION_CLOSED
                    : ContestStatus.OPEN;
        }
        if (item.editalYear() != null && item.editalYear() < today.getYear()) {
            return ContestStatus.REGISTRATION_CLOSED;
        }
        return ContestStatus.OPEN;
    }

    private String toJson(PocosCaldasContestPreviewItem item) {
        try {
            return objectMapper.writeValueAsString(toPayload(item));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Poços de Caldas preview payload for audit", exception);
        }
    }

    private Map<String, Object> toPayload(PocosCaldasContestPreviewItem item) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("contestTitle", item.contestTitle());
        payload.put("organizer", item.organizer());
        payload.put("positionTitle", item.positionTitle());
        payload.put("educationLevel", item.educationLevel());
        payload.put("contestNumber", item.contestNumber());
        payload.put("editalYear", item.editalYear());
        payload.put("editalUrl", item.editalUrl());
        payload.put("publishedAt", iso(item.publishedAt()));
        payload.put("registrationStartDate", iso(item.registrationStartDate()));
        payload.put("registrationEndDate", iso(item.registrationEndDate()));
        payload.put("examDate", iso(item.examDate()));
        payload.put("numberOfVacancies", item.numberOfVacancies());
        payload.put("salaryDescription", item.salaryDescription());
        payload.put("sourceExcerpt", item.sourceExcerpt());
        return payload;
    }

    private String iso(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    private String slugContestNumber(String contestNumber) {
        return contestNumber.toLowerCase()
                .replaceAll("[^0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
