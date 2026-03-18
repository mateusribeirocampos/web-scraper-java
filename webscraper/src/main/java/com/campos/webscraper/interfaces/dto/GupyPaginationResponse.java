package com.campos.webscraper.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO representing pagination metadata from the Gupy Portal API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GupyPaginationResponse(
        int offset,
        int limit,
        int total
) {
}
