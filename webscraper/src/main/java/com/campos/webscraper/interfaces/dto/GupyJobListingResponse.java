package com.campos.webscraper.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing one job posting from the Gupy Portal API.
 * Maps fields from the public endpoint: GET /api/v1/jobs?jobName=...
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GupyJobListingResponse(
        long id,
        String name,
        String description,
        @JsonProperty("careerPageName")
        String careerPageName,
        @JsonProperty("publishedDate")
        String publishedDate,
        @JsonProperty("applicationDeadline")
        String applicationDeadline,
        @JsonProperty("isRemoteWork")
        boolean isRemoteWork,
        String city,
        String state,
        String country,
        @JsonProperty("jobUrl")
        String jobUrl,
        @JsonProperty("workplaceType")
        String workplaceType,
        String type
) {
}
