package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.enums.SeniorityLevel;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.interfaces.dto.IndeedApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

/**
 * Maps Indeed MCP payloads into the canonical job posting entity shape used by the project.
 */
public class IndeedJobNormalizer {

    private static final String DEFAULT_TECH_STACK = "Java,Spring Boot";

    private final ObjectMapper objectMapper;

    public IndeedJobNormalizer() {
        this(new ObjectMapper());
    }

    public IndeedJobNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Normalizes an Indeed API response to the canonical job posting entity.
     */
    public JobPostingEntity normalize(IndeedApiResponse response) {
        Objects.requireNonNull(response, "response must not be null");

        return JobPostingEntity.builder()
                .externalId(response.jobId())
                .canonicalUrl(response.applyUrl())
                .title(response.title())
                .company(response.company())
                .location(response.location())
                .remote(isRemote(response))
                .seniority(SeniorityLevel.JUNIOR)
                .techStackTags(DEFAULT_TECH_STACK)
                .publishedAt(LocalDate.parse(response.postedAt()))
                .payloadJson(toJson(response))
                .createdAt(Instant.now())
                .build();
    }

    private boolean isRemote(IndeedApiResponse response) {
        String title = response.title() == null ? "" : response.title().toLowerCase(Locale.ROOT);
        String location = response.location() == null ? "" : response.location().toLowerCase(Locale.ROOT);
        return title.contains("remote") || location.contains("remoto") || location.contains("remote");
    }

    private String toJson(IndeedApiResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Indeed payload for audit", exception);
        }
    }
}
