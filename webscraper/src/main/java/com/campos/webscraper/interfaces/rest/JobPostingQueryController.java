package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.usecase.ListJobPostingsUseCase;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.SeniorityLevel;
import com.campos.webscraper.interfaces.dto.JobPostingSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * REST queries for private-sector job postings.
 */
@RestController
@RequestMapping("/api/v1/job-postings")
public class JobPostingQueryController {

    private final ListJobPostingsUseCase listJobPostingsUseCase;

    public JobPostingQueryController(ListJobPostingsUseCase listJobPostingsUseCase) {
        this.listJobPostingsUseCase = Objects.requireNonNull(listJobPostingsUseCase, "listJobPostingsUseCase must not be null");
    }

    /**
     * Lists private-sector job postings by publication date and seniority.
     */
    @GetMapping
    public List<JobPostingSummaryResponse> list(
            @RequestParam LocalDate since,
            @RequestParam JobCategory category,
            @RequestParam SeniorityLevel seniority
    ) {
        if (category != JobCategory.PRIVATE_SECTOR) {
            throw new IllegalArgumentException("Unsupported category: " + category);
        }

        return listJobPostingsUseCase.execute(since, seniority).stream()
                .map(posting -> new JobPostingSummaryResponse(
                        posting.getId(),
                        posting.getTitle(),
                        posting.getCompany(),
                        posting.getCanonicalUrl(),
                        posting.getPublishedAt()
                ))
                .toList();
    }
}
