package com.campos.webscraper.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing one published posting from the Lever public postings API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LeverPostingResponse(
        String id,
        String text,
        @JsonProperty("hostedUrl")
        String hostedUrl,
        @JsonProperty("applyUrl")
        String applyUrl,
        @JsonProperty("workplaceType")
        String workplaceType,
        LeverCategoriesResponse categories,
        String description
) {
}
