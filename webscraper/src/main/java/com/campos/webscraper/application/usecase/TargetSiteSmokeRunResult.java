package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.onboarding.BootstrapStatus;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;

import java.util.Objects;

public record TargetSiteSmokeRunResult(
        Long siteId,
        String siteCode,
        Long jobId,
        BootstrapStatus bootstrapStatus,
        String smokeRunStatus,
        CrawlExecutionStatus dispatchStatus
) {

    public TargetSiteSmokeRunResult {
        Objects.requireNonNull(siteId, "siteId must not be null");
        Objects.requireNonNull(siteCode, "siteCode must not be null");
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(bootstrapStatus, "bootstrapStatus must not be null");
        Objects.requireNonNull(smokeRunStatus, "smokeRunStatus must not be null");
    }
}
