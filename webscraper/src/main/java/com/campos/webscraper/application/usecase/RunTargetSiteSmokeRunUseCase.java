package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.orchestrator.CrawlJobDispatcher;
import com.campos.webscraper.application.queue.InFlightCrawlJobRegistry;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.repository.CrawlJobRepository;
import com.campos.webscraper.domain.repository.TargetSiteRepository;
import com.campos.webscraper.shared.TargetSiteActivationBlockedException;
import com.campos.webscraper.shared.TargetSiteNotFoundException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Component
public class RunTargetSiteSmokeRunUseCase {

    private final BootstrapCrawlJobFromTargetSiteUseCase bootstrapCrawlJobFromTargetSiteUseCase;
    private final CrawlJobDispatcher crawlJobDispatcher;
    private final CrawlJobRepository crawlJobRepository;
    private final TargetSiteRepository targetSiteRepository;
    private final InFlightCrawlJobRegistry inFlightCrawlJobRegistry;
    private final Clock clock;

    public RunTargetSiteSmokeRunUseCase(
            BootstrapCrawlJobFromTargetSiteUseCase bootstrapCrawlJobFromTargetSiteUseCase,
            CrawlJobDispatcher crawlJobDispatcher,
            CrawlJobRepository crawlJobRepository,
            TargetSiteRepository targetSiteRepository,
            InFlightCrawlJobRegistry inFlightCrawlJobRegistry,
            Clock clock
    ) {
        this.bootstrapCrawlJobFromTargetSiteUseCase = Objects.requireNonNull(
                bootstrapCrawlJobFromTargetSiteUseCase,
                "bootstrapCrawlJobFromTargetSiteUseCase must not be null"
        );
        this.crawlJobDispatcher = Objects.requireNonNull(crawlJobDispatcher, "crawlJobDispatcher must not be null");
        this.crawlJobRepository = Objects.requireNonNull(crawlJobRepository, "crawlJobRepository must not be null");
        this.targetSiteRepository = Objects.requireNonNull(targetSiteRepository, "targetSiteRepository must not be null");
        this.inFlightCrawlJobRegistry = Objects.requireNonNull(
                inFlightCrawlJobRegistry,
                "inFlightCrawlJobRegistry must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public TargetSiteSmokeRunResult execute(Long siteId) {
        Instant now = Instant.now(clock);
        assertTargetSiteReviewable(siteId);
        BootstrappedCrawlJob bootstrappedCrawlJob =
                bootstrapCrawlJobFromTargetSiteUseCase.executeForSmokeRun(siteId, now.plusSeconds(60));
        boolean claimed = false;
        if (bootstrappedCrawlJob.crawlJob().isSchedulerManaged()
                && bootstrappedCrawlJob.crawlJob().getId() != null) {
            claimed = inFlightCrawlJobRegistry.tryClaim(bootstrappedCrawlJob.crawlJob().getId());
        }
        if (bootstrappedCrawlJob.crawlJob().isSchedulerManaged()
                && bootstrappedCrawlJob.crawlJob().getId() != null
                && !claimed) {
            return new TargetSiteSmokeRunResult(
                    bootstrappedCrawlJob.crawlJob().getTargetSite().getId(),
                    bootstrappedCrawlJob.crawlJob().getTargetSite().getSiteCode(),
                    bootstrappedCrawlJob.crawlJob().getId(),
                    bootstrappedCrawlJob.bootstrapStatus(),
                    "SKIPPED_IN_FLIGHT",
                    null
            );
        }
        try {
            CrawlJobEntity runnableJob = materializeSmokeRunJob(bootstrappedCrawlJob.crawlJob(), now);
            advanceCanonicalScheduleToSmokeRunFloor(bootstrappedCrawlJob.crawlJob(), now);
            return new TargetSiteSmokeRunResult(
                    runnableJob.getTargetSite().getId(),
                    runnableJob.getTargetSite().getSiteCode(),
                    runnableJob.getId(),
                    bootstrappedCrawlJob.bootstrapStatus(),
                    "DISPATCHED",
                    crawlJobDispatcher.dispatch(runnableJob)
            );
        } finally {
            if (claimed
                    && bootstrappedCrawlJob.crawlJob().isSchedulerManaged()
                    && bootstrappedCrawlJob.crawlJob().getId() != null) {
                inFlightCrawlJobRegistry.release(bootstrappedCrawlJob.crawlJob().getId());
            }
        }
    }

    private CrawlJobEntity advanceCanonicalScheduleToSmokeRunFloor(CrawlJobEntity crawlJob, Instant now) {
        Instant smokeRunScheduleFloor = now.plusSeconds(60);
        if (!crawlJob.isSchedulerManaged() || !crawlJob.getScheduledAt().isBefore(smokeRunScheduleFloor)) {
            return crawlJob;
        }

        CrawlJobEntity deferredCanonicalJob = CrawlJobEntity.builder()
                .id(crawlJob.getId())
                .targetSite(crawlJob.getTargetSite())
                .scheduledAt(smokeRunScheduleFloor)
                .jobCategory(crawlJob.getJobCategory())
                .schedulerManaged(true)
                .createdAt(crawlJob.getCreatedAt())
                .build();
        return crawlJobRepository.save(deferredCanonicalJob);
    }

    private CrawlJobEntity materializeSmokeRunJob(CrawlJobEntity crawlJob, Instant now) {
        if (!crawlJob.isSchedulerManaged()) {
            return crawlJob;
        }

        CrawlJobEntity smokeRunJob = CrawlJobEntity.builder()
                .targetSite(crawlJob.getTargetSite())
                .scheduledAt(now)
                .jobCategory(crawlJob.getJobCategory())
                .schedulerManaged(false)
                .createdAt(now)
                .build();
        return crawlJobRepository.save(smokeRunJob);
    }

    private void assertTargetSiteReviewable(Long siteId) {
        if (targetSiteRepository.findById(siteId)
                .orElseThrow(() -> new TargetSiteNotFoundException(siteId))
                .getLegalStatus() == LegalStatus.SCRAPING_PROIBIDO) {
            throw new TargetSiteActivationBlockedException(siteId, java.util.List.of("target site is blocked by compliance"));
        }
    }
}
