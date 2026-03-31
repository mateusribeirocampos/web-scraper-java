package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.enums.JobContractType;
import com.campos.webscraper.domain.enums.SeniorityLevel;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.interfaces.dto.LeverPostingResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Maps Lever postings into the canonical private-sector job posting shape.
 */
@Component
public class LeverJobNormalizer {

    private static final Map<String, String> BOARD_COMPANY_ALIASES = Map.of(
            "ciandt", "CI&T"
    );

    private static final Pattern INTERN_PATTERN  = Pattern.compile("\\b(intern|internship|estagio|estagiario)\\b");
    private static final Pattern LEAD_PATTERN    = Pattern.compile("\\b(lead|staff|principal|lider)\\b");
    private static final Pattern SENIOR_PATTERN  = Pattern.compile("\\b(senior|sr|sênior|senior)\\b");
    private static final Pattern JUNIOR_PATTERN  = Pattern.compile("\\b(junior|jr|junior)\\b");
    private static final Pattern MID_PATTERN     = Pattern.compile("\\b(mid|pleno|mid-level)\\b");
    private static final Pattern JAVA_PATTERN    = Pattern.compile("\\bjava\\b");
    private static final Pattern NODE_PATTERN    = Pattern.compile("\\bnode\\.?js\\b");
    private static final Pattern REACT_PATTERN   = Pattern.compile("\\breact\\b");
    private static final Pattern TYPESCRIPT_PATTERN = Pattern.compile("\\btypescript\\b");
    private static final Pattern PYTHON_PATTERN  = Pattern.compile("\\bpython\\b");
    private static final Pattern AWS_PATTERN     = Pattern.compile("\\baws\\b");

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public LeverJobNormalizer() {
        this(new ObjectMapper(), Clock.systemUTC());
    }

    public LeverJobNormalizer(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public JobPostingEntity normalize(LeverPostingResponse response) {
        Objects.requireNonNull(response, "response must not be null");
        String canonicalUrl = resolveCanonicalUrl(response);

        return JobPostingEntity.builder()
                .externalId(firstNonBlank(response.id(), canonicalUrl))
                .canonicalUrl(canonicalUrl)
                .title(response.text())
                .company(resolveCompany(response, canonicalUrl))
                .location(response.categories() == null ? null : response.categories().location())
                .remote(isRemote(response))
                .contractType(resolveContractType(response))
                .seniority(resolveSeniority(response.text()))
                .techStackTags(resolveTechStackTags(response))
                .description(response.description())
                .publishedAt(null)
                .payloadJson(toJson(response))
                .createdAt(Instant.now(clock))
                .build();
    }

    private String resolveCompany(LeverPostingResponse response, String canonicalUrl) {
        String boardSlug = extractBoardSlug(normalizeBlank(canonicalUrl));
        if (boardSlug == null) {
            boardSlug = extractBoardSlug(normalizeBlank(response.applyUrl()));
        }
        if (boardSlug != null) {
            return BOARD_COMPANY_ALIASES.getOrDefault(boardSlug, humanizeBoardSlug(boardSlug));
        }
        return "Unknown";
    }

    private String resolveCanonicalUrl(LeverPostingResponse response) {
        String hostedUrl = normalizeBlank(response.hostedUrl());
        if (hostedUrl != null) {
            return hostedUrl;
        }

        String applyUrl = normalizeBlank(response.applyUrl());
        if (applyUrl == null) {
            return null;
        }

        String withoutQuery = applyUrl.replaceFirst("\\?.*$", "");
        return withoutQuery.replaceFirst("/apply/?$", "");
    }

    private boolean isRemote(LeverPostingResponse response) {
        String workplaceType = normalizeText(response.workplaceType());
        String title = normalizeText(response.text());
        String location = response.categories() == null ? "" : normalizeText(response.categories().location());
        return workplaceType.contains("remote")
                || title.contains("remote")
                || title.contains("remoto")
                || location.contains("remote")
                || location.contains("remoto");
    }

    private JobContractType resolveContractType(LeverPostingResponse response) {
        String commitment = response.categories() == null ? "" : normalizeText(response.categories().commitment());
        if (commitment.contains("intern")) {
            return JobContractType.INTERNSHIP;
        }
        if (commitment.contains("temporary")) {
            return JobContractType.TEMPORARY;
        }
        if (commitment.contains("contract")) {
            return JobContractType.PJ;
        }
        return JobContractType.UNKNOWN;
    }

    private SeniorityLevel resolveSeniority(String title) {
        String normalized = normalizeText(title);
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

    private String resolveTechStackTags(LeverPostingResponse response) {
        String haystack = normalizeText((response.text() == null ? "" : response.text()) + " "
                + (response.description() == null ? "" : response.description()));
        List<String> tags = new ArrayList<>();
        if (JAVA_PATTERN.matcher(haystack).find())       tags.add("Java");
        if (NODE_PATTERN.matcher(haystack).find())       tags.add("Node.js");
        if (REACT_PATTERN.matcher(haystack).find())      tags.add("React");
        if (TYPESCRIPT_PATTERN.matcher(haystack).find()) tags.add("TypeScript");
        if (PYTHON_PATTERN.matcher(haystack).find())     tags.add("Python");
        if (AWS_PATTERN.matcher(haystack).find())        tags.add("AWS");
        return tags.isEmpty() ? null : String.join(",", tags);
    }

    private String toJson(LeverPostingResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Lever payload for audit", exception);
        }
    }

    private static String firstNonBlank(String preferred, String fallback) {
        String normalizedPreferred = normalizeBlank(preferred);
        if (normalizedPreferred != null) {
            return normalizedPreferred;
        }
        return normalizeBlank(fallback);
    }

    private static String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}", "");
    }

    private static String extractBoardSlug(String canonicalUrl) {
        if (canonicalUrl == null) {
            return null;
        }
        String normalized = canonicalUrl.replaceFirst("^https?://", "");
        String[] parts = normalized.split("/");
        if (parts.length < 2) {
            return null;
        }
        if (!parts[0].contains("jobs.lever.co")) {
            return null;
        }
        return normalizeBlank(parts[1]);
    }

    private static String humanizeBoardSlug(String boardSlug) {
        String[] words = boardSlug.split("[-_]");
        List<String> titleCased = new ArrayList<>();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            titleCased.add(word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1));
        }
        return titleCased.isEmpty() ? "Unknown" : String.join(" ", titleCased);
    }
}
