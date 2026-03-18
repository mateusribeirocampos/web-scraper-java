package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.enums.SeniorityLevel;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.interfaces.dto.GupyJobListingResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Maps Gupy Portal API payloads into the canonical private-sector job posting shape.
 */
@Component
public class GupyJobNormalizer {

    private static final Pattern INTERN_PATTERN  = Pattern.compile("\\b(intern|internship|estagio|estagiario)\\b");
    private static final Pattern LEAD_PATTERN    = Pattern.compile("\\b(lead|staff|principal|lider)\\b");
    private static final Pattern SENIOR_PATTERN  = Pattern.compile("\\b(senior|sr)\\b");
    private static final Pattern JUNIOR_PATTERN  = Pattern.compile("\\b(junior|jr)\\b");
    private static final Pattern MID_PATTERN     = Pattern.compile("\\b(mid|pleno|mid-level)\\b");

    private static final Pattern JAVA_PATTERN        = Pattern.compile("\\bjava\\b");
    private static final Pattern SPRING_PATTERN      = Pattern.compile("\\bspring\\b");
    private static final Pattern KOTLIN_PATTERN      = Pattern.compile("\\bkotlin\\b");
    private static final Pattern NODE_PATTERN        = Pattern.compile("\\bnode\\.?js\\b");
    private static final Pattern TYPESCRIPT_PATTERN  = Pattern.compile("\\btypescript\\b");
    private static final Pattern PYTHON_PATTERN      = Pattern.compile("\\bpython\\b");
    private static final Pattern GO_PATTERN          = Pattern.compile("\\b(golang|\\bgo\\b)");
    private static final Pattern REACT_PATTERN       = Pattern.compile("\\breact\\b");
    private static final Pattern AWS_PATTERN         = Pattern.compile("\\baws\\b");
    private static final Pattern POSTGRES_PATTERN    = Pattern.compile("\\b(postgres|postgresql)\\b");
    private static final Pattern KAFKA_PATTERN       = Pattern.compile("\\bkafka\\b");
    private static final Pattern DOCKER_PATTERN      = Pattern.compile("\\bdocker\\b");

    private final ObjectMapper objectMapper;

    public GupyJobNormalizer() {
        this(new ObjectMapper());
    }

    public GupyJobNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public JobPostingEntity normalize(GupyJobListingResponse response) {
        Objects.requireNonNull(response, "response must not be null");

        return JobPostingEntity.builder()
                .externalId(String.valueOf(response.id()))
                .canonicalUrl(response.jobUrl())
                .title(response.name())
                .company(response.careerPageName())
                .location(resolveLocation(response))
                .remote(resolveRemote(response))
                .seniority(resolveSeniority(response.name()))
                .techStackTags(resolveTechStackTags(response))
                .description(response.description())
                .publishedAt(resolvePublishedAt(response.publishedDate()))
                .applicationDeadline(resolveDeadline(response.applicationDeadline()))
                .payloadJson(toJson(response))
                .createdAt(Instant.now())
                .build();
    }

    private String resolveLocation(GupyJobListingResponse response) {
        String city  = response.city()  == null ? "" : response.city().strip();
        String state = response.state() == null ? "" : response.state().strip();
        if (city.isEmpty() && state.isEmpty()) {
            return response.country();
        }
        if (city.isEmpty()) {
            return state + ", " + response.country();
        }
        if (state.isEmpty()) {
            return city + ", " + response.country();
        }
        return city + ", " + state;
    }

    private boolean resolveRemote(GupyJobListingResponse response) {
        if (response.isRemoteWork()) {
            return true;
        }
        String type = response.workplaceType() == null ? "" : response.workplaceType().toLowerCase(Locale.ROOT);
        return type.contains("remote");
    }

    private LocalDate resolvePublishedAt(String publishedDate) {
        if (publishedDate == null || publishedDate.isBlank()) {
            return LocalDate.now();
        }
        try {
            return OffsetDateTime.parse(publishedDate).toLocalDate();
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private LocalDate resolveDeadline(String deadline) {
        if (deadline == null || deadline.isBlank()) {
            return null;
        }
        try {
            LocalDate date = OffsetDateTime.parse(deadline).toLocalDate();
            // Gupy uses "2035-12-31" as a placeholder for "no deadline"
            if (date.getYear() > 2030) {
                return null;
            }
            return date;
        } catch (Exception e) {
            return null;
        }
    }

    private SeniorityLevel resolveSeniority(String title) {
        if (title == null) {
            return null;
        }
        String normalized = stripAccents(title.toLowerCase(Locale.ROOT));
        if (INTERN_PATTERN.matcher(normalized).find())  return SeniorityLevel.INTERN;
        if (LEAD_PATTERN.matcher(normalized).find())    return SeniorityLevel.LEAD;
        if (SENIOR_PATTERN.matcher(normalized).find())  return SeniorityLevel.SENIOR;
        if (JUNIOR_PATTERN.matcher(normalized).find())  return SeniorityLevel.JUNIOR;
        if (MID_PATTERN.matcher(normalized).find())     return SeniorityLevel.MID;
        return null;
    }

    private String resolveTechStackTags(GupyJobListingResponse response) {
        String haystack = stripAccents(
                ((response.name()        == null ? "" : response.name()) + " "
                + (response.description() == null ? "" : response.description()))
                .toLowerCase(Locale.ROOT));

        List<String> tags = new ArrayList<>();
        if (JAVA_PATTERN.matcher(haystack).find())       tags.add("Java");
        if (SPRING_PATTERN.matcher(haystack).find())     tags.add("Spring");
        if (KOTLIN_PATTERN.matcher(haystack).find())     tags.add("Kotlin");
        if (NODE_PATTERN.matcher(haystack).find())       tags.add("Node.js");
        if (TYPESCRIPT_PATTERN.matcher(haystack).find()) tags.add("TypeScript");
        if (PYTHON_PATTERN.matcher(haystack).find())     tags.add("Python");
        if (GO_PATTERN.matcher(haystack).find())         tags.add("Go");
        if (REACT_PATTERN.matcher(haystack).find())      tags.add("React");
        if (AWS_PATTERN.matcher(haystack).find())        tags.add("AWS");
        if (POSTGRES_PATTERN.matcher(haystack).find())   tags.add("PostgreSQL");
        if (KAFKA_PATTERN.matcher(haystack).find())      tags.add("Kafka");
        if (DOCKER_PATTERN.matcher(haystack).find())     tags.add("Docker");

        return tags.isEmpty() ? null : String.join(",", tags);
    }

    private static String stripAccents(String input) {
        String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}", "");
    }

    private String toJson(GupyJobListingResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Gupy payload for audit", exception);
        }
    }
}
