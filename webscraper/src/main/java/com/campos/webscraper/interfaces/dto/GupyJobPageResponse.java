package com.campos.webscraper.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * DTO representing the paginated root payload from the Gupy Portal API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GupyJobPageResponse(
        List<GupyJobListingResponse> data,
        GupyPaginationResponse pagination
) {
}
