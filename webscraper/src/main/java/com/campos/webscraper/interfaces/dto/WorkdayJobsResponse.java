package com.campos.webscraper.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * DTO representing the paginated root payload from the public Workday jobs endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkdayJobsResponse(
        int total,
        List<WorkdayJobPostingResponse> jobPostings
) {
}
