package com.campos.webscraper.interfaces.dto;

import java.time.Instant;

public record TargetSiteCrawlJobBootstrapResponse(
        String bootstrapStatus,
        Long jobId,
        Long siteId,
        String siteCode,
        boolean schedulerManaged,
        Instant scheduledAt
) {
}
