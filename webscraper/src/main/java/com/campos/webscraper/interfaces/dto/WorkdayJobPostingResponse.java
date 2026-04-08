package com.campos.webscraper.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * DTO representing one job posting returned by the public Workday jobs endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkdayJobPostingResponse(
        String title,
        String externalPath,
        String locationsText,
        String postedOn,
        List<String> bulletFields
) {
}
