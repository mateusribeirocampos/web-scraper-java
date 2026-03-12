package com.campos.webscraper.infrastructure.parser;

import java.util.List;

/**
 * Structured preview of what the PCI Concursos listing parser extracted from a fixture.
 */
public record PciConcursosParsePreview(
        String sourceUrl,
        String selectorBundleVersion,
        int itemsFound,
        List<PciConcursosPreviewItem> items
) {
}
