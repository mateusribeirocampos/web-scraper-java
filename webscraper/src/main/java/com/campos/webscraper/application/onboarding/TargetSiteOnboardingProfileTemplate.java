package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.domain.model.TargetSiteEntity;

import java.util.Objects;

/**
 * Reusable operational onboarding template curated for a concrete source/profile key.
 */
public record TargetSiteOnboardingProfileTemplate(
        String profileKey,
        String sourceFamily,
        String sourceIdentifier,
        String jobsApiUrl,
        TargetSiteEntity targetSite,
        SiteOnboardingChecklist checklist
) {

    public TargetSiteOnboardingProfileTemplate {
        Objects.requireNonNull(profileKey, "profileKey must not be null");
        Objects.requireNonNull(sourceFamily, "sourceFamily must not be null");
        Objects.requireNonNull(sourceIdentifier, "sourceIdentifier must not be null");
        Objects.requireNonNull(targetSite, "targetSite must not be null");
        Objects.requireNonNull(checklist, "checklist must not be null");
    }
}
