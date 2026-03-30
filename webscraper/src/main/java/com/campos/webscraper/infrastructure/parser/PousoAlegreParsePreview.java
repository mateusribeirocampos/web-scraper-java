package com.campos.webscraper.infrastructure.parser;

import java.util.List;

/**
 * Preview summary produced from Pouso Alegre municipal contest pages.
 */
public record PousoAlegreParsePreview(
        String sourceUrl,
        int itemsFound,
        List<PousoAlegreContestPreviewItem> items
) {
}
