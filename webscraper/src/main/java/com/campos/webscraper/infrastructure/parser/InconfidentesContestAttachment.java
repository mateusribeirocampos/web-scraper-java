package com.campos.webscraper.infrastructure.parser;

/**
 * Attachment link found under an Inconfidentes contest listing block.
 */
public record InconfidentesContestAttachment(
        String label,
        String url
) {
}
