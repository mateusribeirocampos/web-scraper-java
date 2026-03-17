package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.enums.DedupStatus;
import com.campos.webscraper.domain.enums.JobContractType;
import com.campos.webscraper.domain.enums.SeniorityLevel;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.infrastructure.parser.DynamicJobListing;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Normalizes dynamic job listings rendered by Playwright into the canonical entity.
 */
@Component
public class DynamicJobNormalizer {

    private static final String FALLBACK_STACK = "JavaScript,Playwright";

    private final ObjectMapper objectMapper;

    public DynamicJobNormalizer() {
        this(new ObjectMapper());
    }

    public DynamicJobNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public JobPostingEntity normalize(DynamicJobListing listing) {
        Objects.requireNonNull(listing, "listing must not be null");

        JobPostingEntity.JobPostingEntityBuilder builder = JobPostingEntity.builder()
                .externalId(listing.externalId())
                .canonicalUrl(listing.url())
                .title(listing.title())
                .company(listing.company())
                .location(listing.location())
                .remote(listing.remote())
                .contractType(JobContractType.CLT)
                .seniority(SeniorityLevel.MID)
                .techStackTags(FALLBACK_STACK)
                .description(listing.description())
                .publishedAt(LocalDate.parse(listing.postedAt()))
                .payloadJson(toJson(listing))
                .createdAt(Instant.now());

        return builder.build();
    }

    private String toJson(DynamicJobListing listing) {
        try {
            return objectMapper.writeValueAsString(listing);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize dynamic listing", exception);
        }
    }
}
