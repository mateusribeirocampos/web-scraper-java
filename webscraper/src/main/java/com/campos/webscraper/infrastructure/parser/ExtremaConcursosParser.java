package com.campos.webscraper.infrastructure.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Extrema education listing and contest detail pages.
 */
@Component
public class ExtremaConcursosParser {

    private static final Pattern CONTEST_NUMBER_PATTERN = Pattern.compile("(?i)(\\d{1,4})/(\\d{4})");
    private static final Pattern EDITAL_YEAR_PATTERN = Pattern.compile(
            "(?i)(?:edital|processo seletivo|concurso|contrata[cç][aã]o).{0,40}?(?:n[oº°.\\s]*)?(\\d+)/(\\d{4})"
    );
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

    public List<String> parseListingUrls(String html, String sourceUrl) {
        Objects.requireNonNull(html, "html must not be null");
        Objects.requireNonNull(sourceUrl, "sourceUrl must not be null");

        Document document = Jsoup.parse(html, sourceUrl);
        Set<String> urls = new LinkedHashSet<>();
        for (Element link : document.select("a.list-item__link[href], a[href]")) {
            String href = link.absUrl("href");
            if (href.isBlank() || href.equals(sourceUrl) || !href.contains("/secretarias/educacao/")) {
                continue;
            }
            if (!looksLikeContestListingLink(link.text(), href)) {
                continue;
            }
            urls.add(href);
        }
        return List.copyOf(urls);
    }

    public ExtremaContestPreviewItem parseDetail(String html, String sourceUrl) {
        Objects.requireNonNull(html, "html must not be null");
        Objects.requireNonNull(sourceUrl, "sourceUrl must not be null");

        Document document = Jsoup.parse(html, sourceUrl);
        String contestTitle = firstNonBlank(
                clean(text(document.selectFirst("h1.page-secretarias__titulo"))),
                clean(text(document.selectFirst("h1"))),
                clean(text(document.selectFirst("title")))
        );
        if (contestTitle == null || contestTitle.isBlank() || !isOperationalContest(contestTitle)) {
            return null;
        }

        Element content = firstNonNull(
                document.selectFirst("main.page-secretarias__grid-item.conteudo"),
                document.selectFirst("main.conteudo"),
                document.body()
        );
        List<ExtremaContestAttachment> attachments = parseAttachments(content);
        ExtremaContestAttachment editalAttachment = attachments.stream()
                .filter(this::isPrimaryEditalAttachment)
                .findFirst()
                .orElse(null);
        if (editalAttachment == null) {
            return null;
        }

        String contestNumber = firstNonBlank(
                extractContestNumber(contestTitle),
                attachments.stream()
                        .map(ExtremaContestAttachment::label)
                        .map(this::extractContestNumber)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null)
        );

        Integer editalYear = inferEditalYear(contestNumber, contestTitle, attachments);

        return new ExtremaContestPreviewItem(
                contestTitle,
                "Prefeitura Municipal de Extrema",
                contestTitle,
                null,
                null,
                contestNumber,
                editalYear,
                sourceUrl,
                editalAttachment.url(),
                parsePublishedAt(document),
                null,
                null,
                null,
                attachments,
                List.of(),
                List.of()
        );
    }

    private List<ExtremaContestAttachment> parseAttachments(Element content) {
        List<ExtremaContestAttachment> attachments = new ArrayList<>();
        Set<String> seenUrls = new LinkedHashSet<>();
        for (Element link : content.select("a[href]")) {
            String url = clean(link.absUrl("href"));
            if (url.isBlank() || !url.toLowerCase(Locale.ROOT).contains(".pdf")) {
                continue;
            }
            if (!seenUrls.add(url)) {
                continue;
            }
            String label = clean(link.text());
            if (label.isBlank()) {
                label = inferAttachmentLabel(url);
            }
            attachments.add(new ExtremaContestAttachment(label, url));
        }
        return List.copyOf(attachments);
    }

    private boolean looksLikeContestListingLink(String label, String href) {
        String haystack = normalize(label + " " + href);
        return (haystack.contains("processo seletivo")
                || haystack.contains("concurso")
                || haystack.contains("contratacoes de professores")
                || haystack.contains("edital de selecao"))
                && !isFollowUpText(haystack);
    }

    private boolean isOperationalContest(String contestTitle) {
        String normalized = normalize(contestTitle);
        return (normalized.contains("processo seletivo")
                || normalized.contains("concurso")
                || normalized.contains("contratacoes de professores")
                || normalized.contains("edital de selecao"))
                && !isFollowUpText(normalized);
    }

    private boolean isPrimaryEditalAttachment(ExtremaContestAttachment attachment) {
        String haystack = normalize(attachment.label() + " " + attachment.url());
        boolean editalLike = haystack.contains("edital")
                || haystack.contains("processo seletivo")
                || haystack.contains("concurso");
        return editalLike && !isFollowUpText(haystack);
    }

    private boolean isFollowUpText(String normalizedText) {
        return normalizedText.contains("retificacao")
                || normalizedText.contains("retificado")
                || normalizedText.contains("republicacao")
                || normalizedText.contains("republicacao")
                || normalizedText.contains("homologacao")
                || normalizedText.contains("convocacao")
                || normalizedText.contains("resultado")
                || normalizedText.contains("gabarito")
                || normalizedText.contains("oficio")
                || normalizedText.contains("despacho")
                || normalizedText.contains("comissao de acompanhamento")
                || normalizedText.contains("classificacao");
    }

    private LocalDate parsePublishedAt(Document document) {
        Element meta = firstNonNull(
                document.selectFirst("meta[property=article:published_time]"),
                document.selectFirst("meta[property=og:published_time]")
        );
        if (meta == null) {
            return null;
        }
        String value = clean(meta.attr("content"));
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value)
                    .atZoneSameInstant(BRAZIL_ZONE)
                    .toLocalDate();
        } catch (Exception exception) {
            return null;
        }
    }

    private Integer inferEditalYear(String contestNumber, String title, List<ExtremaContestAttachment> attachments) {
        Integer year = extractEditalYear(contestNumber);
        if (year != null) {
            return year;
        }
        year = extractEditalYear(title);
        if (year != null) {
            return year;
        }
        return attachments.stream()
                .map(ExtremaContestAttachment::label)
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

    private String inferAttachmentLabel(String url) {
        String path = URI.create(url).getPath();
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private String text(Element element) {
        return element == null ? null : element.text();
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        return value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
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
}
