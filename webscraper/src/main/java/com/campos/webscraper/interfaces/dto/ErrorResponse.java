package com.campos.webscraper.interfaces.dto;

/**
 * Minimal error payload returned by REST endpoints.
 */
public record ErrorResponse(
        String message
) {
}
