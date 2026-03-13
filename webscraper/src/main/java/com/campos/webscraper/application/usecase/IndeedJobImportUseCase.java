package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.strategy.IndeedApiJobScraperStrategy;
import com.campos.webscraper.domain.enums.DedupStatus;
import com.campos.webscraper.domain.enums.JobContractType;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.JobPostingRepository;
import com.campos.webscraper.shared.JobPostingFingerprintCalculator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * End-to-end use case for importing Indeed jobs through the strategy and persisting them.
 */
@Component
public class IndeedJobImportUseCase {

    private final JobPostingRepository jobPostingRepository;
    private final IndeedApiJobScraperStrategy strategy;
    private final JobPostingFingerprintCalculator fingerprintCalculator;
    private final IdempotentJobPostingPersistenceService idempotentPersistenceService;

    public IndeedJobImportUseCase(JobPostingRepository jobPostingRepository, IndeedApiJobScraperStrategy strategy) {
        this(
                jobPostingRepository,
                strategy,
                new JobPostingFingerprintCalculator(),
                new IdempotentJobPostingPersistenceService(jobPostingRepository)
        );
    }

    public IndeedJobImportUseCase(
            JobPostingRepository jobPostingRepository,
            IndeedApiJobScraperStrategy strategy,
            JobPostingFingerprintCalculator fingerprintCalculator,
            IdempotentJobPostingPersistenceService idempotentPersistenceService
    ) {
        this.jobPostingRepository = Objects.requireNonNull(jobPostingRepository, "jobPostingRepository must not be null");
        this.strategy = Objects.requireNonNull(strategy, "strategy must not be null");
        this.fingerprintCalculator = Objects.requireNonNull(fingerprintCalculator, "fingerprintCalculator must not be null");
        this.idempotentPersistenceService = Objects.requireNonNull(
                idempotentPersistenceService,
                "idempotentPersistenceService must not be null"
        );
    }

    /**
     * Executes the full Indeed import slice and persists the normalized postings.
     */
    public List<JobPostingEntity> execute(
            TargetSiteEntity targetSite,
            CrawlExecutionEntity crawlExecution,
            ScrapeCommand command
    ) {
        Objects.requireNonNull(targetSite, "targetSite must not be null");
        Objects.requireNonNull(crawlExecution, "crawlExecution must not be null");
        Objects.requireNonNull(command, "command must not be null");

        List<JobPostingEntity> items = strategy.scrape(command).items();

        List<JobPostingEntity> enriched = items.stream()
                .map(item -> enrich(item, targetSite, crawlExecution))
                .toList();

        return idempotentPersistenceService.persist(enriched);
    }

    private JobPostingEntity enrich(
            JobPostingEntity item,
            TargetSiteEntity targetSite,
            CrawlExecutionEntity crawlExecution
    ) {
        JobPostingEntity enriched = JobPostingEntity.builder()
                .crawlExecution(crawlExecution)
                .targetSite(targetSite)
                .externalId(item.getExternalId())
                .canonicalUrl(item.getCanonicalUrl())
                .title(item.getTitle())
                .company(item.getCompany())
                .location(item.getLocation())
                .remote(item.isRemote())
                .contractType(JobContractType.CLT)
                .seniority(item.getSeniority())
                .salaryRange(item.getSalaryRange())
                .techStackTags(item.getTechStackTags())
                .description(item.getDescription())
                .publishedAt(item.getPublishedAt())
                .applicationDeadline(item.getApplicationDeadline())
                .dedupStatus(DedupStatus.NEW)
                .payloadJson(item.getPayloadJson())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();

        String fingerprint = fingerprintCalculator.calculate(enriched);

        return JobPostingEntity.builder()
                .crawlExecution(enriched.getCrawlExecution())
                .targetSite(enriched.getTargetSite())
                .externalId(enriched.getExternalId())
                .canonicalUrl(enriched.getCanonicalUrl())
                .title(enriched.getTitle())
                .company(enriched.getCompany())
                .location(enriched.getLocation())
                .remote(enriched.isRemote())
                .contractType(enriched.getContractType())
                .seniority(enriched.getSeniority())
                .salaryRange(enriched.getSalaryRange())
                .techStackTags(enriched.getTechStackTags())
                .description(enriched.getDescription())
                .publishedAt(enriched.getPublishedAt())
                .applicationDeadline(enriched.getApplicationDeadline())
                .fingerprintHash(fingerprint)
                .dedupStatus(enriched.getDedupStatus())
                .payloadJson(enriched.getPayloadJson())
                .createdAt(enriched.getCreatedAt())
                .updatedAt(enriched.getUpdatedAt())
                .build();
    }
}
