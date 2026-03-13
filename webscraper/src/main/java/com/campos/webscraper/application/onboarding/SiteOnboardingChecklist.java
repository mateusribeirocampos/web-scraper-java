package com.campos.webscraper.application.onboarding;

import java.util.Objects;

/**
 * Mandatory onboarding evidence required before a site can be activated in production.
 */
public record SiteOnboardingChecklist(
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
        OnboardingLegalCategory legalCategory,
        String owner,
        String authenticationStatus,
        String discoveryEvidence
) {

    public SiteOnboardingChecklist {
        Objects.requireNonNull(legalCategory, "legalCategory must not be null");
    }
}
