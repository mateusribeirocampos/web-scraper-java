package com.campos.webscraper.infrastructure.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.text.Normalizer;
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
public class PciConcursosFixtureParser {

    static final String SELECTOR_BUNDLE_VERSION = "pci_concursos_v1";
    private static final Pattern REGISTRATION_PERIOD_PATTERN =
            Pattern.compile("(\\d{4}-\\d{2}-\\d{2}).*?(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern VACANCIES_PATTERN = Pattern.compile("(\\d[\\d.\\s]*)");

    public PciConcursosParsePreview parse(String html, String sourceUrl) {
        Objects.requireNonNull(html, "html must not be null");
        Objects.requireNonNull(sourceUrl, "sourceUrl must not be null");

        Document document = Jsoup.parse(html, sourceUrl);
        List<PciConcursosPreviewItem> items = document.select("article.ca").stream()
                .map(this::toPreviewItem)
                .toList();

        return new PciConcursosParsePreview(
                sourceUrl,
                SELECTOR_BUNDLE_VERSION,
                items.size(),
                items
        );
    }

    private PciConcursosPreviewItem toPreviewItem(Element card) {
        return new PciConcursosPreviewItem(
                text(card, ".ca-link"),
                text(card, ".ca-orgao"),
                text(card, ".ca-cargo"),
                extractVacancies(text(card, ".ca-vagas")),
                normalizeEducationLevel(text(card, ".ca-escolaridade")),
                text(card, ".ca-salario"),
                extractRegistrationDate(text(card, ".ca-inscricoes"), 1),
                extractRegistrationDate(text(card, ".ca-inscricoes"), 2),
                absoluteHref(card, ".ca-detalhes")
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
}
