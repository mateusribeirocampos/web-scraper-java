package com.campos.webscraper.infrastructure.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for the official Campinas contests JSONAPI site node.
 */
@Component
public class CampinasConcursosParser {

    static final String OFFICIAL_SITE_URL = "https://campinas.sp.gov.br/sites/concursos/";
    private static final URI OFFICIAL_SITE_URI = URI.create(OFFICIAL_SITE_URL);
    private static final ZoneId OFFICIAL_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final String ORGANIZER = "Prefeitura Municipal de Campinas";
    private static final Pattern POSITION_PATTERN = Pattern.compile(
            "(?i)para\\s+(.+?)(?:\\s+de\\s+\\d{2}/\\d{2}/\\d{2}\\s+a\\s+\\d{2}/\\d{2}/\\d{2}|$)"
    );
    private static final Pattern REGISTRATION_WINDOW_PATTERN = Pattern.compile(
            "(?i)\\bde\\s+(\\d{2}/\\d{2}/\\d{2})\\s+a\\s+(\\d{2}/\\d{2}/\\d{2})\\b"
    );
    private static final Pattern LINK_CODE_PATTERN = Pattern.compile("([A-Z]{2,}\\d{3,})/?$");

    private final ObjectMapper objectMapper;

    public CampinasConcursosParser() {
        this(new ObjectMapper().findAndRegisterModules());
    }

    CampinasConcursosParser(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public List<CampinasContestPreviewItem> parse(String json, String sourceUrl, LocalDateTime fetchedAt) {
        Objects.requireNonNull(json, "json must not be null");
        Objects.requireNonNull(sourceUrl, "sourceUrl must not be null");
        Objects.requireNonNull(fetchedAt, "fetchedAt must not be null");

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode firstItem = root.path("data").isArray() && !root.path("data").isEmpty()
                    ? root.path("data").get(0)
                    : null;
            if (firstItem == null || firstItem.isMissingNode()) {
                return List.of();
            }

            JsonNode attributes = firstItem.path("attributes");
            if (!attributes.path("field_blc_exibir_alerta").asBoolean(false)) {
                return List.of();
            }

            String alertTitle = text(attributes, "field_site_alerta_titulo");
            String alertLink = link(attributes.path("field_site_alerta_link"));
            if (isBlank(alertTitle) || isBlank(alertLink)) {
                return List.of();
            }

            String displayStartRaw = attributes.path("field_site_alerta_exibicao").path("value").asText(null);
            String displayEndRaw = attributes.path("field_site_alerta_exibicao").path("end_value").asText(null);
            if (!isDisplayWindowActive(displayStartRaw, displayEndRaw, fetchedAt)) {
                return List.of();
            }

            LocalDate publishedAt = firstNonNull(
                    parseDate(attributes.path("field_dt_publish_on").asText(null)),
                    parseDate(displayStartRaw),
                    fetchedAt.toLocalDate()
            );
            RegistrationWindow registrationWindow = inferRegistrationWindow(alertTitle);

            CampinasContestPreviewItem item = new CampinasContestPreviewItem(
                    alertTitle,
                    ORGANIZER,
                    inferPositionTitle(alertTitle),
                    extractContestCode(alertLink, alertTitle),
                    OFFICIAL_SITE_URL,
                    sourceUrl,
                    alertLink,
                    publishedAt,
                    registrationWindow.startDate(),
                    registrationWindow.endDate()
            );
            return List.of(item);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to parse Campinas concursos JSON payload", exception);
        }
    }

    private boolean isDisplayWindowActive(String startRaw, String endRaw, LocalDateTime fetchedAt) {
        OffsetDateTime fetchedAtOffset = fetchedAt.atZone(ZoneId.systemDefault()).toOffsetDateTime();
        OffsetDateTime start = parseOffsetDateTime(startRaw);
        OffsetDateTime end = parseOffsetDateTime(endRaw);
        if (start != null && fetchedAtOffset.isBefore(start)) {
            return false;
        }
        return end == null || !fetchedAtOffset.isAfter(end);
    }

    private String inferPositionTitle(String alertTitle) {
        if (isBlank(alertTitle)) {
            return null;
        }
        Matcher matcher = POSITION_PATTERN.matcher(alertTitle);
        if (!matcher.find()) {
            return null;
        }
        String positionTitle = matcher.group(1).replaceAll("\\s+", " ").trim();
        return positionTitle.isBlank() ? null : positionTitle;
    }

