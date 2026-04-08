package com.campos.webscraper.application.normalizer;

import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.domain.enums.EducationLevel;
import com.campos.webscraper.domain.enums.GovernmentLevel;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.infrastructure.parser.CamaraItajubaContestAttachment;
import com.campos.webscraper.infrastructure.parser.CamaraItajubaContestPreviewItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CamaraItajubaContestNormalizer {

    private static final Pattern TITLE_YEAR_PATTERN = Pattern.compile("(20\\d{2})");

    private final ObjectMapper objectMapper;

    public CamaraItajubaContestNormalizer() {
        this(new ObjectMapper().findAndRegisterModules());
    }

    CamaraItajubaContestNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public PublicContestPostingEntity normalize(CamaraItajubaContestPreviewItem item, LocalDateTime fetchedAt) {
        Objects.requireNonNull(item, "item must not be null");
        Objects.requireNonNull(fetchedAt, "fetchedAt must not be null");

        if (item.editalUrl() == null || item.editalUrl().isBlank()) {
            throw new IllegalArgumentException("Itajubá Câmara contest item must have a stable edital URL");
        }

        return PublicContestPostingEntity.builder()
                .externalId(buildStableContestId(item))
                .canonicalUrl(item.editalUrl())
                .contestName(item.contestTitle())
                .organizer(item.organizer())
                .positionTitle(item.positionTitle())
                .governmentLevel(GovernmentLevel.MUNICIPAL)
                .state("MG")
                .educationLevel(resolveEducationLevel(item.educationLevel()))
                .numberOfVacancies(item.numberOfVacancies())
                .salaryDescription(item.salaryDescription())
                .editalUrl(item.editalUrl())
                .publishedAt(item.publishedAt() != null ? item.publishedAt() : fetchedAt.toLocalDate())
                .registrationStartDate(item.registrationStartDate())
                .registrationEndDate(item.registrationEndDate())
                .examDate(item.examDate())
                .contestStatus(resolveContestStatus(item, fetchedAt))
                .payloadJson(toJson(item))
                .createdAt(Instant.now())
                .build();
    }

    private String buildStableContestId(CamaraItajubaContestPreviewItem item) {
        return "camara_itajuba:" + resolveDurableContestKey(item);
    }

    private String resolveDurableContestKey(CamaraItajubaContestPreviewItem item) {
        String normalizedTitle = slugify(item.contestTitle());
        Integer titleYear = extractTitleYear(normalizedTitle);
        if (normalizedTitle.contains("concurso-publico") && titleYear != null) {
            return "concurso-publico-" + titleYear;
        }
        if (item.contestNumber() != null && !item.contestNumber().isBlank()) {
            return slugify(item.contestNumber());
        }
        if (titleYear != null) {
            return "concurso-" + titleYear;
        }
        if (item.editalYear() != null) {
            return "concurso-" + item.editalYear();
        }
        return normalizedTitle;
    }

    private Integer extractTitleYear(String normalizedTitle) {
        Matcher matcher = TITLE_YEAR_PATTERN.matcher(normalizedTitle);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private String slugify(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private EducationLevel resolveEducationLevel(String raw) {
        if (raw == null || raw.isBlank()) {
            return EducationLevel.UNKNOWN;
        }
        return EducationLevel.valueOf(raw);
    }

    private ContestStatus resolveContestStatus(CamaraItajubaContestPreviewItem item, LocalDateTime fetchedAt) {
        if (item.registrationEndDate() != null) {
            return item.registrationEndDate().isBefore(fetchedAt.toLocalDate())
                    ? ContestStatus.REGISTRATION_CLOSED
                    : ContestStatus.OPEN;
        }
        if (item.examDate() != null && item.examDate().isBefore(fetchedAt.toLocalDate())) {
            return ContestStatus.REGISTRATION_CLOSED;
        }
        return ContestStatus.OPEN;
    }

    private String toJson(CamaraItajubaContestPreviewItem item) {
        try {
            return objectMapper.writeValueAsString(toPayload(item));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Itajubá Câmara preview payload for audit", exception);
        }
    }

    private Map<String, Object> toPayload(CamaraItajubaContestPreviewItem item) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("contestTitle", sanitizeText(item.contestTitle()));
        payload.put("organizer", sanitizeText(item.organizer()));
        payload.put("positionTitle", sanitizeText(item.positionTitle()));
        payload.put("educationLevel", sanitizeText(item.educationLevel()));
        payload.put("contestNumber", sanitizeText(item.contestNumber()));
        payload.put("editalYear", item.editalYear());
        payload.put("contestUrl", sanitizeText(item.contestUrl()));
        payload.put("editalUrl", sanitizeText(item.editalUrl()));
        payload.put("publishedAt", toIsoDate(item.publishedAt()));
        payload.put("registrationStartDate", toIsoDate(item.registrationStartDate()));
        payload.put("registrationEndDate", toIsoDate(item.registrationEndDate()));
        payload.put("examDate", toIsoDate(item.examDate()));
        payload.put("numberOfVacancies", item.numberOfVacancies());
        payload.put("salaryDescription", sanitizeText(item.salaryDescription()));
        payload.put("attachments", toAttachmentPayload(item.attachments()));
        return payload;
    }

    private String toIsoDate(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    private List<Map<String, String>> toAttachmentPayload(List<CamaraItajubaContestAttachment> attachments) {
        if (attachments == null) {
            return List.of();
        }
        return attachments.stream()
                .map(attachment -> Map.of(
                        "label", sanitizeText(attachment.label()),
                        "url", sanitizeText(attachment.url())
                ))
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
}
