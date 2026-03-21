package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.enums.SeniorityLevel;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.interfaces.dto.GreenhouseJobBoardItemResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Maps Greenhouse Job Board payloads into the canonical private-sector job posting shape.
 */
@Component
public class GreenhouseJobNormalizer {

    private static final Pattern INTERN_PATTERN  = Pattern.compile("\\b(intern|internship|estagio|estagiario)\\b");
    private static final Pattern LEAD_PATTERN    = Pattern.compile("\\b(lead|staff|principal|lider)\\b");
    private static final Pattern SENIOR_PATTERN  = Pattern.compile("\\b(senior|sr|sênior|senior)\\b");
    private static final Pattern JUNIOR_PATTERN  = Pattern.compile("\\b(junior|jr|junior)\\b");
    private static final Pattern MID_PATTERN     = Pattern.compile("\\b(mid|pleno|mid-level)\\b");

    // Tech stack detection patterns (title + description)
    private static final Pattern JAVA_PATTERN        = Pattern.compile("\\bjava\\b");
    private static final Pattern SPRING_PATTERN      = Pattern.compile("\\bspring\\b");
    private static final Pattern KOTLIN_PATTERN      = Pattern.compile("\\bkotlin\\b");
    private static final Pattern NODE_PATTERN        = Pattern.compile("\\bnode\\.?js\\b");
    private static final Pattern TYPESCRIPT_PATTERN  = Pattern.compile("\\btypescript\\b");
    private static final Pattern PYTHON_PATTERN      = Pattern.compile("\\bpython\\b");
    private static final Pattern PYTHON_TECH_TITLE_PATTERN = Pattern.compile(
            "\\b(engineer|developer|software|backend|frontend|fullstack|platform|security|infrastructure|devops|sre|scientist|ml|machine learning)\\b"
    );
    private static final Pattern PYTHON_NON_TECH_TITLE_PATTERN = Pattern.compile(
            "\\b(specialist|supervisor|controllership|operations|ops|partnership|projects?|revenue|business|finance|product operations)\\b"
    );
    private static final Pattern PYTHON_TECH_CONTEXT_PATTERN = Pattern.compile(
            "\\bpython\\b.{0,40}\\b(programming|code|coding|script|scripting|automation|api|backend|service|services|application|applications|data models?|machine learning|ml|pandas|django|flask)\\b"
            + "|\\b(programming|code|coding|script|scripting|automation|api|backend|service|services|application|applications|data models?|machine learning|ml|pandas|django|flask)\\b.{0,40}\\bpython\\b"
    );
    private static final Pattern GO_PATTERN          = Pattern.compile(
            "\\bgolang\\b"
            + "|\\bgo\\s+(developer|engineer|lang|language|microservices?|services?|backend|api|apis|programming|code|coding)\\b"
            + "|\\b(developer|engineer|backend|software|programming|code|coding|service|services)\\b.{0,40}\\bgo\\b"
    );
    private static final Pattern REACT_PATTERN       = Pattern.compile("\\breact\\b");
    private static final Pattern AWS_PATTERN         = Pattern.compile("\\baws\\b");
    private static final Pattern POSTGRES_PATTERN    = Pattern.compile("\\b(postgres|postgresql)\\b");
    private static final Pattern KAFKA_PATTERN       = Pattern.compile("\\bkafka\\b");
    private static final Pattern DOCKER_PATTERN      = Pattern.compile("\\bdocker\\b");

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

        // Remove accents so "Júnior" → "junior", "Sênior" → "senior", etc.
        String normalized = stripAccents(title.toLowerCase(Locale.ROOT));
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
        String normalizedTitle = stripAccents((response.title() == null ? "" : response.title()).toLowerCase(Locale.ROOT));
        String haystack = stripAccents(
                ((response.title() == null ? "" : response.title()) + " "
                + (response.content() == null ? "" : response.content()))
                .toLowerCase(Locale.ROOT));

        List<String> tags = new ArrayList<>();
        if (JAVA_PATTERN.matcher(haystack).find())       tags.add("Java");
        if (SPRING_PATTERN.matcher(haystack).find())     tags.add("Spring");
        if (KOTLIN_PATTERN.matcher(haystack).find())     tags.add("Kotlin");
        if (NODE_PATTERN.matcher(haystack).find())       tags.add("Node.js");
        if (TYPESCRIPT_PATTERN.matcher(haystack).find()) tags.add("TypeScript");
        if (shouldTagPython(normalizedTitle, haystack))  tags.add("Python");
        if (GO_PATTERN.matcher(haystack).find())         tags.add("Go");
        if (REACT_PATTERN.matcher(haystack).find())      tags.add("React");
        if (AWS_PATTERN.matcher(haystack).find())        tags.add("AWS");
        if (POSTGRES_PATTERN.matcher(haystack).find())   tags.add("PostgreSQL");
        if (KAFKA_PATTERN.matcher(haystack).find())      tags.add("Kafka");
        if (DOCKER_PATTERN.matcher(haystack).find())     tags.add("Docker");

        return tags.isEmpty() ? null : String.join(",", tags);
    }

    private boolean shouldTagPython(String normalizedTitle, String haystack) {
        if (!PYTHON_PATTERN.matcher(haystack).find()) {
            return false;
        }

        if (PYTHON_TECH_TITLE_PATTERN.matcher(normalizedTitle).find()) {
            return true;
        }

        return !PYTHON_NON_TECH_TITLE_PATTERN.matcher(normalizedTitle).find()
                && PYTHON_TECH_CONTEXT_PATTERN.matcher(haystack).find();
    }

    /** Removes diacritical marks: "júnior" → "junior", "sênior" → "senior". */
    private static String stripAccents(String input) {
        String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}", "");
    }

    private String toJson(GreenhouseJobBoardItemResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Greenhouse payload for audit", exception);
        }
    }
}
