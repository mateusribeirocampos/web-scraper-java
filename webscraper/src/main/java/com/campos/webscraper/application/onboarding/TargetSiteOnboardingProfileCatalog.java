package com.campos.webscraper.application.onboarding;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * In-memory catalog of curated onboarding templates that can be consumed operationally by the app.
 */
@Component
public class TargetSiteOnboardingProfileCatalog {

    private final Map<String, TargetSiteOnboardingProfileTemplate> templatesByKey;

    public TargetSiteOnboardingProfileCatalog() {
        TargetSiteOnboardingProfileTemplate greenhouseBitso = fromGreenhouseProfile(GreenhouseBoardOnboardingProfiles.bitso());
        TargetSiteOnboardingProfileTemplate indeedBr = CoreSourceOnboardingProfiles.indeedBr();
        TargetSiteOnboardingProfileTemplate douApi = CoreSourceOnboardingProfiles.douApi();
        TargetSiteOnboardingProfileTemplate pciConcursos = CoreSourceOnboardingProfiles.pciConcursos();
        this.templatesByKey = Map.of(
                greenhouseBitso.profileKey(), greenhouseBitso,
                indeedBr.profileKey(), indeedBr,
                douApi.profileKey(), douApi,
                pciConcursos.profileKey(), pciConcursos
        );
    }

    public List<TargetSiteOnboardingProfileTemplate> list() {
        return templatesByKey.values().stream()
                .sorted((left, right) -> left.profileKey().compareTo(right.profileKey()))
                .toList();
    }

    public TargetSiteOnboardingProfileTemplate get(String profileKey) {
        TargetSiteOnboardingProfileTemplate template = templatesByKey.get(profileKey);
        if (template == null) {
            throw new TargetSiteOnboardingProfileNotFoundException(profileKey);
        }
        return template;
    }

    private static TargetSiteOnboardingProfileTemplate fromGreenhouseProfile(GreenhouseBoardOnboardingProfile profile) {
        return new TargetSiteOnboardingProfileTemplate(
                "greenhouse_" + profile.boardToken(),
                "GREENHOUSE",
                profile.boardToken(),
                profile.jobsApiUrl(),
                profile.targetSite(),
                profile.checklist()
        );
    }
}
