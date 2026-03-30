package com.campos.webscraper.infrastructure.parser;

/**
 * Attachment found in a Munhoz contest detail page.
 */
public record MunhozContestAttachment(
        String type,
        String label,
        String url
) {
}
