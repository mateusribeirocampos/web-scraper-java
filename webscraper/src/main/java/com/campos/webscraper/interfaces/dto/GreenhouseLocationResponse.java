package com.campos.webscraper.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO representing the location object returned by the Greenhouse Job Board API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GreenhouseLocationResponse(
        String name,
        String country
) {
}
