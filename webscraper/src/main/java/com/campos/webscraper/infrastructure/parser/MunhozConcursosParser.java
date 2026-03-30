package com.campos.webscraper.infrastructure.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Munhoz public contest listing and detail pages.
 */
@Component
public class MunhozConcursosParser {

    private static final DateTimeFormatter BRAZILIAN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern CONTEST_NUMBER_PATTERN = Pattern.compile("(?i)(\\d{1,4})/(\\d{4})");
    private static final Pattern EDITAL_YEAR_PATTERN = Pattern.compile(
            "(?i)(?:edital|processo seletivo|processos seletivos|concurso|processo de escolha).{0,40}?(?:n[oº.]\\s*)?(\\d+)/(\\d{4})"
    );

    public List<String> parseListingUrls(String html, String sourceUrl) {
        Objects.requireNonNull(html, "html must not be null");
        Objects.requireNonNull(sourceUrl, "sourceUrl must not be null");

        Document document = Jsoup.parse(html, sourceUrl);
        Set<String> urls = new LinkedHashSet<>();
        for (Element link : document.select("a[href]")) {
            String href = link.absUrl("href");
            if (href.contains("/concursos_view/")) {
                urls.add(href);
            }
        }
        return List.copyOf(urls);
    }

    public MunhozContestPreviewItem parseDetail(String html, String sourceUrl) {
        Objects.requireNonNull(html, "html must not be null");
        Objects.requireNonNull(sourceUrl, "sourceUrl must not be null");

        Document document = Jsoup.parse(html, sourceUrl);
        String contestTitle = firstNonBlank(
                extractMetadataValue(document, "Súmula:"),
                firstHeading(document, "h2", "h5")
        );
        if (contestTitle == null || contestTitle.isBlank()) {
            return null;
        }
        String contestType = firstNonBlank(
                extractMetadataValue(document, "Tipo:"),
                firstHeading(document, "h2", "h5"),
                contestTitle
        );
        if (!isOperationalContest(contestType, contestTitle)) {
            return null;
        }

        String contestNumber = extractContestNumber(firstNonBlank(
                extractMetadataValue(document, "Nº/ Ano:"),
                extractMetadataValue(document, "No/ Ano:"),
                extractMetadataValue(document, "N°/ Ano:"),
                contestTitle
        ));

        List<MunhozContestAttachment> attachments = parseAttachments(document);
        MunhozContestAttachment editalAttachment = selectEditalAttachment(attachments);
        if (editalAttachment == null) {
            return null;
        }

        LocalDate publishedAt = parseDate(extractMetadataValue(document, "Data:"));
        Integer editalYear = inferEditalYear(contestNumber, contestTitle, attachments);

        return new MunhozContestPreviewItem(
                contestTitle,
                "Prefeitura Municipal de Munhoz",
                inferPositionTitle(contestTitle),
                inferEducationLevel(contestTitle),
                null,
                contestNumber,
                editalYear,
                sourceUrl,
                editalAttachment.url(),
                publishedAt,
                null,
                null,
                null,
                attachments,
                List.of(),
                List.of()
        );
    }

    private List<MunhozContestAttachment> parseAttachments(Document document) {
        List<MunhozContestAttachment> attachments = new ArrayList<>();
        for (Element row : document.select("table tr")) {
            Elements cells = row.select("td");
            if (cells.size() < 4) {
                continue;
            }
            Element link = cells.get(3).selectFirst("a[href]");
            if (link == null) {
                continue;
            }
            String url = link.absUrl("href");
            if (url.isBlank()) {
                continue;
            }
            attachments.add(new MunhozContestAttachment(
                    cells.get(0).text().trim(),
                    cells.get(1).text().trim(),
                    url
            ));
        }
        return List.copyOf(attachments);
    }

    private MunhozContestAttachment selectEditalAttachment(List<MunhozContestAttachment> attachments) {
        return attachments.stream()
                .filter(this::isPrimaryEditalAttachment)
                .findFirst()
                .orElse(null);
    }

    private boolean isPrimaryEditalAttachment(MunhozContestAttachment attachment) {
        String normalizedType = normalize(attachment.type());
        String normalizedLabel = normalize(attachment.label());
        boolean editalLike = normalizedType.contains("edital")
                && (normalizedLabel.contains("edital")
                || normalizedLabel.contains("processo seletivo")
                || normalizedLabel.contains("concurso")
                || normalizedLabel.contains("processo de escolha"));
        return editalLike && !isFollowUpAttachment(normalizedLabel);
    }

