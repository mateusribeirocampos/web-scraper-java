package com.campos.webscraper.interfaces.dto;

/**
 * DTO representing a single DOU API item relevant to public contest extraction.
 */
public record DouApiItemResponse(
        String id,
        String title,
        String summary,
        String publishedAt,
        String detailUrl
) {
}
