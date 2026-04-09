package com.campos.webscraper.infrastructure.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PocosCaldasContestPdfParser {

    private static final Pattern CONTEST_NUMBER_PATTERN =
            Pattern.compile("EDITAL DE PROCESSO SELETIVO SIMPLIFICADO N[º°O]\\s*([0-9]{3}/20\\d{2})");
    private static final Pattern REGISTRATION_PERIOD_PATTERN =
            Pattern.compile("a partir das \\d{1,2}h(?:\\d{2})? do dia (\\d{2}/\\d{2}/\\d{4}) até às \\d{1,2}h(?:\\d{2})? do dia (\\d{2}/\\d{2}/\\d{4})");
    private static final DateTimeFormatter BRAZILIAN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);
    private static final Pattern YEAR_PATTERN = Pattern.compile("(20\\d{2})");
    private static final Pattern CONTEST_NUMBER_IN_ATTACHMENT_PATTERN = Pattern.compile("(\\d{1,4})[-_/](20\\d{2})");

    public PocosCaldasContestPreviewItem parse(String pdfText, String editalUrl) {
        Objects.requireNonNull(pdfText, "pdfText must not be null");
        Objects.requireNonNull(editalUrl, "editalUrl must not be null");

        String normalized = normalizeWhitespace(pdfText);
        String contestNumber = extractContestNumber(normalized);
        LocalDate registrationStartDate = extractRegistrationDate(normalized, 1);
        LocalDate registrationEndDate = extractRegistrationDate(normalized, 2);

        return new PocosCaldasContestPreviewItem(
                contestTitle(contestNumber),
                "Prefeitura Municipal de Poços de Caldas",
                "Processo seletivo simplificado para múltiplos cargos",
                "UNKNOWN",
                contestNumber,
                extractYear(contestNumber),
                editalUrl,
                null,
                registrationStartDate,
                registrationEndDate,
                null,
                null,
                "Vencimentos variáveis conforme Anexo IV do edital",
                excerpt(normalized)
        );
    }

    public String selectCanonicalEditalUrl(String html, String sourceUrl) {
        Objects.requireNonNull(html, "html must not be null");
        Objects.requireNonNull(sourceUrl, "sourceUrl must not be null");

        Document document = Jsoup.parse(html, sourceUrl);
        return document.select("a[href]").stream()
                .map(link -> new AttachmentCandidate(
                        link.absUrl("href"),
                        link.text().replaceAll("\\s+", " ").trim()
                ))
                .filter(candidate -> !candidate.url().isBlank())
                .filter(candidate -> candidate.url().toLowerCase(Locale.ROOT).contains(".pdf"))
                .filter(this::looksLikePrimaryEdital)
                .sorted(Comparator
                        .comparingInt(this::candidateYear).reversed()
                        .thenComparing(Comparator.comparingInt(this::candidateContestNumber).reversed())
                        .thenComparing(AttachmentCandidate::url))
                .map(AttachmentCandidate::url)
                .findFirst()
                .orElse(null);
    }

    private boolean looksLikePrimaryEdital(AttachmentCandidate candidate) {
        String haystack = normalize(candidate.label() + " " + candidate.url());
        boolean editalLike = haystack.contains("edital")
                && (haystack.contains("processo seletivo")
                || haystack.contains("processo seletivo simplificado")
                || haystack.contains("concurso"));
        return editalLike && !isFollowUpArtifact(haystack);
    }

    private boolean isFollowUpArtifact(String haystack) {
        return List.of(
                        "retificacao",
                        "retificação",
                        "errata",
                        "homologacao",
                        "homologação",
                        "resultado",
                        "gabarito",
                        "recurso",
                        "convocacao",
                        "convocação",
                        "classificacao",
                        "classificação",
                        "aditamento"
                ).stream()
                .map(this::normalize)
                .anyMatch(haystack::contains);
    }

    private int candidateYear(AttachmentCandidate candidate) {
        Matcher matcher = YEAR_PATTERN.matcher(candidate.url() + " " + candidate.label());
        int year = -1;
        while (matcher.find()) {
            year = Integer.parseInt(matcher.group(1));
        }
        return year;
    }

    private int candidateContestNumber(AttachmentCandidate candidate) {
        Matcher matcher = CONTEST_NUMBER_IN_ATTACHMENT_PATTERN.matcher(candidate.url() + " " + candidate.label());
        int contestNumber = -1;
        while (matcher.find()) {
            contestNumber = Integer.parseInt(matcher.group(1));
        }
        return contestNumber;
    }

    private String extractContestNumber(String text) {
        Matcher matcher = CONTEST_NUMBER_PATTERN.matcher(text);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Poços de Caldas PDF must expose edital number");
        }
        return matcher.group(1);
    }

    private LocalDate extractRegistrationDate(String text, int groupIndex) {
        Matcher matcher = REGISTRATION_PERIOD_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return LocalDate.parse(matcher.group(groupIndex), BRAZILIAN_DATE);
    }

    private Integer extractYear(String contestNumber) {
        String[] parts = contestNumber.split("/");
        return parts.length == 2 ? Integer.parseInt(parts[1]) : null;
    }

    private String contestTitle(String contestNumber) {
        return "Edital de Processo Seletivo Simplificado nº " + contestNumber;
    }

    private String excerpt(String normalized) {
        int endIndex = Math.min(normalized.length(), 280);
        return normalized.substring(0, endIndex);
    }

    private String normalizeWhitespace(String value) {
        return value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replace('_', ' ')
                .replace('/', ' ')
                .replace('ç', 'c')
                .replace('ã', 'a')
                .replace('á', 'a')
                .replace('à', 'a')
                .replace('â', 'a')
                .replace('é', 'e')
                .replace('ê', 'e')
                .replace('í', 'i')
                .replace('ó', 'o')
                .replace('ô', 'o')
                .replace('õ', 'o')
                .replace('ú', 'u');
    }

    private record AttachmentCandidate(String url, String label) {
    }
}
