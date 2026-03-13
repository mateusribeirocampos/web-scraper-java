package com.campos.webscraper.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing one published Greenhouse job posting.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GreenhouseJobBoardItemResponse(
        long id,
        String title,
        @JsonProperty("absolute_url")
        String absoluteUrl,
        @JsonProperty("company_name")
        String companyName,
        GreenhouseLocationResponse location,
        @JsonProperty("first_published")
        String firstPublished,
        String content
) {
}
