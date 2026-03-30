package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.domain.enums.EducationLevel;
import com.campos.webscraper.domain.enums.GovernmentLevel;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.infrastructure.parser.MunhozContestAttachment;
import com.campos.webscraper.infrastructure.parser.MunhozContestPreviewItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps Munhoz municipal contest previews into the canonical public contest entity shape.
 */
@Component
public class MunhozContestNormalizer {

    private static final Pattern DURABLE_IDENTIFIER_PATTERN =
            Pattern.compile("(?i)(?:edital|processo seletivo|processos seletivos|concurso|processo de escolha).{0,40}?(\\d+/\\d{4})");

    private final ObjectMapper objectMapper;

    public MunhozContestNormalizer() {
        this(new ObjectMapper().findAndRegisterModules());
    }

    public MunhozContestNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public PublicContestPostingEntity normalize(MunhozContestPreviewItem item, LocalDateTime fetchedAt) {
        Objects.requireNonNull(item, "item must not be null");
        Objects.requireNonNull(fetchedAt, "fetchedAt must not be null");

        if (item.editalUrl() == null || item.editalUrl().isBlank()) {
            throw new IllegalArgumentException("Munhoz contest item must have a stable edital URL");
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

    private String toJson(MunhozContestPreviewItem item) {
        try {
            return objectMapper.writeValueAsString(toPayload(item));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Munhoz preview payload for audit", exception);
        }
    }

    private Map<String, Object> toPayload(MunhozContestPreviewItem item) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("contestTitle", sanitizeText(item.contestTitle()));
        payload.put("organizer", sanitizeText(item.organizer()));
        payload.put("positionTitle", sanitizeText(item.positionTitle()));
        payload.put("educationLevel", sanitizeText(item.educationLevel()));
        payload.put("formationRequirements", sanitizeText(item.formationRequirements()));
        payload.put("contestNumber", sanitizeText(item.contestNumber()));
        payload.put("editalYear", item.editalYear());
        payload.put("contestUrl", sanitizeText(item.contestUrl()));
        payload.put("editalUrl", sanitizeText(item.editalUrl()));
        payload.put("publishedAt", toIsoDate(item.publishedAt()));
        payload.put("registrationStartDate", toIsoDate(item.registrationStartDate()));
        payload.put("registrationEndDate", toIsoDate(item.registrationEndDate()));
        payload.put("examDate", toIsoDate(item.examDate()));
        payload.put("attachments", toAttachmentPayload(item.attachments()));
        payload.put("pdfPositionTitles", sanitizeStrings(item.pdfPositionTitles()));
        payload.put("pdfAnnexReferences", sanitizeStrings(item.pdfAnnexReferences()));
        return payload;
    }

    private String toIsoDate(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    private List<Map<String, String>> toAttachmentPayload(List<MunhozContestAttachment> attachments) {
        if (attachments == null) {
            return List.of();
        }
        return attachments.stream()
                .map(attachment -> Map.of(
                        "type", sanitizeText(attachment.type()),
                        "label", sanitizeText(attachment.label()),
                        "url", sanitizeText(attachment.url())
                ))
                .toList();
    }

    private List<String> sanitizeStrings(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(this::sanitizeText)
                .toList();
    }

    private String sanitizeText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        StringBuilder sanitized = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current) || Character.isLowSurrogate(current)) {
                continue;
            }
            if (Character.isISOControl(current) && current != '\n' && current != '\r' && current != '\t') {
                continue;
            }
            sanitized.append(current);
        }
        return sanitized.toString();
    }

    private String buildStableContestId(MunhozContestPreviewItem item) {
        String durableIdentifier = resolveDurableIdentifier(item);
        String seed = durableIdentifier == null ? item.contestTitle() : durableIdentifier;
        String normalized = Normalizer.normalize(seed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return "municipal_munhoz:" + normalized;
    }

    private String resolveDurableIdentifier(MunhozContestPreviewItem item) {
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
                .map(MunhozContestAttachment::label)
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

    private LocalDate resolvePublishedAt(MunhozContestPreviewItem item, LocalDateTime fetchedAt) {
        if (item.publishedAt() != null) {
            return item.publishedAt();
        }
        if (item.editalYear() != null) {
            return LocalDate.of(item.editalYear(), 1, 1);
        }
        return fetchedAt.toLocalDate();
    }

    private ContestStatus resolveContestStatus(MunhozContestPreviewItem item, LocalDateTime fetchedAt) {
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
