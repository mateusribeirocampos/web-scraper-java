package com.campos.webscraper.interfaces.dto;

/**
 * Detailed operational onboarding template exposed by the API.
 */
public record TargetSiteOnboardingProfileResponse(
        String profileKey,
        String sourceFamily,
        String boardToken,
        String jobsApiUrl,
        String siteCode,
        String displayName,
        String baseUrl,
        String siteType,
        String extractionMode,
        String jobCategory,
        String legalStatus,
        boolean enabled,
        String selectorBundleVersion,
        String robotsTxtUrl,
        boolean robotsTxtReviewed,
        boolean robotsTxtAllowsScraping,
        String termsOfServiceUrl,
        boolean termsReviewed,
        boolean termsAllowScraping,
        boolean officialApiChecked,
        String officialApiEndpointUrl,
        boolean strategySupportVerified,
        String businessJustification,
        String rateLimitProfile,
        String legalCategory,
        String owner,
        String authenticationStatus,
        String discoveryEvidence
) {
}
