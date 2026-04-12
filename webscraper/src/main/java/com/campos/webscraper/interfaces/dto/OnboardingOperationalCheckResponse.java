package com.campos.webscraper.interfaces.dto;

import java.time.Instant;
import java.util.List;

public record OnboardingOperationalCheckResponse(
        String profileKey,
        String targetSiteBootstrapStatus,
        Long siteId,
        String siteCode,
        String crawlJobBootstrapStatus,
        Long crawlJobId,
        Instant crawlJobScheduledAt,
        boolean smokeRunRequested,
        String smokeRunStatus,
        String smokeRunDispatchStatus,
        Long smokeRunJobId,
        OnboardingOperationalCheckExecutionResponse executionSummary,
        int recentPostingsCount,
        List<JobPostingSummaryResponse> recentPostingsSample,
        boolean activationReady,
        List<String> activationBlockers
) {
}
