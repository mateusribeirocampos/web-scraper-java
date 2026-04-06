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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for the official Câmara Municipal de Santa Rita do Sapucaí process page.
 */
@Component
public class CamaraSantaRitaProcessosSeletivosParser {

    private static final Pattern TITLE_PATTERN =
            Pattern.compile("(?i)edital\\s*n[ºo]?\\s*(\\d+/\\d{4})\\s*:\\s*(.+)$");
    private static final Pattern DATE_RANGE_PATTERN =
            Pattern.compile("(\\d{2}/\\d{2}/\\d{4})\\s*a\\s*(\\d{2}/\\d{2}/\\d{4})");
    private static final DateTimeFormatter BRAZILIAN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public List<CamaraSantaRitaContestPreviewItem> parse(String html, String sourceUrl) {
        Objects.requireNonNull(html, "html must not be null");
        Objects.requireNonNull(sourceUrl, "sourceUrl must not be null");

        Document document = Jsoup.parse(html, sourceUrl);
        Element root = document.selectFirst("div[id^=parent-fieldname-text]");
        if (root == null) {
            root = document.body();
        }

        List<CamaraSantaRitaContestPreviewItem> items = new ArrayList<>();
        String currentTitle = null;
        List<Element> blockElements = new ArrayList<>();

        for (Element element : root.children()) {
            String title = extractContestTitle(element.text());
            if (title != null) {
                flushBlock(items, currentTitle, blockElements, sourceUrl);
                currentTitle = title;
                blockElements = new ArrayList<>();
                blockElements.add(element);
                continue;
            }
            if (currentTitle != null) {
                blockElements.add(element);
            }
        }

        flushBlock(items, currentTitle, blockElements, sourceUrl);
        return List.copyOf(items);
    }

    private void flushBlock(
            List<CamaraSantaRitaContestPreviewItem> items,
            String title,
            List<Element> blockElements,
            String sourceUrl
    ) {
        if (title == null || title.isBlank()) {
            return;
        }

        List<CamaraSantaRitaContestAttachment> attachments = extractAttachments(blockElements);
        CamaraSantaRitaContestAttachment editalAttachment = selectEditalAttachment(attachments);
        if (editalAttachment == null) {
            return;
        }

        LocalDate publishedAt = null;
        LocalDate registrationStartDate = null;
        LocalDate registrationEndDate = null;

        for (Element element : blockElements) {
            if (!"table".equalsIgnoreCase(element.tagName())) {
                continue;
            }
            for (Element row : element.select("tr")) {
                Elements cells = row.select("td");
                if (cells.size() < 3) {
                    continue;
                }
                String label = clean(cells.get(0).text());
                String dateText = clean(cells.get(2).text());

                if (isEditalLabel(label) && publishedAt == null) {
                    publishedAt = parseSingleDate(dateText);
                }
                if (isRegistrationLabel(label) && registrationEndDate == null) {
                    LocalDate[] range = parseDateRange(dateText);
                    if (range != null) {
                        registrationStartDate = range[0];
                        registrationEndDate = range[1];
                    }
                }
            }
        }

        String contestNumber = extractContestNumber(title);
        Integer editalYear = extractEditalYear(contestNumber);
        items.add(new CamaraSantaRitaContestPreviewItem(
                title,
                "Câmara Municipal de Santa Rita do Sapucaí",
                inferPositionTitle(title),
                "SUPERIOR",
                contestNumber,
                editalYear,
                sourceUrl,
                editalAttachment.url(),
                publishedAt,
                registrationStartDate,
                registrationEndDate,
                attachments
        ));
    }

    private List<CamaraSantaRitaContestAttachment> extractAttachments(List<Element> blockElements) {
        List<CamaraSantaRitaContestAttachment> attachments = new ArrayList<>();
        for (Element element : blockElements) {
            for (Element link : element.select("a[href]")) {
                String label = clean(link.text());
                String url = clean(link.absUrl("href"));
                if (label.isBlank() || url.isBlank() || url.contains("docs.google.com")) {
                    continue;
                }
                attachments.add(new CamaraSantaRitaContestAttachment(label, url));
            }
        }
        return List.copyOf(attachments);
    }

    private CamaraSantaRitaContestAttachment selectEditalAttachment(List<CamaraSantaRitaContestAttachment> attachments) {
        return attachments.stream()
                .filter(attachment -> isEditalLabel(attachment.label()) && !isFollowUpLabel(attachment.label()))
                .findFirst()
                .orElse(null);
    }

    private boolean isEditalLabel(String value) {
        String normalized = normalize(value);
        return normalized.contains("divulgacao do edital");
    }

    private boolean isRegistrationLabel(String value) {
        return normalize(value).contains("periodo de inscricoes");
    }

    private boolean isFollowUpLabel(String value) {
        String normalized = normalize(value);
        return normalized.contains("retificacao")
                || normalized.contains("resultado")
                || normalized.contains("classificacao")
                || normalized.contains("homologacao")
                || normalized.contains("prorrogacao");
    }

    private String extractContestTitle(String text) {
        Matcher matcher = TITLE_PATTERN.matcher(clean(text));
        if (!matcher.find()) {
            return null;
        }
        return "Edital nº %s: %s".formatted(matcher.group(1), clean(matcher.group(2)));
    }

    private String extractContestNumber(String title) {
        Matcher matcher = TITLE_PATTERN.matcher(title);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Integer extractEditalYear(String contestNumber) {
        if (contestNumber == null || !contestNumber.contains("/")) {
            return null;
        }
        return Integer.parseInt(contestNumber.substring(contestNumber.indexOf('/') + 1));
    }

    private String inferPositionTitle(String title) {
        String cleaned = clean(title);
        int colonIndex = cleaned.indexOf(':');
        if (colonIndex < 0 || colonIndex == cleaned.length() - 1) {
            return cleaned;
        }
        String suffix = clean(cleaned.substring(colonIndex + 1));
        return suffix.replaceFirst("(?i)^processo seletivo\\s+", "");
    }

    private LocalDate parseSingleDate(String value) {
        try {
            return LocalDate.parse(clean(value), BRAZILIAN_DATE);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private LocalDate[] parseDateRange(String value) {
        Matcher matcher = DATE_RANGE_PATTERN.matcher(clean(value));
        if (!matcher.find()) {
            return null;
        }
        try {
            return new LocalDate[] {
                    LocalDate.parse(matcher.group(1), BRAZILIAN_DATE),
                    LocalDate.parse(matcher.group(2), BRAZILIAN_DATE)
            };
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    private String normalize(String value) {
        return Normalizer.normalize(clean(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
