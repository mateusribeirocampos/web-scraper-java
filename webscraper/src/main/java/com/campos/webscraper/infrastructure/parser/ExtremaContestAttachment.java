package com.campos.webscraper.infrastructure.parser;

/**
 * Attachment reference found on an Extrema municipal education detail page.
 */
public record ExtremaContestAttachment(
        String label,
        String url
) {
}
