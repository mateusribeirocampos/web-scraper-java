package com.campos.webscraper.infrastructure.parser;

/**
 * Attachment extracted from a Pouso Alegre contest detail page.
 */
public record PousoAlegreContestAttachment(
        String type,
        String label,
        String url
) {
}
