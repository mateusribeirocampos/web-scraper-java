package com.campos.webscraper.interfaces.dto;

public record TargetSiteBootstrapResponse(
        String profileKey,
        String bootstrapStatus,
        Long siteId,
        String siteCode,
        boolean enabled,
        String legalStatus
) {
}
