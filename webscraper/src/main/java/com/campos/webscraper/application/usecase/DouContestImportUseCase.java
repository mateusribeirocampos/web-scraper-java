package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.strategy.DouApiContestScraperStrategy;
import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.domain.enums.DedupStatus;
import com.campos.webscraper.domain.enums.EducationLevel;
import com.campos.webscraper.domain.enums.GovernmentLevel;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.PublicContestPostingRepository;
import com.campos.webscraper.shared.ContestPostingFingerprintCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * End-to-end use case for importing DOU contests through the strategy and persisting them.
 */
@Component
public class DouContestImportUseCase {

    private final PublicContestPostingRepository publicContestPostingRepository;
    private final DouApiContestScraperStrategy strategy;
    private final ContestPostingFingerprintCalculator fingerprintCalculator;
    private final IdempotentPublicContestPersistenceService idempotentPersistenceService;

    @Autowired
    public DouContestImportUseCase(
            PublicContestPostingRepository publicContestPostingRepository,
            DouApiContestScraperStrategy strategy
    ) {
        this(
                publicContestPostingRepository,
                strategy,
                new ContestPostingFingerprintCalculator(),
                new IdempotentPublicContestPersistenceService(publicContestPostingRepository)
        );
    }

    public DouContestImportUseCase(
            PublicContestPostingRepository publicContestPostingRepository,
            DouApiContestScraperStrategy strategy,
            ContestPostingFingerprintCalculator fingerprintCalculator,
            IdempotentPublicContestPersistenceService idempotentPersistenceService
    ) {
        this.publicContestPostingRepository = Objects.requireNonNull(
                publicContestPostingRepository,
                "publicContestPostingRepository must not be null"
        );
        this.strategy = Objects.requireNonNull(strategy, "strategy must not be null");
        this.fingerprintCalculator = Objects.requireNonNull(
                fingerprintCalculator,
                "fingerprintCalculator must not be null"
        );
        this.idempotentPersistenceService = Objects.requireNonNull(
                idempotentPersistenceService,
                "idempotentPersistenceService must not be null"
        );
    }

    /**
     * Executes the full DOU import slice and persists the normalized contest postings.
     */
    public List<PublicContestPostingEntity> execute(
            TargetSiteEntity targetSite,
            CrawlExecutionEntity crawlExecution,
            ScrapeCommand command
    ) {
        Objects.requireNonNull(targetSite, "targetSite must not be null");
        Objects.requireNonNull(crawlExecution, "crawlExecution must not be null");
        Objects.requireNonNull(command, "command must not be null");

        List<PublicContestPostingEntity> items = strategy.scrape(command).items();

        List<PublicContestPostingEntity> enriched = items.stream()
                .map(item -> enrich(item, targetSite, crawlExecution))
                .toList();

        return idempotentPersistenceService.persist(enriched);
    }

    private PublicContestPostingEntity enrich(
            PublicContestPostingEntity item,
            TargetSiteEntity targetSite,
            CrawlExecutionEntity crawlExecution
    ) {
        PublicContestPostingEntity enriched = PublicContestPostingEntity.builder()
                .crawlExecution(crawlExecution)
                .targetSite(targetSite)
                .externalId(item.getExternalId())
                .canonicalUrl(item.getCanonicalUrl())
                .contestName(item.getContestName())
                .organizer(item.getOrganizer())
                .positionTitle(item.getPositionTitle())
                .governmentLevel(item.getGovernmentLevel() != null ? item.getGovernmentLevel() : GovernmentLevel.FEDERAL)
                .state(item.getState())
                .educationLevel(item.getEducationLevel() != null ? item.getEducationLevel() : EducationLevel.SUPERIOR)
                .numberOfVacancies(item.getNumberOfVacancies())
                .baseSalary(item.getBaseSalary())
                .salaryDescription(item.getSalaryDescription())
                .editalUrl(item.getEditalUrl())
                .publishedAt(item.getPublishedAt())
                .registrationStartDate(item.getRegistrationStartDate())
                .registrationEndDate(item.getRegistrationEndDate())
                .examDate(item.getExamDate())
                .contestStatus(item.getContestStatus() != null ? item.getContestStatus() : ContestStatus.OPEN)
                .dedupStatus(DedupStatus.NEW)
                .payloadJson(item.getPayloadJson())
                .createdAt(item.getCreatedAt() != null ? item.getCreatedAt() : Instant.now())
                .updatedAt(item.getUpdatedAt())
                .build();

        String fingerprint = fingerprintCalculator.calculate(enriched);

        return PublicContestPostingEntity.builder()
                .crawlExecution(enriched.getCrawlExecution())
                .targetSite(enriched.getTargetSite())
                .externalId(enriched.getExternalId())
                .canonicalUrl(enriched.getCanonicalUrl())
                .contestName(enriched.getContestName())
                .organizer(enriched.getOrganizer())
                .positionTitle(enriched.getPositionTitle())
                .governmentLevel(enriched.getGovernmentLevel())
                .state(enriched.getState())
                .educationLevel(enriched.getEducationLevel())
                .numberOfVacancies(enriched.getNumberOfVacancies())
                .baseSalary(enriched.getBaseSalary())
                .salaryDescription(enriched.getSalaryDescription())
                .editalUrl(enriched.getEditalUrl())
                .publishedAt(enriched.getPublishedAt())
                .registrationStartDate(enriched.getRegistrationStartDate())
                .registrationEndDate(enriched.getRegistrationEndDate())
                .examDate(enriched.getExamDate())
                .contestStatus(enriched.getContestStatus())
                .fingerprintHash(fingerprint)
                .dedupStatus(enriched.getDedupStatus())
                .payloadJson(enriched.getPayloadJson())
                .createdAt(enriched.getCreatedAt())
                .updatedAt(enriched.getUpdatedAt())
                .build();
    }
}
