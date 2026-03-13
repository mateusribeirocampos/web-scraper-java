package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.domain.enums.EducationLevel;
import com.campos.webscraper.domain.enums.GovernmentLevel;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.infrastructure.parser.PciConcursosPreviewItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps PCI Concursos preview items into the canonical public contest entity shape used by the project.
 */
public class PciConcursosContestNormalizer {

    private static final Pattern MONEY_VALUE_PATTERN =
            Pattern.compile("(\\d{4,}(?:,\\d{2})?|\\d{1,3}(?:\\.\\d{3})+(?:,\\d{2})?|\\d{1,3},\\d{2}|\\d+)");
    private static final Pattern CURRENCY_SCOPED_PATTERN =
            Pattern.compile("(?i)r\\$\\s*(\\d{4,}(?:,\\d{2})?|\\d{1,3}(?:\\.\\d{3})+(?:,\\d{2})?|\\d{1,3},\\d{2}|\\d+)");

    private final ObjectMapper objectMapper;

    public PciConcursosContestNormalizer() {
        this(new ObjectMapper());
    }

    public PciConcursosContestNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public PublicContestPostingEntity normalize(PciConcursosPreviewItem item, LocalDateTime fetchedAt) {
        Objects.requireNonNull(item, "item must not be null");
        Objects.requireNonNull(fetchedAt, "fetchedAt must not be null");

        LocalDate registrationStartDate = parseDate(item.registrationStartDate());
        LocalDate registrationEndDate = parseDate(item.registrationEndDate());
        LocalDate crawlDate = fetchedAt.toLocalDate();
        String stableContestUrl = item.contestUrl() == null || item.contestUrl().isBlank()
                ? item.detailUrl()
                : item.contestUrl();

        return PublicContestPostingEntity.builder()
                .externalId(stableContestUrl)
                .canonicalUrl(stableContestUrl)
                .contestName(item.contestName())
                .organizer(item.organizer())
                .positionTitle(item.positionTitle())
                .governmentLevel(inferGovernmentLevel(item))
                .educationLevel(resolveEducationLevel(item.educationLevel()))
                .numberOfVacancies(item.numberOfVacancies())
                .baseSalary(parseSalary(item.salaryDescription()))
                .salaryDescription(item.salaryDescription())
                .editalUrl(item.detailUrl())
                .publishedAt(crawlDate)
                .registrationStartDate(registrationStartDate)
                .registrationEndDate(registrationEndDate)
                .contestStatus(resolveContestStatus(crawlDate, registrationEndDate))
                .payloadJson(toJson(item))
                .createdAt(Instant.now())
                .build();
    }

    private GovernmentLevel inferGovernmentLevel(PciConcursosPreviewItem item) {
        String haystack = normalizeText((item.organizer() == null ? "" : item.organizer()) + " "
                + (item.contestName() == null ? "" : item.contestName()));
        if (haystack.contains("universidade federal")
                || haystack.contains("instituto federal")
                || haystack.contains("ministerio")
                || haystack.contains("governo federal")
                || haystack.contains("federal")) {
            return GovernmentLevel.FEDERAL;
        }
        if (haystack.contains("prefeitura") || haystack.contains("municipal")) {
            return GovernmentLevel.MUNICIPAL;
        }
        if (haystack.contains("tribunal de justica do estado")
                || haystack.contains("governo do estado")
                || haystack.contains("estado do ")) {
            return GovernmentLevel.ESTADUAL;
        }
        if (haystack.contains("fundacao") || haystack.contains("autarquia")) {
            return GovernmentLevel.AUTARCHY;
        }
        return GovernmentLevel.AUTARCHY;
    }

    private EducationLevel resolveEducationLevel(String rawEducationLevel) {
        if (rawEducationLevel == null || rawEducationLevel.isBlank()) {
            return EducationLevel.SUPERIOR;
        }
        return EducationLevel.valueOf(rawEducationLevel);
    }

    private LocalDate parseDate(String rawDate) {
        return rawDate == null || rawDate.isBlank() ? null : LocalDate.parse(rawDate);
    }

    private ContestStatus resolveContestStatus(LocalDate crawlDate, LocalDate registrationEndDate) {
        if (registrationEndDate != null && registrationEndDate.isBefore(crawlDate)) {
            return ContestStatus.REGISTRATION_CLOSED;
        }
        return ContestStatus.OPEN;
    }

    private BigDecimal parseSalary(String rawSalary) {
        if (rawSalary == null || rawSalary.isBlank()) {
            return null;
        }
        String firstAmount = firstMoneyToken(rawSalary);
        if (firstAmount == null || firstAmount.isBlank()) {
            return null;
        }
        if (firstAmount.contains(",")) {
            firstAmount = firstAmount.replace(".", "").replace(",", ".");
        } else {
            firstAmount = firstAmount.replace(".", "");
        }
        return firstAmount.isBlank() ? null : new BigDecimal(firstAmount);
    }

    private String firstMoneyToken(String rawSalary) {
        Matcher currencyMatcher = CURRENCY_SCOPED_PATTERN.matcher(rawSalary);
        if (currencyMatcher.find()) {
            return currencyMatcher.group(1);
        }

        Matcher genericMatcher = MONEY_VALUE_PATTERN.matcher(rawSalary);
        return genericMatcher.find() ? genericMatcher.group(1) : null;
    }

    private String toJson(PciConcursosPreviewItem item) {
        try {
            return objectMapper.writeValueAsString(item);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize PCI preview payload for audit", exception);
        }
    }

    private String normalizeText(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
