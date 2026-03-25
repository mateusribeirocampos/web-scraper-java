package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.application.usecase.BootstrapCrawlJobFromTargetSiteUseCase;
import com.campos.webscraper.application.usecase.BootstrappedCrawlJob;
import com.campos.webscraper.application.usecase.RunTargetSiteSmokeRunUseCase;
import com.campos.webscraper.application.usecase.TargetSiteSmokeRunResult;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.repository.CrawlJobRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class BootstrapOnboardingProfileWorkflowUseCase {

    private final BootstrapTargetSiteFromProfileUseCase bootstrapTargetSiteFromProfileUseCase;
    private final BootstrapCrawlJobFromTargetSiteUseCase bootstrapCrawlJobFromTargetSiteUseCase;
    private final RunTargetSiteSmokeRunUseCase runTargetSiteSmokeRunUseCase;
    private final CrawlJobRepository crawlJobRepository;

    public BootstrapOnboardingProfileWorkflowUseCase(
            BootstrapTargetSiteFromProfileUseCase bootstrapTargetSiteFromProfileUseCase,
            BootstrapCrawlJobFromTargetSiteUseCase bootstrapCrawlJobFromTargetSiteUseCase,
            RunTargetSiteSmokeRunUseCase runTargetSiteSmokeRunUseCase,
            CrawlJobRepository crawlJobRepository
    ) {
        this.bootstrapTargetSiteFromProfileUseCase = Objects.requireNonNull(
                bootstrapTargetSiteFromProfileUseCase,
                "bootstrapTargetSiteFromProfileUseCase must not be null"
        );
        this.bootstrapCrawlJobFromTargetSiteUseCase = Objects.requireNonNull(
                bootstrapCrawlJobFromTargetSiteUseCase,
                "bootstrapCrawlJobFromTargetSiteUseCase must not be null"
        );
        this.runTargetSiteSmokeRunUseCase = Objects.requireNonNull(
                runTargetSiteSmokeRunUseCase,
                "runTargetSiteSmokeRunUseCase must not be null"
        );
        this.crawlJobRepository = Objects.requireNonNull(crawlJobRepository, "crawlJobRepository must not be null");
    }

    public BootstrappedOnboardingWorkflowResult execute(String profileKey, boolean smokeRunRequested) {
        BootstrappedTargetSite targetSite = bootstrapTargetSiteFromProfileUseCase.execute(profileKey);
        BootstrappedCrawlJob crawlJob = bootstrapCrawlJobFromTargetSiteUseCase.execute(targetSite.targetSite().getId());
        TargetSiteSmokeRunResult smokeRun = smokeRunRequested
                ? runTargetSiteSmokeRunUseCase.execute(targetSite.targetSite().getId())
                : null;
        BootstrappedCrawlJob refreshedCrawlJob = smokeRunRequested
                ? refreshCanonicalCrawlJob(targetSite.targetSite().getId(), crawlJob)
                : crawlJob;
        return new BootstrappedOnboardingWorkflowResult(
                profileKey,
                targetSite,
                refreshedCrawlJob,
                smokeRunRequested,
                smokeRun
        );
    }

    private BootstrappedCrawlJob refreshCanonicalCrawlJob(Long siteId, BootstrappedCrawlJob bootstrappedCrawlJob) {
        CrawlJobEntity refreshed = crawlJobRepository.findFirstByTargetSiteIdAndSchedulerManagedTrueOrderByCreatedAtAsc(siteId)
                .orElse(bootstrappedCrawlJob.crawlJob());
        return new BootstrappedCrawlJob(bootstrappedCrawlJob.bootstrapStatus(), refreshed);
    }
}
