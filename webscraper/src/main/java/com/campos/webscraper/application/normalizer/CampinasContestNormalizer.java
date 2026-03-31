package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.domain.enums.EducationLevel;
import com.campos.webscraper.domain.enums.GovernmentLevel;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.infrastructure.parser.CampinasContestPreviewItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Maps Campinas official contest alerts into the canonical public contest entity shape.
 */
@Component
public class CampinasContestNormalizer {

    private final ObjectMapper objectMapper;

    public CampinasContestNormalizer() {
        this(new ObjectMapper().findAndRegisterModules());
    }

    CampinasContestNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public PublicContestPostingEntity normalize(CampinasContestPreviewItem item, LocalDateTime fetchedAt) {
        Objects.requireNonNull(item, "item must not be null");
        Objects.requireNonNull(fetchedAt, "fetchedAt must not be null");

        if (item.editalUrl() == null || item.editalUrl().isBlank()) {
            throw new IllegalArgumentException("Campinas contest item must have an official alert URL");
        }

        return PublicContestPostingEntity.builder()
                .externalId(buildStableContestId(item))
                .canonicalUrl(item.editalUrl())
                .contestName(item.contestTitle())
                .organizer(item.organizer())
                .positionTitle(resolvePositionTitle(item))
                .governmentLevel(GovernmentLevel.MUNICIPAL)
                .state("SP")
                .educationLevel(EducationLevel.UNKNOWN)
                .editalUrl(item.editalUrl())
                .publishedAt(item.publishedAt() != null ? item.publishedAt() : fetchedAt.toLocalDate())
                .registrationStartDate(item.registrationStartDate())
                .registrationEndDate(item.registrationEndDate())
                .contestStatus(resolveContestStatus(item, fetchedAt))
                .payloadJson(toJson(item))
                .createdAt(Instant.now())
                .build();
    }

    private String resolvePositionTitle(CampinasContestPreviewItem item) {
        String positionTitle = sanitizeText(item.positionTitle());
        if (!positionTitle.isBlank()) {
            return positionTitle;
        }
        String fallback = sanitizeText(item.contestTitle());
        if (!fallback.isBlank()) {
            return fallback;
        }
        throw new IllegalArgumentException("Campinas contest item must have a non-blank title");
    }

    private String buildStableContestId(CampinasContestPreviewItem item) {
        String seed = item.contestCode() == null || item.contestCode().isBlank()
                ? item.contestTitle()
                : item.contestCode();
        String normalized = Normalizer.normalize(seed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return "municipal_campinas:" + normalized;
    }

    private ContestStatus resolveContestStatus(CampinasContestPreviewItem item, LocalDateTime fetchedAt) {
        if (item.registrationEndDate() != null && item.registrationEndDate().isBefore(fetchedAt.toLocalDate())) {
            return ContestStatus.REGISTRATION_CLOSED;
        }
        return ContestStatus.OPEN;
    }

    private String toJson(CampinasContestPreviewItem item) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("contestTitle", sanitizeText(item.contestTitle()));
        payload.put("organizer", sanitizeText(item.organizer()));
        payload.put("positionTitle", sanitizeText(item.positionTitle()));
        payload.put("contestCode", sanitizeText(item.contestCode()));
        payload.put("officialSiteUrl", sanitizeText(item.officialSiteUrl()));
        payload.put("sourceApiUrl", sanitizeText(item.sourceApiUrl()));
        payload.put("editalUrl", sanitizeText(item.editalUrl()));
        payload.put("publishedAt", toIsoDate(item.publishedAt()));
        payload.put("registrationStartDate", toIsoDate(item.registrationStartDate()));
        payload.put("registrationEndDate", toIsoDate(item.registrationEndDate()));
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Campinas preview payload for audit", exception);
        }
    }

    private String toIsoDate(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    private String sanitizeText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }
}
