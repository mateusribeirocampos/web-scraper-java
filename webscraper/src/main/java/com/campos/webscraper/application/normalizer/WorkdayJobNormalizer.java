package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.enums.JobContractType;
import com.campos.webscraper.domain.enums.SeniorityLevel;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.interfaces.dto.WorkdayJobPostingResponse;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps Workday job postings into the canonical private-sector job posting shape.
 */
@Component
public class WorkdayJobNormalizer {

    private static final Map<String, String> BOARD_ROOTS = Map.of(
            "airbus_helibras_workday", "https://ag.wd3.myworkdayjobs.com/en-US/Airbus",
            "alcoa_pocos_caldas_workday", "https://alcoa.wd5.myworkdayjobs.com/en-US/Careers"
    );
    private static final Map<String, String> BOARD_COMPANY_ALIASES = Map.of(
            "airbus_helibras_workday", "Helibras / Airbus",
            "alcoa_pocos_caldas_workday", "Alcoa"
    );

    private static final Pattern INTERN_PATTERN = Pattern.compile("\\b(intern|internship|estagio|estagiario)\\b");
    private static final Pattern LEAD_PATTERN = Pattern.compile("\\b(lead|staff|principal|lider)\\b");
    private static final Pattern SENIOR_PATTERN = Pattern.compile("\\b(senior|sr|sênior|senior)\\b");
    private static final Pattern JUNIOR_PATTERN = Pattern.compile("\\b(junior|jr|junior)\\b");
    private static final Pattern MID_PATTERN = Pattern.compile("\\b(mid|pleno|mid-level|pl)\\b");
    private static final Pattern SAP_PATTERN = Pattern.compile("\\bsap\\b");
    private static final Pattern SECURITY_PATTERN = Pattern.compile("\\bsecurity|seguranca\\b");
    private static final Pattern ENGINEERING_PATTERN = Pattern.compile("\\bengineering|engenharia\\b");
    private static final Pattern DAYS_AGO_PATTERN = Pattern.compile("posted\\s+(\\d+)\\s+days?\\s+ago");

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public WorkdayJobNormalizer() {
        this(new ObjectMapper(), Clock.systemUTC());
    }

    public WorkdayJobNormalizer(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public JobPostingEntity normalize(String siteCode, WorkdayJobPostingResponse response) {
        Objects.requireNonNull(siteCode, "siteCode must not be null");
        Objects.requireNonNull(response, "response must not be null");

        String canonicalUrl = resolveCanonicalUrl(siteCode, response.externalPath());

        return JobPostingEntity.builder()
                .externalId(resolveExternalId(response, canonicalUrl))
                .canonicalUrl(canonicalUrl)
                .title(response.title())
                .company(BOARD_COMPANY_ALIASES.getOrDefault(siteCode, "Unknown"))
                .location(response.locationsText())
                .remote(false)
                .contractType(resolveContractType(response.title()))
                .seniority(resolveSeniority(response.title()))
                .techStackTags(resolveTechStackTags(response.title()))
                .description(null)
                .publishedAt(resolvePublishedAt(response.postedOn()))
                .payloadJson(toJson(response))
                .createdAt(Instant.now(clock))
                .build();
    }

    private String resolveCanonicalUrl(String siteCode, String externalPath) {
        String boardRoot = BOARD_ROOTS.get(siteCode);
        if (boardRoot == null) {
            throw new IllegalStateException("No Workday board root mapped for site: " + siteCode);
        }
        if (externalPath == null || externalPath.isBlank()) {
            return boardRoot;
        }
        return boardRoot + externalPath;
    }

    private String resolveExternalId(WorkdayJobPostingResponse response, String canonicalUrl) {
        if (response.bulletFields() != null) {
            for (String bulletField : response.bulletFields()) {
                if (bulletField != null && !bulletField.isBlank()) {
                    return bulletField.trim();
                }
            }
        }
        return canonicalUrl;
    }

    private JobContractType resolveContractType(String title) {
        String normalized = normalizeText(title);
        if (INTERN_PATTERN.matcher(normalized).find()) {
            return JobContractType.INTERNSHIP;
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

    private String resolveTechStackTags(String title) {
        String normalized = normalizeText(title);
        List<String> tags = new ArrayList<>();
        if (SAP_PATTERN.matcher(normalized).find()) tags.add("SAP");
        if (SECURITY_PATTERN.matcher(normalized).find()) tags.add("Security");
        if (ENGINEERING_PATTERN.matcher(normalized).find()) tags.add("Engineering");
        return tags.isEmpty() ? null : String.join(",", tags);
    }

    private LocalDate resolvePublishedAt(String postedOn) {
        LocalDate today = LocalDate.now(clock);
        String normalized = normalizeText(postedOn);
        if (normalized.contains("posted today")) {
            return today;
        }
        if (normalized.contains("posted yesterday")) {
            return today.minusDays(1);
        }
        if (normalized.contains("30+ days ago")) {
            return today.minusDays(30);
        }
        Matcher matcher = DAYS_AGO_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return today.minusDays(Long.parseLong(matcher.group(1)));
        }
        return today;
    }

    private String toJson(WorkdayJobPostingResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Workday payload for audit", exception);
        }
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}", "");
    }
}