    private boolean isFollowUpAttachment(String normalizedLabel) {
        return normalizedLabel.contains("resultado")
                || normalizedLabel.contains("gabarito")
                || normalizedLabel.contains("classificacao")
                || normalizedLabel.contains("homologacao")
                || normalizedLabel.contains("convocacao")
                || normalizedLabel.contains("retificacao")
                || normalizedLabel.contains("formulario")
                || normalizedLabel.contains("inscritos")
                || normalizedLabel.contains("inscricao");
    }

    private String extractMetadataValue(Document document, String label) {
        for (Element row : document.select("table tr")) {
            Elements cells = row.select("td");
            if (cells.size() < 2) {
                continue;
            }
            String left = normalize(cells.get(0).text());
            if (left.equals(normalize(label))) {
                String value = cells.get(1).text().replaceAll("\\s+", " ").trim();
                return value.isBlank() ? null : value;
            }
        }
        return null;
    }

    private String firstHeading(Document document, String... tags) {
        for (String tag : tags) {
            Element element = document.selectFirst(tag);
            if (element != null) {
                String text = element.text().replaceAll("\\s+", " ").trim();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    private boolean isOperationalContest(String contestType, String contestTitle) {
        String normalizedType = normalize(contestType);
        String normalizedTitle = normalize(contestTitle);
        boolean contestLike = normalizedType.contains("processo seletivo")
                || normalizedType.contains("processos seletivos")
                || normalizedType.contains("concurso")
                || normalizedTitle.contains("processo seletivo")
                || normalizedTitle.contains("processos seletivos")
                || normalizedTitle.contains("concurso")
                || normalizedTitle.contains("processo de escolha")
                || normalizedTitle.contains("conselho tutelar");
        return contestLike && !isFollowUpNotice(normalizedTitle);
    }

    private boolean isFollowUpNotice(String normalizedTitle) {
        return normalizedTitle.startsWith("edital de convocacao")
                || normalizedTitle.startsWith("convocacao")
                || normalizedTitle.startsWith("resultado")
                || normalizedTitle.startsWith("homologacao")
                || normalizedTitle.startsWith("gabarito")
                || normalizedTitle.startsWith("classificacao")
                || normalizedTitle.startsWith("retificacao");
    }

    private Integer inferEditalYear(String contestNumber, String title, List<MunhozContestAttachment> attachments) {
        Integer year = extractEditalYear(contestNumber);
        if (year != null) {
            return year;
        }
        year = extractEditalYear(title);
        if (year != null) {
            return year;
        }
        return attachments.stream()
                .map(MunhozContestAttachment::label)
                .map(this::extractEditalYear)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Integer extractEditalYear(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        Matcher contestNumberMatcher = CONTEST_NUMBER_PATTERN.matcher(rawValue);
        if (contestNumberMatcher.find()) {
            return Integer.parseInt(contestNumberMatcher.group(2));
        }
        Matcher matcher = EDITAL_YEAR_PATTERN.matcher(rawValue);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(2));
    }

    private String extractContestNumber(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        Matcher matcher = CONTEST_NUMBER_PATTERN.matcher(rawValue);
        if (!matcher.find()) {
            return null;
        }
        int contestNumber = Integer.parseInt(matcher.group(1));
        return "%03d/%s".formatted(contestNumber, matcher.group(2));
    }

    private String inferPositionTitle(String title) {
        return title;
    }

    private String inferEducationLevel(String title) {
        String normalized = normalize(title);
        if (normalized.contains("tecnico de nivel medio") || normalized.contains("ensino medio") || normalized.contains("nivel medio")) {
            return "MEDIO";
        }
        if (normalized.contains("tecnico")) {
            return "TECNICO";
        }
        if (normalized.contains("superior")
                || normalized.contains("psicologia")
                || normalized.contains("psicologo")
                || normalized.contains("servico social")
                || normalized.contains("assistente social")
                || normalized.contains("docente")
                || normalized.contains("professor")) {
            return "SUPERIOR";
        }
        return null;
    }

    private LocalDate parseDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(rawDate.trim(), BRAZILIAN_DATE);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
