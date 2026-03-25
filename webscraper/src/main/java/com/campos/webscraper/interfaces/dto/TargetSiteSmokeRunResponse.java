package com.campos.webscraper.interfaces.dto;

public record TargetSiteSmokeRunResponse(
        Long siteId,
        String siteCode,
        Long jobId,
        String bootstrapStatus,
        String smokeRunStatus,
        String dispatchStatus
) {
}
