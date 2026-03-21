package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.usecase.JobPostingSearchProfileMatcher;
import com.campos.webscraper.application.usecase.ListJobPostingsUseCase;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.JobPostingSearchProfile;
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
    private final JobPostingSearchProfileMatcher jobPostingSearchProfileMatcher;

    public JobPostingQueryController(
            ListJobPostingsUseCase listJobPostingsUseCase,
            JobPostingSearchProfileMatcher jobPostingSearchProfileMatcher
    ) {
        this.listJobPostingsUseCase = Objects.requireNonNull(listJobPostingsUseCase, "listJobPostingsUseCase must not be null");
        this.jobPostingSearchProfileMatcher = Objects.requireNonNull(
                jobPostingSearchProfileMatcher,
                "jobPostingSearchProfileMatcher must not be null"
        );
    }

    /**
     * Lists recent private-sector job postings. Recency is mandatory either via "since" or the default "daysBack".
     */
    @GetMapping
    public List<JobPostingSummaryResponse> list(
            @RequestParam(required = false) LocalDate since,
            @RequestParam(defaultValue = "60") int daysBack,
            @RequestParam JobCategory category,
            @RequestParam(defaultValue = "JAVA_JUNIOR_BACKEND") JobPostingSearchProfile profile,
            @RequestParam(required = false) SeniorityLevel seniority
    ) {
        if (category != JobCategory.PRIVATE_SECTOR) {
            throw new IllegalArgumentException("Unsupported category: " + category);
        }

        if (daysBack <= 0) {
            throw new IllegalArgumentException("daysBack must be greater than zero");
        }

        LocalDate effectiveSince = since != null ? since : LocalDate.now().minusDays(daysBack);

        return listJobPostingsUseCase.execute(effectiveSince, seniority).stream()
                .filter(posting -> jobPostingSearchProfileMatcher.matches(posting, profile))
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
