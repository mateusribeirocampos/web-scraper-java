package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.application.usecase.TargetSiteSmokeRunResult;
import com.campos.webscraper.domain.model.JobPostingEntity;

import java.util.List;
import java.util.Objects;

public record OnboardingOperationalCheckResult(
        String profileKey,
        BootstrappedOnboardingWorkflowResult workflow,
        OnboardingOperationalCheckExecutionSummary executionSummary,
        int recentPostingsCount,
        List<JobPostingEntity> recentPostingsSample
) {

    public OnboardingOperationalCheckResult {
        Objects.requireNonNull(profileKey, "profileKey must not be null");
        Objects.requireNonNull(workflow, "workflow must not be null");
        recentPostingsSample = List.copyOf(recentPostingsSample == null ? List.of() : recentPostingsSample);
    }

    public boolean smokeRunRequested() {
        return workflow.smokeRunRequested();
    }

    public TargetSiteSmokeRunResult smokeRun() {
        return workflow.smokeRun();
    }
}
