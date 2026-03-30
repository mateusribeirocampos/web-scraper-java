package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.domain.enums.EducationLevel;
import com.campos.webscraper.domain.enums.GovernmentLevel;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.infrastructure.parser.PousoAlegreContestAttachment;
import com.campos.webscraper.infrastructure.parser.PousoAlegreContestPreviewItem;
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
 * Maps Pouso Alegre municipal contest previews into the canonical public contest entity shape.
 */
@Component
public class PousoAlegreContestNormalizer {

    private static final Pattern DURABLE_IDENTIFIER_PATTERN =
            Pattern.compile("(?i)(?:edital|processo seletivo|concurso).{0,40}?(\\d+/\\d{4})");

    private final ObjectMapper objectMapper;

    public PousoAlegreContestNormalizer() {
        this(new ObjectMapper().findAndRegisterModules());
    }

    public PousoAlegreContestNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public PublicContestPostingEntity normalize(PousoAlegreContestPreviewItem item, LocalDateTime fetchedAt) {
        Objects.requireNonNull(item, "item must not be null");
        Objects.requireNonNull(fetchedAt, "fetchedAt must not be null");

        if (item.editalUrl() == null || item.editalUrl().isBlank()) {
            throw new IllegalArgumentException("Pouso Alegre contest item must have a stable edital URL");
        }

        return PublicContestPostingEntity.builder()
                .externalId(buildStableContestId(item))
                .canonicalUrl(item.contestUrl() != null && !item.contestUrl().isBlank() ? item.contestUrl() : item.editalUrl())
                .contestName(item.contestTitle())
                .organizer(item.organizer())
                .positionTitle(item.positionTitle())
                .governmentLevel(GovernmentLevel.MUNICIPAL)
                .state("MG")
                .educationLevel(resolveEducationLevel(item.educationLevel()))
                .editalUrl(item.editalUrl())
                .publishedAt(resolvePublishedAt(item, fetchedAt))
                .registrationStartDate(item.registrationStartDate())
                .registrationEndDate(item.registrationEndDate())
                .examDate(item.examDate())
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

    private String toJson(PousoAlegreContestPreviewItem item) {
        try {
            return objectMapper.writeValueAsString(item);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Pouso Alegre preview payload for audit", exception);
        }
    }

    private String buildStableContestId(PousoAlegreContestPreviewItem item) {
        String durableIdentifier = resolveDurableIdentifier(item);
        String seed = durableIdentifier == null ? item.contestTitle() : durableIdentifier;
        String normalized = Normalizer.normalize(seed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return "municipal_pouso_alegre:" + normalized;
    }

    private String resolveDurableIdentifier(PousoAlegreContestPreviewItem item) {
        if (item.contestNumber() != null && !item.contestNumber().isBlank()) {
            return item.contestNumber();
        }
        String identifierFromTitle = extractDurableIdentifier(item.contestTitle());
        if (identifierFromTitle != null) {
            return identifierFromTitle;
        }
        if (item.attachments() == null) {
            return null;
        }
        return item.attachments().stream()
                .map(PousoAlegreContestAttachment::label)
                .map(this::extractDurableIdentifier)
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
        return normalizeContestIdentifier(matcher.group(1));
    }

    private String normalizeContestIdentifier(String rawIdentifier) {
        Matcher matcher = Pattern.compile("(\\d{1,4})/(\\d{4})").matcher(rawIdentifier);
        if (!matcher.find()) {
            return rawIdentifier;
        }
        int contestNumber = Integer.parseInt(matcher.group(1));
        return "%03d/%s".formatted(contestNumber, matcher.group(2));
    }

    private LocalDate resolvePublishedAt(PousoAlegreContestPreviewItem item, LocalDateTime fetchedAt) {
        if (item.publishedAt() != null) {
            return item.publishedAt();
        }
        if (item.editalYear() != null) {
            return LocalDate.of(item.editalYear(), 1, 1);
        }
        return fetchedAt.toLocalDate();
    }

    private ContestStatus resolveContestStatus(PousoAlegreContestPreviewItem item, LocalDateTime fetchedAt) {
        if (item.registrationEndDate() != null) {
            if (!item.registrationEndDate().isBefore(fetchedAt.toLocalDate())) {
                return ContestStatus.OPEN;
            }
            if (item.examDate() != null && !item.examDate().isBefore(fetchedAt.toLocalDate())) {
                return ContestStatus.EXAM_SCHEDULED;
            }
            return ContestStatus.REGISTRATION_CLOSED;
        }

        if (item.editalYear() != null && item.editalYear() < fetchedAt.getYear()) {
            return ContestStatus.REGISTRATION_CLOSED;
        }

        return ContestStatus.OPEN;
    }
}
