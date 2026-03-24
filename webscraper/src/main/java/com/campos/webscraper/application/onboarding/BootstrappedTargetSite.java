package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.domain.model.TargetSiteEntity;

import java.util.Objects;

public record BootstrappedTargetSite(
        String profileKey,
        BootstrapStatus bootstrapStatus,
        TargetSiteEntity targetSite
) {

    public BootstrappedTargetSite {
        Objects.requireNonNull(profileKey, "profileKey must not be null");
        Objects.requireNonNull(bootstrapStatus, "bootstrapStatus must not be null");
        Objects.requireNonNull(targetSite, "targetSite must not be null");
    }
}