    private String extractContestCode(String alertLink, String alertTitle) {
        if (!isBlank(alertLink)) {
            Matcher matcher = LINK_CODE_PATTERN.matcher(alertLink.trim());
            if (matcher.find()) {
                return matcher.group(1).toUpperCase(Locale.ROOT);
            }
            String stableLinkIdentity = extractStableLinkIdentity(alertLink);
            if (!isBlank(stableLinkIdentity)) {
                return stableLinkIdentity;
            }
        }
        return slugify(removeRegistrationWindow(alertTitle));
    }

    private RegistrationWindow inferRegistrationWindow(String alertTitle) {
        if (isBlank(alertTitle)) {
            return RegistrationWindow.empty();
        }
        Matcher matcher = REGISTRATION_WINDOW_PATTERN.matcher(alertTitle);
        if (!matcher.find()) {
            return RegistrationWindow.empty();
        }
        LocalDate startDate = parseBrazilianShortDate(matcher.group(1));
        LocalDate endDate = parseBrazilianShortDate(matcher.group(2));
        if (startDate == null || endDate == null) {
            return RegistrationWindow.empty();
        }
        return new RegistrationWindow(startDate, endDate);
    }

    private String slugify(String rawValue) {
        if (isBlank(rawValue)) {
            return "campinas-alerta";
        }
        return Normalizer.normalize(rawValue, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private LocalDate parseDate(String rawValue) {
        OffsetDateTime offsetDateTime = parseOffsetDateTime(rawValue);
        if (offsetDateTime == null) {
            return null;
        }
        return offsetDateTime.atZoneSameInstant(OFFICIAL_ZONE).toLocalDate();
    }

    private OffsetDateTime parseOffsetDateTime(String rawValue) {
        if (isBlank(rawValue)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(rawValue);
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDate parseBrazilianShortDate(String rawValue) {
        if (isBlank(rawValue)) {
            return null;
        }
        try {
            String[] parts = rawValue.trim().split("/");
            if (parts.length != 3) {
                return null;
            }
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = 2000 + Integer.parseInt(parts[2]);
            return LocalDate.of(year, month, day);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String text(JsonNode attributes, String fieldName) {
        JsonNode fieldNode = attributes.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return null;
        }
        String value = fieldNode.asText(null);
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String link(JsonNode fieldNode) {
        if (fieldNode == null || fieldNode.isMissingNode() || fieldNode.isNull()) {
            return null;
        }
        if (fieldNode.isTextual()) {
            return sanitizeLinkValue(fieldNode.asText());
        }
        String url = sanitizeLinkValue(fieldNode.path("url").asText(null));
        if (!isBlank(url)) {
            return url;
        }
        String uri = sanitizeLinkValue(fieldNode.path("uri").asText(null));
        if (!isBlank(uri)) {
            return uri;
        }
        return null;
    }

    private String sanitizeLinkValue(String rawValue) {
        if (isBlank(rawValue)) {
            return null;
        }
        String normalized = rawValue.replaceAll("\\s+", " ").trim();
        if (normalized.startsWith("internal:")) {
            normalized = normalized.substring("internal:".length());
        }
        if (normalized.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(normalized);
            if (uri.isAbsolute()) {
                return uri.toString();
            }
            return OFFICIAL_SITE_URI.resolve(normalized).toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractStableLinkIdentity(String alertLink) {
        try {
            URI uri = URI.create(alertLink.trim());
            String host = uri.getHost();
            String path = uri.getPath();
            if (isBlank(host) && isBlank(path)) {
                return null;
            }
            String identitySeed = ((host == null ? "" : host) + (path == null ? "" : path))
                    .replaceAll("/+$", "");
            return slugify(identitySeed);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String removeRegistrationWindow(String alertTitle) {
        if (isBlank(alertTitle)) {
            return alertTitle;
        }
        return REGISTRATION_WINDOW_PATTERN.matcher(alertTitle)
                .replaceFirst("")
                .replaceAll("\\s+", " ")
                .trim();
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record RegistrationWindow(LocalDate startDate, LocalDate endDate) {
        private static RegistrationWindow empty() {
            return new RegistrationWindow(null, null);
        }
    }
}
