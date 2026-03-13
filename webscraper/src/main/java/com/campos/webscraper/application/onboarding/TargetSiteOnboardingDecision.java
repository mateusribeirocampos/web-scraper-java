package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.domain.model.TargetSiteEntity;

import java.util.List;
import java.util.Objects;

/**
 * Result of reconciling onboarding evidence with the persisted target-site metadata.
 */
public record TargetSiteOnboardingDecision(
        TargetSiteEntity targetSite,
        boolean productionReady,
        List<String> blockingReasons
) {

    public TargetSiteOnboardingDecision {
        Objects.requireNonNull(targetSite, "targetSite must not be null");
        blockingReasons = List.copyOf(blockingReasons);
    }
}
