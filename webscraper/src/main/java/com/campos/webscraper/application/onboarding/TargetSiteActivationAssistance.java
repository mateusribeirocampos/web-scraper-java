package com.campos.webscraper.application.onboarding;

import java.util.List;
import java.util.Objects;

public record TargetSiteActivationAssistance(
        Long siteId,
        String siteCode,
        String profileKey,
        ActivationAssistanceSource assistanceSource,
        SiteOnboardingChecklist suggestedChecklist,
        boolean productionReadyIfActivatedNow,
        List<String> blockingReasonsIfActivatedNow,
        List<String> notes
) {

    public TargetSiteActivationAssistance {
        Objects.requireNonNull(siteId, "siteId must not be null");
        Objects.requireNonNull(siteCode, "siteCode must not be null");
        Objects.requireNonNull(assistanceSource, "assistanceSource must not be null");
        Objects.requireNonNull(suggestedChecklist, "suggestedChecklist must not be null");
        blockingReasonsIfActivatedNow = List.copyOf(blockingReasonsIfActivatedNow == null ? List.of() : blockingReasonsIfActivatedNow);
        notes = List.copyOf(notes == null ? List.of() : notes);
    }
}
