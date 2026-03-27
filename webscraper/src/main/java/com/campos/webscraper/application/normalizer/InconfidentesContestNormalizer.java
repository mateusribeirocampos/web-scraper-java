package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.domain.enums.EducationLevel;
import com.campos.webscraper.domain.enums.GovernmentLevel;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.infrastructure.parser.InconfidentesContestPreviewItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps Inconfidentes municipal contest previews into the canonical public contest entity shape.
 */
@Component
public class InconfidentesContestNormalizer {

    private static final Pattern DURABLE_IDENTIFIER_PATTERN =
            Pattern.compile("(?i)(?:edital|processo seletivo|concurso)\\s+(\\d+/\\d{4})");

    private final ObjectMapper objectMapper;

    public InconfidentesContestNormalizer() {
        this(new ObjectMapper());
    }

    public InconfidentesContestNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public PublicContestPostingEntity normalize(InconfidentesContestPreviewItem item, LocalDateTime fetchedAt) {
        Objects.requireNonNull(item, "item must not be null");
        Objects.requireNonNull(fetchedAt, "fetchedAt must not be null");

        if (item.editalUrl() == null || item.editalUrl().isBlank()) {
            throw new IllegalArgumentException("Inconfidentes contest item must have a stable edital URL");
        }
        String contestId = buildStableContestId(item);
        String canonicalIdentityUrl = item.contestUrl() != null && !item.contestUrl().isBlank()
                ? item.contestUrl()
                : item.editalUrl();
        return PublicContestPostingEntity.builder()
                .externalId(contestId)
                .canonicalUrl(canonicalIdentityUrl)
                .contestName(item.contestTitle())
                .organizer(item.organizer())
                .positionTitle(item.positionTitle())
                .governmentLevel(GovernmentLevel.MUNICIPAL)
                .state("MG")
                .educationLevel(resolveEducationLevel(item.educationLevel()))
                .editalUrl(item.editalUrl())
                .publishedAt(resolvePublishedAt(item, fetchedAt))
                .contestStatus(resolveContestStatus(item, fetchedAt))
                .payloadJson(toJson(item))
                .createdAt(Instant.now())
                .build();
    }

    private EducationLevel resolveEducationLevel(String rawEducationLevel) {
        if (rawEducationLevel == null || rawEducationLevel.isBlank()) {
            return EducationLevel.UNKNOWN;
        }
        return EducationLevel.valueOf(rawEducationLevel);
    }

    private String toJson(InconfidentesContestPreviewItem item) {
        try {
            return objectMapper.writeValueAsString(item);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Inconfidentes preview payload for audit", exception);
        }
    }

    private String buildStableContestId(InconfidentesContestPreviewItem item) {
        String durableIdentifier = resolveDurableIdentifier(item);
        String seed = durableIdentifier == null
                ? "%s|%s".formatted(item.department() == null ? "" : item.department(), item.contestTitle())
                : "%s|%s".formatted(item.department() == null ? "" : item.department(), durableIdentifier);
        String normalized = Normalizer.normalize(seed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return "municipal_inconfidentes:" + normalized;
    }

    private String resolveDurableIdentifier(InconfidentesContestPreviewItem item) {
        String identifierFromTitle = extractDurableIdentifier(item.contestTitle());
        if (identifierFromTitle != null) {
            return identifierFromTitle;
        }
        if (item.attachments() == null) {
            return null;
        }
        return item.attachments().stream()
                .map(attachment -> extractDurableIdentifier(attachment.label()))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String extractDurableIdentifier(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        Matcher matcher = DURABLE_IDENTIFIER_PATTERN.matcher(rawValue);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    private LocalDate resolvePublishedAt(InconfidentesContestPreviewItem item, LocalDateTime fetchedAt) {
        if (item.editalYear() != null) {
            return LocalDate.of(item.editalYear(), 1, 1);
        }
        return fetchedAt.toLocalDate();
    }

    private ContestStatus resolveContestStatus(InconfidentesContestPreviewItem item, LocalDateTime fetchedAt) {
        String normalizedTitle = normalizeForStatus(item.contestTitle());
        boolean followUpSignal = normalizedTitle.contains("resultado")
                || normalizedTitle.contains("homologacao")
                || normalizedTitle.contains("gabarito");
        if (followUpSignal) {
            return ContestStatus.RESULT_PUBLISHED;
        }

        if (item.editalYear() != null && item.editalYear() < fetchedAt.getYear()) {
            return ContestStatus.REGISTRATION_CLOSED;
        }

        // This municipal page behaves like an archive, but current-year editais are the best available
        // proxy for live contests until PDF parsing extracts explicit registration deadlines.
        return ContestStatus.OPEN;
    }

    private String normalizeForStatus(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
