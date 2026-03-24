package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.interfaces.dto.TargetSiteOnboardingProfileSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Lists the curated onboarding templates available for operational use.
 */
@Component
public class ListTargetSiteOnboardingProfilesUseCase {

    private final TargetSiteOnboardingProfileCatalog catalog;

    public ListTargetSiteOnboardingProfilesUseCase(TargetSiteOnboardingProfileCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
    }

    public List<TargetSiteOnboardingProfileSummaryResponse> execute() {
        return catalog.list().stream()
                .map(template -> new TargetSiteOnboardingProfileSummaryResponse(
                        template.profileKey(),
                        template.sourceFamily(),
                        template.targetSite().getSiteCode(),
                        template.targetSite().getDisplayName(),
                        template.checklist().legalCategory().name()
                ))
                .toList();
    }
}
