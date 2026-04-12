package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.strategy.GupyJobScraperStrategy;
import com.campos.webscraper.domain.enums.DedupStatus;
import com.campos.webscraper.domain.enums.JobContractType;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.JobPostingRepository;
import com.campos.webscraper.shared.JobPostingFingerprintCalculator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * End-to-end use case for importing Gupy jobs and persisting them idempotently.
 */
@Component
public class GupyJobImportUseCase {

    private final JobPostingRepository jobPostingRepository;
    private final GupyJobScraperStrategy strategy;
    private final JobPostingFingerprintCalculator fingerprintCalculator;
    private final IdempotentJobPostingPersistenceService idempotentPersistenceService;

    public GupyJobImportUseCase(
            JobPostingRepository jobPostingRepository,
            GupyJobScraperStrategy strategy,
            JobPostingFingerprintCalculator fingerprintCalculator,
            IdempotentJobPostingPersistenceService idempotentPersistenceService
    ) {
        this.jobPostingRepository = Objects.requireNonNull(jobPostingRepository, "jobPostingRepository must not be null");
        this.strategy = Objects.requireNonNull(strategy, "strategy must not be null");
        this.fingerprintCalculator = Objects.requireNonNull(fingerprintCalculator, "fingerprintCalculator must not be null");
        this.idempotentPersistenceService = Objects.requireNonNull(
                idempotentPersistenceService, "idempotentPersistenceService must not be null");
    }

    public List<JobPostingEntity> execute(
            TargetSiteEntity targetSite,
            CrawlExecutionEntity crawlExecution,
            ScrapeCommand command
    ) {
        Objects.requireNonNull(targetSite, "targetSite must not be null");
        Objects.requireNonNull(crawlExecution, "crawlExecution must not be null");
        Objects.requireNonNull(command, "command must not be null");

        ScrapeResult<JobPostingEntity> result = strategy.scrape(command);
        if (!result.success()) {
            throw new IllegalStateException("Gupy scrape failed: " + result.errorMessage());
        }

        List<JobPostingEntity> enriched = result.items().stream()
                .map(item -> enrich(item, targetSite, crawlExecution))
                .toList();

        return idempotentPersistenceService.persist(enriched);
    }

    private JobPostingEntity enrich(
            JobPostingEntity item,
            TargetSiteEntity targetSite,
            CrawlExecutionEntity crawlExecution
    ) {
        JobPostingEntity withContext = JobPostingEntity.builder()
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
                .techStackTags(item.getTechStackTags())
                .description(item.getDescription())
                .publishedAt(item.getPublishedAt())
                .applicationDeadline(item.getApplicationDeadline())
                .dedupStatus(DedupStatus.NEW)
                .payloadJson(item.getPayloadJson())
                .createdAt(item.getCreatedAt())
                .build();

        String fingerprint = fingerprintCalculator.calculate(withContext);

        return JobPostingEntity.builder()
                .crawlExecution(withContext.getCrawlExecution())
                .targetSite(withContext.getTargetSite())
                .externalId(withContext.getExternalId())
                .canonicalUrl(withContext.getCanonicalUrl())
                .title(withContext.getTitle())
                .company(withContext.getCompany())
                .location(withContext.getLocation())
                .remote(withContext.isRemote())
                .contractType(withContext.getContractType())
                .seniority(withContext.getSeniority())
                .techStackTags(withContext.getTechStackTags())
                .description(withContext.getDescription())
                .publishedAt(withContext.getPublishedAt())
                .applicationDeadline(withContext.getApplicationDeadline())
                .fingerprintHash(fingerprint)
                .dedupStatus(withContext.getDedupStatus())
                .payloadJson(withContext.getPayloadJson())
                .createdAt(withContext.getCreatedAt())
                .build();
    }
}
