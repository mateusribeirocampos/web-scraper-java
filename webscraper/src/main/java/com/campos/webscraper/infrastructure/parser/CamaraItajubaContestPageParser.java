package com.campos.webscraper.infrastructure.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CamaraItajubaContestPageParser {

    private static final Pattern VACANCIES_PATTERN = Pattern.compile("(?i)(\\d{1,3})\\s+vagas");
    private static final Pattern SALARY_RANGE_PATTERN =
            Pattern.compile("(?i)sal[aá]rios\\s+variam\\s+de\\s+R\\$\\s*([\\d.]+,\\d{2})\\s+a\\s+R\\$\\s*([\\d.]+,\\d{2})");
    private static final Pattern REGISTRATION_END_PATTERN =
            Pattern.compile("(?i)at[eé].{0,40}?dia\\s+(\\d{1,2})\\s+de\\s+([a-zç]+)(?:\\s+de\\s+(20\\d{2}))?");
    private static final Pattern EXAM_DATE_PATTERN =
            Pattern.compile("(?i)provas?[^.]{0,80}?dia\\s+(\\d{1,2})\\s+de\\s+([a-zç]+)(?:\\s+de\\s+(20\\d{2}))?");
    private static final Pattern REGISTRATION_MONTH_YEAR_PATTERN =
            Pattern.compile("(?i)inscri[cç][õo]es?[^.]{0,100}?em\\s+([a-zç]+)\\s+de\\s+(20\\d{2})");
    private static final Pattern EDITAL_YEAR_PATTERN = Pattern.compile("(?i)edital[^\\d]*(\\d{4})");
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

    public List<CamaraItajubaContestPreviewItem> parse(String html, String sourceUrl) {
        Objects.requireNonNull(html, "html must not be null");
        Objects.requireNonNull(sourceUrl, "sourceUrl must not be null");

        Document document = Jsoup.parse(html, sourceUrl);
        String contestTitle = clean(extractContestTitle(document));
        if (contestTitle.isBlank()) {
            return List.of();
        }

        Element content = Optional.ofNullable(document.selectFirst(".elementor-widget-theme-post-content"))
                .orElse(document.body());
        String contentText = clean(content.text());
        LocalDate publishedAt = parsePublishedAt(document);
        List<CamaraItajubaContestAttachment> attachments = extractAttachments(content);
        CamaraItajubaContestAttachment editalAttachment = attachments.stream()
                .filter(this::looksLikeCanonicalEditalAttachment)
                .findFirst()
                .orElseGet(() -> attachments.stream()
                .filter(this::looksLikeEditalAttachment)
                .findFirst()
                .orElse(null));
        if (editalAttachment == null) {
            return List.of();
        }

        LocalDate registrationStartDate = parseRegistrationStartDate(contentText);
        Integer editalYear = extractEditalYear(editalAttachment.url());
        Integer scheduleYear = registrationStartDate != null
                ? Integer.valueOf(registrationStartDate.getYear())
                : null;
        LocalDate registrationEndDate = parseRegistrationEndDate(contentText, scheduleYear, registrationStartDate);
        LocalDate examDate = parseExamDate(contentText, scheduleYear);
        Integer numberOfVacancies = extractInteger(contentText, VACANCIES_PATTERN);
        String salaryDescription = extractSalaryDescription(contentText);

        return List.of(new CamaraItajubaContestPreviewItem(
                contestTitle,
                "Câmara Municipal de Itajubá",
                "Cargos efetivos diversos",
                "UNKNOWN",
                null,
                editalYear,
                sourceUrl,
                editalAttachment.url(),
                publishedAt,
                registrationStartDate,
                registrationEndDate,
                examDate,
                numberOfVacancies,
                salaryDescription,
                attachments
        ));
    }

    private String extractContestTitle(Document document) {
        Element heading = document.selectFirst("h1.elementor-heading-title");
        if (heading != null) {
            return heading.text();
        }
        Element title = document.selectFirst("title");
        return title == null ? "" : title.text().replace(" - Câmara Municipal de Itajubá", "");
    }

    private LocalDate parsePublishedAt(Document document) {
        Element meta = document.selectFirst("meta[property=article:published_time]");
        if (meta == null) {
            return null;
        }
        String value = clean(meta.attr("content"));
        if (value.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(value)
                .atZoneSameInstant(BRAZIL_ZONE)
                .toLocalDate();
    }

    private List<CamaraItajubaContestAttachment> extractAttachments(Element content) {
        Set<String> seenUrls = new LinkedHashSet<>();
        List<CamaraItajubaContestAttachment> attachments = new ArrayList<>();
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
            attachments.add(new CamaraItajubaContestAttachment(label, url));
        }
        return List.copyOf(attachments);
    }

    private String inferAttachmentLabel(String url) {
        String path = URI.create(url).getPath();
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private Integer extractEditalYear(String editalUrl) {
        Matcher matcher = EDITAL_YEAR_PATTERN.matcher(editalUrl);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private LocalDate parseMonthDate(String text, Pattern pattern, Integer year) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        Integer resolvedYear = matcher.groupCount() >= 3 && matcher.group(3) != null
                ? Integer.valueOf(Integer.parseInt(matcher.group(3)))
                : year;
        if (resolvedYear == null) {
            return null;
        }
        int day = Integer.parseInt(matcher.group(1));
        int month = monthFromPortuguese(matcher.group(2));
        if (month <= 0) {
            return null;
        }
        return LocalDate.of(resolvedYear, month, day);
    }

    private LocalDate parseRegistrationStartDate(String text) {
        Matcher matcher = REGISTRATION_MONTH_YEAR_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        int month = monthFromPortuguese(matcher.group(1));
        if (month <= 0) {
            return null;
        }
        int year = Integer.parseInt(matcher.group(2));
        return LocalDate.of(year, month, 1);
    }

    private LocalDate parseRegistrationEndDate(String text, Integer fallbackYear, LocalDate registrationStartDate) {
        LocalDate explicitDate = parseMonthDate(text, REGISTRATION_END_PATTERN, fallbackYear);
        if (explicitDate != null) {
            return explicitDate;
        }
        return null;
    }

    private LocalDate parseExamDate(String text, Integer fallbackYear) {
        return parseMonthDate(text, EXAM_DATE_PATTERN, fallbackYear);
    }

    private boolean looksLikeEditalAttachment(CamaraItajubaContestAttachment attachment) {
        return normalize(attachment.label()).contains("edital")
                || normalize(attachment.url()).contains("edital");
    }

    private boolean looksLikeCanonicalEditalAttachment(CamaraItajubaContestAttachment attachment) {
        return looksLikeEditalAttachment(attachment) && !looksLikeFollowUpAttachment(attachment);
    }

    private boolean looksLikeFollowUpAttachment(CamaraItajubaContestAttachment attachment) {
        String text = normalize(attachment.label()) + " " + normalize(attachment.url());
        return text.contains("retificacao")
                || text.contains("retificado")
                || text.contains("errata")
                || text.contains("homologacao")
                || text.contains("gabarito")
                || text.contains("resultado")
                || text.contains("recurso")
                || text.contains("convocacao")
                || text.contains("aditamento");
    }

    private int monthFromPortuguese(String monthName) {
        String normalized = normalize(monthName);
        for (int month = 1; month <= 12; month++) {
            String candidate = java.time.Month.of(month)
                    .getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
            if (normalize(candidate).equals(normalized)) {
                return month;
            }
        }
        return -1;
    }

    private Integer extractInteger(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private String extractSalaryDescription(String text) {
        Matcher matcher = SALARY_RANGE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return "";
        }
        return "Salários variam de R$ %s a R$ %s".formatted(matcher.group(1), matcher.group(2));
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    private String normalize(String value) {
        return java.text.Normalizer.normalize(clean(value), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
