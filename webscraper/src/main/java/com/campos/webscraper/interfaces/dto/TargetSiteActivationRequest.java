package com.campos.webscraper.interfaces.dto;

import com.campos.webscraper.application.onboarding.OnboardingLegalCategory;
import com.campos.webscraper.application.onboarding.SiteOnboardingChecklist;

/**
 * REST payload used to submit onboarding evidence for target-site activation.
 */
public record TargetSiteActivationRequest(
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

    public void validate() {
        if (legalCategory == null || legalCategory.isBlank()) {
            throw new IllegalArgumentException("legalCategory must not be null or blank");
        }
    }

    public SiteOnboardingChecklist toChecklist() {
        validate();

        return new SiteOnboardingChecklist(
                robotsTxtUrl,
                robotsTxtReviewed,
                robotsTxtAllowsScraping,
                termsOfServiceUrl,
                termsReviewed,
                termsAllowScraping,
                officialApiChecked,
                officialApiEndpointUrl,
                strategySupportVerified,
                businessJustification,
                rateLimitProfile,
                parseLegalCategory(),
                owner,
                authenticationStatus,
                discoveryEvidence
        );
    }

    private OnboardingLegalCategory parseLegalCategory() {
        try {
            return OnboardingLegalCategory.valueOf(legalCategory);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("legalCategory must be a valid onboarding category");
        }
    }
}
