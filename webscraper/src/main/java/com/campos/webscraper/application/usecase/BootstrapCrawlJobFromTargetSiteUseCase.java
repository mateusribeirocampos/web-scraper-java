package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.onboarding.BootstrapStatus;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.CrawlJobRepository;
import com.campos.webscraper.domain.repository.TargetSiteRepository;
import com.campos.webscraper.shared.TargetSiteNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Component
public class BootstrapCrawlJobFromTargetSiteUseCase {

    private final TargetSiteRepository targetSiteRepository;
    private final CrawlJobRepository crawlJobRepository;
    private final Clock clock;

    public BootstrapCrawlJobFromTargetSiteUseCase(
            TargetSiteRepository targetSiteRepository,
            CrawlJobRepository crawlJobRepository,
            Clock clock
    ) {
        this.targetSiteRepository = Objects.requireNonNull(targetSiteRepository, "targetSiteRepository must not be null");
        this.crawlJobRepository = Objects.requireNonNull(crawlJobRepository, "crawlJobRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public BootstrappedCrawlJob execute(Long siteId) {
        TargetSiteEntity targetSite = targetSiteRepository.findById(siteId)
                .orElseThrow(() -> new TargetSiteNotFoundException(siteId));

        Optional<CrawlJobEntity> existing = crawlJobRepository.findFirstByTargetSiteIdAndSchedulerManagedTrueOrderByCreatedAtAsc(siteId);
        Instant now = Instant.now(clock);

        BootstrapStatus bootstrapStatus = existing.isPresent() ? BootstrapStatus.UPDATED : BootstrapStatus.CREATED;
        CrawlJobEntity materialized = existing
                .map(persisted -> merge(targetSite, persisted))
                .orElseGet(() -> create(targetSite, now));

        try {
            CrawlJobEntity saved = crawlJobRepository.save(materialized);
            return new BootstrappedCrawlJob(bootstrapStatus, saved);
        } catch (DataIntegrityViolationException exception) {
            if (bootstrapStatus != BootstrapStatus.CREATED) {
                throw exception;
            }
            CrawlJobEntity persisted = crawlJobRepository.findFirstByTargetSiteIdAndSchedulerManagedTrueOrderByCreatedAtAsc(siteId)
                    .orElseThrow(() -> exception);
            return new BootstrappedCrawlJob(BootstrapStatus.UPDATED, persisted);
        }
    }

    private static CrawlJobEntity create(TargetSiteEntity targetSite, Instant now) {
        return CrawlJobEntity.builder()
                .targetSite(targetSite)
                .scheduledAt(now)
                .jobCategory(null)
                .schedulerManaged(true)
                .createdAt(now)
                .build();
    }

    private static CrawlJobEntity merge(TargetSiteEntity targetSite, CrawlJobEntity existing) {
        return CrawlJobEntity.builder()
                .id(existing.getId())
                .targetSite(targetSite)
                .scheduledAt(existing.getScheduledAt())
                .jobCategory(null)
                .schedulerManaged(existing.isSchedulerManaged())
                .createdAt(existing.getCreatedAt())
                .build();
    }
}
