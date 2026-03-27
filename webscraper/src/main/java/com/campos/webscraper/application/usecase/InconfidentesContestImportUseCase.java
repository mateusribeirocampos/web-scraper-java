package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.strategy.InconfidentesContestScraperStrategy;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * End-to-end use case for importing municipal public contests from Inconfidentes.
 */
@Component
public class InconfidentesContestImportUseCase {

    private final PublicContestPostingRepository publicContestPostingRepository;
    private final InconfidentesContestScraperStrategy strategy;
    private final ContestPostingFingerprintCalculator fingerprintCalculator;
    private final IdempotentPublicContestPersistenceService idempotentPersistenceService;

    @Autowired
    public InconfidentesContestImportUseCase(
            PublicContestPostingRepository publicContestPostingRepository,
            InconfidentesContestScraperStrategy strategy
    ) {
        this(
                publicContestPostingRepository,
                strategy,
                new ContestPostingFingerprintCalculator(),
                new IdempotentPublicContestPersistenceService(publicContestPostingRepository)
        );
    }

    public InconfidentesContestImportUseCase(
            PublicContestPostingRepository publicContestPostingRepository,
            InconfidentesContestScraperStrategy strategy,
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

    public List<PublicContestPostingEntity> execute(
            TargetSiteEntity targetSite,
            CrawlExecutionEntity crawlExecution,
            ScrapeCommand command
    ) {
        Objects.requireNonNull(targetSite, "targetSite must not be null");
        Objects.requireNonNull(crawlExecution, "crawlExecution must not be null");
        Objects.requireNonNull(command, "command must not be null");

        var scrapeResult = strategy.scrape(command);
        if (!scrapeResult.success()) {
            throw new IllegalStateException("Inconfidentes scrape failed: " + scrapeResult.errorMessage());
        }

        List<PublicContestPostingEntity> items = scrapeResult.items();

        List<PublicContestPostingEntity> enriched = items.stream()
                .map(item -> enrich(item, targetSite, crawlExecution))
                .toList();

        return persistWithMutableFieldRefresh(enriched);
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
                .governmentLevel(item.getGovernmentLevel() != null ? item.getGovernmentLevel() : GovernmentLevel.MUNICIPAL)
                .state(item.getState())
                .educationLevel(item.getEducationLevel() != null ? item.getEducationLevel() : EducationLevel.UNKNOWN)
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

    private List<PublicContestPostingEntity> persistWithMutableFieldRefresh(List<PublicContestPostingEntity> candidates) {
        List<PublicContestPostingEntity> distinctCandidates = deduplicateByStableExternalId(candidates);
        Map<String, PublicContestPostingEntity> existingByFingerprint = new LinkedHashMap<>();
        List<PublicContestPostingEntity> newCandidates = distinctCandidates.stream()
                .filter(candidate -> resolveExisting(candidate, existingByFingerprint) == null)
                .toList();

        Map<String, PublicContestPostingEntity> persistedNewByFingerprint = idempotentPersistenceService.persist(newCandidates).stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getFingerprintHash(), item), Map::putAll);

        return distinctCandidates.stream()
                .map(candidate -> {
                    PublicContestPostingEntity existing = existingByFingerprint.get(candidate.getFingerprintHash());
                    if (existing == null) {
                        return persistedNewByFingerprint.get(candidate.getFingerprintHash());
                    }
                    if (!hasMutableChanges(existing, candidate)) {
                        return existing;
                    }
                    return publicContestPostingRepository.save(mergeExisting(existing, candidate));
                })
                .toList();
    }

    private List<PublicContestPostingEntity> deduplicateByStableExternalId(List<PublicContestPostingEntity> candidates) {
        Map<String, PublicContestPostingEntity> latestByStableId = new LinkedHashMap<>();
        for (PublicContestPostingEntity candidate : candidates) {
            latestByStableId.put(stableExternalKey(candidate), candidate);
        }
        return List.copyOf(latestByStableId.values());
    }

    private String stableExternalKey(PublicContestPostingEntity candidate) {
        Long targetSiteId = candidate.getTargetSite() == null ? null : candidate.getTargetSite().getId();
        return "%s|%s".formatted(targetSiteId, candidate.getExternalId());
    }

