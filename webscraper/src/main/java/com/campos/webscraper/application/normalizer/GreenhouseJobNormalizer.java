package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.enums.SeniorityLevel;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.interfaces.dto.GreenhouseJobBoardItemResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Maps Greenhouse Job Board payloads into the canonical private-sector job posting shape.
 */
public class GreenhouseJobNormalizer {

    private static final Pattern INTERN_PATTERN = Pattern.compile("\\b(intern|internship)\\b");
    private static final Pattern LEAD_PATTERN = Pattern.compile("\\b(lead|staff|principal)\\b");
    private static final Pattern SENIOR_PATTERN = Pattern.compile("\\b(senior|sr)\\b");
    private static final Pattern JUNIOR_PATTERN = Pattern.compile("\\b(junior|jr)\\b");
    private static final Pattern MID_PATTERN = Pattern.compile("\\b(mid|pleno)\\b");
    private static final Pattern JAVA_PATTERN = Pattern.compile("\\bjava\\b");

    private final ObjectMapper objectMapper;

    public GreenhouseJobNormalizer() {
        this(new ObjectMapper());
    }

    public GreenhouseJobNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public JobPostingEntity normalize(GreenhouseJobBoardItemResponse response) {
        Objects.requireNonNull(response, "response must not be null");

        return JobPostingEntity.builder()
                .externalId(String.valueOf(response.id()))
                .canonicalUrl(response.absoluteUrl())
                .title(response.title())
                .company(response.companyName())
                .location(response.location() == null ? null : response.location().name())
                .remote(isRemote(response))
                .seniority(resolveSeniority(response.title()))
                .techStackTags(resolveTechStackTags(response))
                .description(response.content())
                .publishedAt(OffsetDateTime.parse(response.firstPublished()).toLocalDate())
                .payloadJson(toJson(response))
                .createdAt(Instant.now())
                .build();
    }

    private boolean isRemote(GreenhouseJobBoardItemResponse response) {
        String title = response.title() == null ? "" : response.title().toLowerCase(Locale.ROOT);
        String location = response.location() == null || response.location().name() == null
                ? ""
                : response.location().name().toLowerCase(Locale.ROOT);
        String country = response.location() == null || response.location().country() == null
                ? ""
                : response.location().country().toLowerCase(Locale.ROOT);
        return title.contains("remote")
                || location.contains("remote")
                || location.contains("remoto")
                || location.contains("latam")
                || location.contains("latin america")
                || country.contains("remote");
    }

    private SeniorityLevel resolveSeniority(String title) {
        if (title == null) {
            return null;
        }

        String normalized = title.toLowerCase(Locale.ROOT);
        if (INTERN_PATTERN.matcher(normalized).find()) {
            return SeniorityLevel.INTERN;
        }
        if (LEAD_PATTERN.matcher(normalized).find()) {
            return SeniorityLevel.LEAD;
        }
        if (SENIOR_PATTERN.matcher(normalized).find()) {
            return SeniorityLevel.SENIOR;
        }
        if (JUNIOR_PATTERN.matcher(normalized).find()) {
            return SeniorityLevel.JUNIOR;
        }
        if (MID_PATTERN.matcher(normalized).find()) {
            return SeniorityLevel.MID;
        }
        return null;
    }

    private String resolveTechStackTags(GreenhouseJobBoardItemResponse response) {
        String haystack = ((response.title() == null ? "" : response.title()) + " "
                + (response.content() == null ? "" : response.content()))
                .toLowerCase(Locale.ROOT);

        if (JAVA_PATTERN.matcher(haystack).find()) {
            return "Java";
        }

        return null;
    }

    private String toJson(GreenhouseJobBoardItemResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Greenhouse payload for audit", exception);
        }
    }
}
