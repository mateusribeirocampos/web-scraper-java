package com.campos.webscraper.application.enrichment;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic parser for extracting contest metadata from raw edital PDF text.
 */
@Component
public class InconfidentesEditalPdfMetadataParser {

    private static final DateTimeFormatter BRAZILIAN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern REGISTRATION_WINDOW_PATTERN = Pattern.compile(
            "(?is)inscri[cç][oõ]es?.{0,80}?(\\d{2}/\\d{2}/\\d{4}).{0,20}?(?:a|ate|at[eé])\\s*(\\d{2}/\\d{2}/\\d{4})"
    );
    private static final Pattern EXAM_DATE_PATTERN = Pattern.compile(
            "(?is)(?:data da prova|prova objetiva|provas? ser[aã]o realizadas?).{0,60}?(\\d{2}/\\d{2}/\\d{4})"
    );
    private static final Pattern POSITION_PATTERN = Pattern.compile(
            "(?is)(?:cargo|emprego p[úu]blico|fun[cç][aã]o)\\s*:?\\s*([A-Za-zÀ-ÿ0-9 /-]{4,120})"
    );
    private static final Pattern FORMATION_PATTERN = Pattern.compile(
            "(?is)(?:escolaridade|requisito(?:s)?|forma[cç][aã]o(?: exigida)?)\\s*:?\\s*([A-Za-zÀ-ÿ0-9,;()\\- /]{8,220})"
    );
    private static final Pattern ANNEX_PATTERN = Pattern.compile(
            "(?ium)^\\s*(anexo\\s+(?:[ivxlcdm0-9]+|[uú]nico)(?:\\s*[-–:]\\s*[^\\r\\n]{1,120})?)\\s*$"
    );

    public InconfidentesEditalPdfMetadata parse(String pdfText) {
        Objects.requireNonNull(pdfText, "pdfText must not be null");

        String formationRequirements = parseFormationRequirements(pdfText);
        List<String> positionTitles = parsePositionTitles(pdfText);
        return new InconfidentesEditalPdfMetadata(
                positionTitles.size() == 1 ? positionTitles.getFirst() : null,
                positionTitles,
                parseEducationLevel(formationRequirements),
                formationRequirements,
                parseRegistrationStartDate(pdfText),
                parseRegistrationEndDate(pdfText),
                parseExamDate(pdfText),
                parseAnnexReferences(pdfText)
        );
    }

    private List<String> parsePositionTitles(String text) {
        Matcher matcher = POSITION_PATTERN.matcher(text);
        Set<String> distinctTitles = new LinkedHashSet<>();
        while (matcher.find()) {
            String value = matcher.group(1).replaceAll("\\s+", " ").trim();
            if (!value.isBlank()) {
                distinctTitles.add(value);
            }
        }
        return List.copyOf(distinctTitles);
    }

    private String parseEducationLevel(String formationRequirements) {
        if (formationRequirements == null || formationRequirements.isBlank()) {
            return null;
        }
        String normalized = normalize(formationRequirements);
        if (normalized.contains("mestrado") || normalized.contains("doutorado")
                || normalized.contains("especializacao") || normalized.contains("pos-graduacao")
                || normalized.contains("pos graduacao")) {
            return "POS_GRADUACAO";
        }
        if (normalized.contains("curso tecnico") || normalized.contains("tecnico em")) {
            return "TECNICO";
        }
        if (normalized.contains("ensino medio") || normalized.contains("nivel medio")) {
            return "MEDIO";
        }
        if (normalized.contains("ensino fundamental") || normalized.contains("nivel fundamental")) {
            return "FUNDAMENTAL";
        }
        if (normalized.contains("ensino superior")
                || normalized.contains("nivel superior")
                || normalized.contains("graduacao")
                || normalized.contains("ciencia da computacao")
                || normalized.contains("sistemas de informacao")
                || normalized.contains("analise e desenvolvimento de sistemas")
                || normalized.contains("engenharia da computacao")) {
            return "SUPERIOR";
        }
        return null;
    }

    private String parseFormationRequirements(String text) {
        Matcher matcher = FORMATION_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1).replaceAll("\\s+", " ").trim();
        return value.isBlank() ? null : value;
    }

    private LocalDate parseRegistrationStartDate(String text) {
        Matcher matcher = REGISTRATION_WINDOW_PATTERN.matcher(text);
        return matcher.find() ? parseDate(matcher.group(1)) : null;
    }

    private LocalDate parseRegistrationEndDate(String text) {
        Matcher matcher = REGISTRATION_WINDOW_PATTERN.matcher(text);
        return matcher.find() ? parseDate(matcher.group(2)) : null;
    }

    private LocalDate parseExamDate(String text) {
        Matcher matcher = EXAM_DATE_PATTERN.matcher(text);
        return matcher.find() ? parseDate(matcher.group(1)) : null;
    }

    private List<String> parseAnnexReferences(String text) {
        Matcher matcher = ANNEX_PATTERN.matcher(text);
        Set<String> annexReferences = new LinkedHashSet<>();
        while (matcher.find()) {
            String value = matcher.group(1).replaceAll("\\s+", " ").trim();
            if (!value.isBlank()) {
                annexReferences.add(value);
            }
        }
        return List.copyOf(annexReferences);
    }

    private LocalDate parseDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(rawDate, BRAZILIAN_DATE);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
