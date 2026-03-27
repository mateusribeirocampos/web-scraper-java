package com.campos.webscraper.infrastructure.parser;

import java.util.List;

/**
 * Preview summary produced from an Inconfidentes HTML page.
 */
public record InconfidentesParsePreview(
        String sourceUrl,
        int itemsFound,
        List<InconfidentesContestPreviewItem> items
) {
}