    private PublicContestPostingEntity resolveExisting(
            PublicContestPostingEntity candidate,
            Map<String, PublicContestPostingEntity> existingByFingerprint
    ) {
        PublicContestPostingEntity existingByStableId = publicContestPostingRepository
                .findByTargetSiteAndExternalId(candidate.getTargetSite(), candidate.getExternalId())
                .map(existing -> {
                    existingByFingerprint.put(candidate.getFingerprintHash(), existing);
                    return existing;
                })
                .orElse(null);
        if (existingByStableId != null) {
            return existingByStableId;
        }

        return publicContestPostingRepository.findByFingerprintHash(candidate.getFingerprintHash())
                .map(existing -> {
                    existingByFingerprint.put(candidate.getFingerprintHash(), existing);
                    return existing;
                })
                .orElse(null);
    }

    private PublicContestPostingEntity mergeExisting(
            PublicContestPostingEntity existing,
            PublicContestPostingEntity candidate
    ) {
        return PublicContestPostingEntity.builder()
                .id(existing.getId())
                .crawlExecution(candidate.getCrawlExecution())
                .targetSite(candidate.getTargetSite())
                .externalId(existing.getExternalId())
                .canonicalUrl(candidate.getCanonicalUrl())
                .contestName(candidate.getContestName())
                .organizer(candidate.getOrganizer())
                .positionTitle(candidate.getPositionTitle())
                .governmentLevel(candidate.getGovernmentLevel())
                .state(candidate.getState())
                .educationLevel(candidate.getEducationLevel())
                .numberOfVacancies(candidate.getNumberOfVacancies())
                .baseSalary(candidate.getBaseSalary())
                .salaryDescription(candidate.getSalaryDescription())
                .editalUrl(candidate.getEditalUrl())
                .publishedAt(candidate.getPublishedAt())
                .registrationStartDate(candidate.getRegistrationStartDate())
                .registrationEndDate(candidate.getRegistrationEndDate())
                .examDate(candidate.getExamDate())
                .contestStatus(candidate.getContestStatus())
                .fingerprintHash(existing.getFingerprintHash())
                .dedupStatus(DedupStatus.UPDATED)
                .payloadJson(candidate.getPayloadJson())
                .createdAt(existing.getCreatedAt())
                .updatedAt(Instant.now())
                .build();
    }

    private boolean hasMutableChanges(
            PublicContestPostingEntity existing,
            PublicContestPostingEntity candidate
    ) {
        return !Objects.equals(existing.getContestName(), candidate.getContestName())
                || !Objects.equals(existing.getCanonicalUrl(), candidate.getCanonicalUrl())
                || !Objects.equals(existing.getOrganizer(), candidate.getOrganizer())
                || !Objects.equals(existing.getPositionTitle(), candidate.getPositionTitle())
                || !Objects.equals(existing.getGovernmentLevel(), candidate.getGovernmentLevel())
                || !Objects.equals(existing.getState(), candidate.getState())
                || !Objects.equals(existing.getEducationLevel(), candidate.getEducationLevel())
                || !Objects.equals(existing.getNumberOfVacancies(), candidate.getNumberOfVacancies())
                || !Objects.equals(existing.getBaseSalary(), candidate.getBaseSalary())
                || !Objects.equals(existing.getSalaryDescription(), candidate.getSalaryDescription())
                || !Objects.equals(existing.getEditalUrl(), candidate.getEditalUrl())
                || !Objects.equals(existing.getPublishedAt(), candidate.getPublishedAt())
                || !Objects.equals(existing.getRegistrationStartDate(), candidate.getRegistrationStartDate())
                || !Objects.equals(existing.getRegistrationEndDate(), candidate.getRegistrationEndDate())
                || !Objects.equals(existing.getExamDate(), candidate.getExamDate())
                || !Objects.equals(existing.getContestStatus(), candidate.getContestStatus())
                || !Objects.equals(existing.getPayloadJson(), candidate.getPayloadJson());
    }
}
