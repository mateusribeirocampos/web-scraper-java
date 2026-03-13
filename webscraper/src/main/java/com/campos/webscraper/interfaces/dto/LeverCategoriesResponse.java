package com.campos.webscraper.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO representing the categories object from the Lever postings API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LeverCategoriesResponse(
        String team,
        String location,
        String commitment
) {
}
