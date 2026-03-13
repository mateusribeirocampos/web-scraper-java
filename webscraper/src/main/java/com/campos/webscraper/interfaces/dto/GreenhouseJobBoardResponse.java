package com.campos.webscraper.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * DTO representing the root payload returned by the Greenhouse Job Board API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GreenhouseJobBoardResponse(
        List<GreenhouseJobBoardItemResponse> jobs
) {
}
