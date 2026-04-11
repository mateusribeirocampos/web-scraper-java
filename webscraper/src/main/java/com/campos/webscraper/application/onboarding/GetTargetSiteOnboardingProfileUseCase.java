package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.interfaces.dto.TargetSiteOnboardingProfileResponse;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Returns the full operational onboarding template for a curated source profile.
 */
@Component
public class GetTargetSiteOnboardingProfileUseCase {

    private final TargetSiteOnboardingProfileCatalog catalog;

    public GetTargetSiteOnboardingProfileUseCase(TargetSiteOnboardingProfileCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
    }

    public TargetSiteOnboardingProfileResponse execute(String profileKey) {
        return toResponse(catalog.get(profileKey));
    }

    private static TargetSiteOnboardingProfileResponse toResponse(TargetSiteOnboardingProfileTemplate template) {
        return new TargetSiteOnboardingProfileResponse(
                template.profileKey(),
                template.sourceFamily(),
                supportsBoardToken(template.sourceFamily()) ? template.sourceIdentifier() : null,
                template.sourceIdentifier(),
                template.jobsApiUrl(),
                template.targetSite().getSiteCode(),
                template.targetSite().getDisplayName(),
                template.targetSite().getBaseUrl(),
                template.targetSite().getSiteType().name(),
                template.targetSite().getExtractionMode().name(),
                template.targetSite().getJobCategory().name(),
                template.targetSite().getLegalStatus().name(),
                template.targetSite().isEnabled(),
                template.targetSite().getSelectorBundleVersion(),
                template.checklist().robotsTxtUrl(),
                template.checklist().robotsTxtReviewed(),
                template.checklist().robotsTxtAllowsScraping(),
                template.checklist().termsOfServiceUrl(),
                template.checklist().termsReviewed(),
                template.checklist().termsAllowScraping(),
                template.checklist().officialApiChecked(),
                template.checklist().officialApiEndpointUrl(),
                template.checklist().strategySupportVerified(),
                template.checklist().businessJustification(),
                template.checklist().rateLimitProfile(),
                template.checklist().legalCategory().name(),
                template.checklist().owner(),
                template.checklist().authenticationStatus(),
                template.checklist().discoveryEvidence()
        );
    }

    private static boolean supportsBoardToken(String sourceFamily) {
        return "GREENHOUSE".equals(sourceFamily)
                || "LEVER".equals(sourceFamily)
                || "WORKDAY".equals(sourceFamily)
                || "GUPY".equals(sourceFamily);
    }
}
