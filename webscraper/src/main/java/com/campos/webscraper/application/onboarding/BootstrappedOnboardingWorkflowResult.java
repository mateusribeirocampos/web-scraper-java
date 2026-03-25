package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.application.usecase.TargetSiteSmokeRunResult;
import com.campos.webscraper.application.usecase.BootstrappedCrawlJob;

import java.util.Objects;

public record BootstrappedOnboardingWorkflowResult(
        String profileKey,
        BootstrappedTargetSite targetSite,
        BootstrappedCrawlJob crawlJob,
        boolean smokeRunRequested,
        TargetSiteSmokeRunResult smokeRun
) {

    public BootstrappedOnboardingWorkflowResult {
        Objects.requireNonNull(profileKey, "profileKey must not be null");
        Objects.requireNonNull(targetSite, "targetSite must not be null");
        Objects.requireNonNull(crawlJob, "crawlJob must not be null");
    }
}
