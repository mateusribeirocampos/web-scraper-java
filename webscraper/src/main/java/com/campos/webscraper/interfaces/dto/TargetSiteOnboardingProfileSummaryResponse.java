package com.campos.webscraper.interfaces.dto;

/**
 * Compact summary of a curated operational onboarding template.
 */
public record TargetSiteOnboardingProfileSummaryResponse(
        String profileKey,
        String sourceFamily,
        String siteCode,
        String displayName,
        String legalCategory
) {
}
