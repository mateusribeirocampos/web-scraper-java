package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.domain.model.TargetSiteEntity;

import java.util.Objects;

/**
 * Selected Workday board plus the onboarding evidence required for activation.
 */
public record WorkdayBoardOnboardingProfile(
        String boardToken,
        String jobsApiUrl,
        TargetSiteEntity targetSite,
        SiteOnboardingChecklist checklist
) {

    public WorkdayBoardOnboardingProfile {
        Objects.requireNonNull(boardToken, "boardToken must not be null");
        Objects.requireNonNull(jobsApiUrl, "jobsApiUrl must not be null");
        Objects.requireNonNull(targetSite, "targetSite must not be null");
        Objects.requireNonNull(checklist, "checklist must not be null");
    }
}
