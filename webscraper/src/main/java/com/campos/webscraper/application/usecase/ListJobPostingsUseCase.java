package com.campos.webscraper.application.usecase;

import com.campos.webscraper.domain.enums.SeniorityLevel;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.repository.JobPostingRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Query use case for private-sector job postings.
 */
@Component
public class ListJobPostingsUseCase {

    private final JobPostingRepository jobPostingRepository;

    public ListJobPostingsUseCase(JobPostingRepository jobPostingRepository) {
        this.jobPostingRepository = Objects.requireNonNull(jobPostingRepository, "jobPostingRepository must not be null");
    }

    /**
     * Returns postings on or after the given date filtered by seniority.
     */
    public List<JobPostingEntity> execute(LocalDate since, SeniorityLevel seniority) {
        return jobPostingRepository.findByPublishedAtGreaterThanEqualAndSeniorityOrderByPublishedAtDesc(since, seniority);
    }
}
