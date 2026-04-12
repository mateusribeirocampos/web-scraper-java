package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.application.usecase.TargetSiteSmokeRunResult;

import java.util.ArrayList;
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
        List<String> blockers = new ArrayList<>();

        TargetSiteSmokeRunResult smokeRun = workflow.smokeRun();
        if (smokeRun != null && "BLOCKED_BY_COMPLIANCE".equals(smokeRun.smokeRunStatus())) {
            blockers.add("smoke run was blocked by compliance — legal review still required");
            return List.copyOf(blockers);
        }

        if (executionSummary == null) {
            blockers.add("no execution recorded for this source");
            return List.copyOf(blockers);
        }

        if (!"SUCCEEDED".equals(executionSummary.status())) {
            blockers.add("last execution did not succeed (status: " + executionSummary.status() + ")");
        }

        if (recentPostingsCount == 0) {
            blockers.add("no postings collected in the observed window");
        }

        return List.copyOf(blockers);
    }
}
