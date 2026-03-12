package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.domain.enums.GovernmentLevel;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.interfaces.dto.DouApiItemResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Maps DOU API items into the canonical public contest entity shape used by the project.
 */
public class DouContestNormalizer {

    private final ObjectMapper objectMapper;

    public DouContestNormalizer() {
        this(new ObjectMapper());
    }

    public DouContestNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Normalizes a DOU API item to the canonical public contest entity.
     */
    public PublicContestPostingEntity normalize(DouApiItemResponse response) {
        Objects.requireNonNull(response, "response must not be null");

        return PublicContestPostingEntity.builder()
                .externalId(response.id())
                .canonicalUrl(response.detailUrl())
                .contestName(response.title())
                .organizer("DOU")
                .positionTitle(response.title())
                .governmentLevel(GovernmentLevel.FEDERAL)
                .editalUrl(response.detailUrl())
                .publishedAt(LocalDate.parse(response.publishedAt()))
                .contestStatus(ContestStatus.OPEN)
                .payloadJson(toJson(response))
                .createdAt(Instant.now())
                .build();
    }

    private String toJson(DouApiItemResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize DOU payload for audit", exception);
        }
    }
}
