package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.strategy.WorkdayJobScraperStrategy;
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

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * End-to-end use case for importing Workday jobs through the strategy and persisting them.
 */
@Component
public class WorkdayJobImportUseCase {

    private final JobPostingRepository jobPostingRepository;
    private final WorkdayJobScraperStrategy strategy;
    private final JobPostingFingerprintCalculator fingerprintCalculator;
    private final IdempotentJobPostingPersistenceService idempotentPersistenceService;

    public WorkdayJobImportUseCase(
            JobPostingRepository jobPostingRepository,
            WorkdayJobScraperStrategy strategy,
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
            throw new IllegalStateException("Workday scrape failed: " + result.errorMessage());
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
        JobPostingEntity enriched = JobPostingEntity.builder()
                .crawlExecution(crawlExecution)
                .targetSite(targetSite)
                .externalId(item.getExternalId())
                .canonicalUrl(item.getCanonicalUrl())
                .title(item.getTitle())
                .company(item.getCompany())
                .location(item.getLocation())
                .remote(item.isRemote())
                .contractType(item.getContractType() != null ? item.getContractType() : JobContractType.UNKNOWN)
                .seniority(item.getSeniority())
                .salaryRange(item.getSalaryRange())
                .techStackTags(item.getTechStackTags())
                .description(item.getDescription())
                .publishedAt(resolvePublishedAt(item, targetSite, crawlExecution))
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

    private LocalDate resolvePublishedAt(
            JobPostingEntity item,
            TargetSiteEntity targetSite,
            CrawlExecutionEntity crawlExecution
    ) {
        if (item.getPublishedAt() != null) {
            return item.getPublishedAt();
        }
        String externalId = item.getExternalId();
        if (externalId != null && !externalId.isBlank()) {
            return jobPostingRepository.findByTargetSiteAndExternalIdOrderByPublishedAtDescCreatedAtDesc(
                            targetSite,
                            externalId
                    ).stream()
                    .map(JobPostingEntity::getPublishedAt)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(crawlExecution.getStartedAt().atOffset(ZoneOffset.UTC).toLocalDate());
        }
        return crawlExecution.getStartedAt().atOffset(ZoneOffset.UTC).toLocalDate();
    }
}
