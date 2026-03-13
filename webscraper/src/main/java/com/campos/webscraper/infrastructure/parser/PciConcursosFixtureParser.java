package com.campos.webscraper.infrastructure.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal fixture-driven parser for the first PCI Concursos HTML listing slice.
 *
 * <p>This parser intentionally keeps selectors inline. Story 8.2 will extract them into a
 * dedicated SelectorBundle.
 */
@Component
public class PciConcursosFixtureParser {

    private static final List<String> REQUIRED_SELECTOR_FIELDS = Arrays.asList(
            "contestCard",
            "contestName",
            "organizer",
            "positionTitle",
            "numberOfVacancies",
            "educationLevel",
            "salaryRange",
            "registrationDeadline",
            "detailUrl",
            "nextPage"
    );
    private static final Pattern REGISTRATION_PERIOD_PATTERN =
            Pattern.compile("(\\d{4}-\\d{2}-\\d{2}).*?(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern VACANCIES_PATTERN = Pattern.compile("(\\d[\\d.\\s]*)");
    private final SelectorBundle selectorBundle;

    public PciConcursosFixtureParser() {
        this(PciConcursosSelectorBundles.v1());
    }

    public PciConcursosFixtureParser(SelectorBundle selectorBundle) {
        this.selectorBundle = Objects.requireNonNull(selectorBundle, "selectorBundle must not be null");
        this.selectorBundle.requireSelectors(REQUIRED_SELECTOR_FIELDS);
    }

    public PciConcursosParsePreview parse(String html, String sourceUrl) {
        Objects.requireNonNull(html, "html must not be null");
        Objects.requireNonNull(sourceUrl, "sourceUrl must not be null");

        Document document = Jsoup.parse(html, sourceUrl);
        List<PciConcursosPreviewItem> items = document.select(selector("contestCard")).stream()
                .map(this::toPreviewItem)
                .toList();

        return new PciConcursosParsePreview(
                sourceUrl,
                selectorBundle.selectorBundleVersion(),
                items.size(),
                items
        );
    }

    public String extractNextPageUrl(String html, String sourceUrl) {
        Objects.requireNonNull(html, "html must not be null");
        Objects.requireNonNull(sourceUrl, "sourceUrl must not be null");

        Document document = Jsoup.parse(html, sourceUrl);
        Element nextPageElement = document.selectFirst(selector("nextPage"));
        if (nextPageElement == null) {
            return null;
        }

        String nextPageUrl = nextPageElement.absUrl("href");
        if (nextPageUrl == null || nextPageUrl.isBlank()) {
            return null;
        }
        if (nextPageUrl.equals(sourceUrl + "#")) {
            return null;
        }
        return nextPageUrl;
    }

    private PciConcursosPreviewItem toPreviewItem(Element card) {
        return new PciConcursosPreviewItem(
                text(card, selector("contestName")),
                text(card, selector("organizer")),
                text(card, selector("positionTitle")),
                extractVacancies(text(card, selector("numberOfVacancies"))),
                normalizeEducationLevel(text(card, selector("educationLevel"))),
                text(card, selector("salaryRange")),
                extractRegistrationDate(text(card, selector("registrationDeadline")), 1),
                extractRegistrationDate(text(card, selector("registrationDeadline")), 2),
                absoluteHref(card, selector("contestName")),
                absoluteHref(card, selector("detailUrl"))
        );
    }

    private String text(Element root, String cssQuery) {
        Element element = root.selectFirst(cssQuery);
        return element == null ? "" : element.text().trim();
    }

    private Integer extractVacancies(String rawVacancies) {
        Matcher matcher = VACANCIES_PATTERN.matcher(rawVacancies);
        if (!matcher.find()) {
            return null;
        }

        String normalizedDigits = matcher.group(1).replaceAll("[^\\d]", "");
        return normalizedDigits.isBlank() ? null : Integer.parseInt(normalizedDigits);
    }

    private String normalizeEducationLevel(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String normalized = Normalizer.normalize(rawValue, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
        if (normalized.contains("pos-graduacao") || normalized.contains("especializacao")
                || normalized.contains("mestrado") || normalized.contains("doutorado")) {
            return "POS_GRADUACAO";
        }
        if (normalized.contains("superior")) {
            return "SUPERIOR";
        }
        if (normalized.contains("tecnico")) {
            return "TECNICO";
        }
        if (normalized.contains("medio")) {
            return "MEDIO";
        }
        if (normalized.contains("fundamental")) {
            return "FUNDAMENTAL";
        }
        return null;
    }

    private String extractRegistrationDate(String rawPeriod, int groupIndex) {
        Matcher matcher = REGISTRATION_PERIOD_PATTERN.matcher(rawPeriod);
        return matcher.find() ? matcher.group(groupIndex) : null;
    }

    private String absoluteHref(Element root, String cssQuery) {
        Element element = root.selectFirst(cssQuery);
        return element == null ? null : element.absUrl("href");
    }

    private String selector(String field) {
        return selectorBundle.selector(field);
    }
}
