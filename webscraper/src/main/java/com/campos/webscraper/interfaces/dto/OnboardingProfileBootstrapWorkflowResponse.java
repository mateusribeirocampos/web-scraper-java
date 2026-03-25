package com.campos.webscraper.interfaces.dto;

import java.time.Instant;

public record OnboardingProfileBootstrapWorkflowResponse(
        String profileKey,
        String targetSiteBootstrapStatus,
        Long siteId,
        String siteCode,
        boolean enabled,
        String legalStatus,
        String crawlJobBootstrapStatus,
        Long crawlJobId,
        boolean schedulerManaged,
        Instant scheduledAt,
        boolean smokeRunRequested,
        String smokeRunStatus,
        String smokeRunDispatchStatus,
        Long smokeRunJobId
) {
}
