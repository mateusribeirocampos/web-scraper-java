package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.application.usecase.TargetSiteSmokeRunResult;

import java.util.List;
import java.util.Objects;

public record OnboardingOperationalCheckResult(
        String profileKey,
        BootstrappedOnboardingWorkflowResult workflow,
        OnboardingOperationalCheckExecutionSummary executionSummary,
        int recentPostingsCount,
        List<OnboardingRecentPostingSample> recentPostingsSample
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

    public boolean activationReady() {
        return activationBlockers().isEmpty();
    }

    public List<String> activationBlockers() {
        TargetSiteSmokeRunResult smokeRun = workflow.smokeRun();
        if (smokeRun != null && "BLOCKED_BY_COMPLIANCE".equals(smokeRun.smokeRunStatus())) {
            return List.of("smoke run was blocked by compliance — legal review still required");
        }

        if (executionSummary == null) {
            return List.of("no execution recorded for this source");
        }

        if (!"SUCCEEDED".equals(executionSummary.status())) {
            return List.of("last execution did not succeed (status: " + executionSummary.status() + ")");
        }

        if (recentPostingsCount == 0) {
            return List.of("execution succeeded but no postings found in the requested window — source may only contain historical items");
        }

        return List.of();
    }
}
