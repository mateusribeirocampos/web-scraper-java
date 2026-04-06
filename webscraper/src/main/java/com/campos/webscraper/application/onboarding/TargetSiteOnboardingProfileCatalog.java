package com.campos.webscraper.application.onboarding;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory catalog of curated onboarding templates that can be consumed operationally by the app.
 */
@Component
public class TargetSiteOnboardingProfileCatalog {

    private final Map<String, TargetSiteOnboardingProfileTemplate> templatesByKey;

    public TargetSiteOnboardingProfileCatalog() {
        TargetSiteOnboardingProfileTemplate greenhouseBitso = fromGreenhouseProfile(GreenhouseBoardOnboardingProfiles.bitso());
        TargetSiteOnboardingProfileTemplate leverCiandt = fromLeverProfile(LeverBoardOnboardingProfiles.ciandt());
        TargetSiteOnboardingProfileTemplate leverWatchguard = fromLeverProfile(LeverBoardOnboardingProfiles.watchguard());
        TargetSiteOnboardingProfileTemplate indeedBr = CoreSourceOnboardingProfiles.indeedBr();
        TargetSiteOnboardingProfileTemplate douApi = CoreSourceOnboardingProfiles.douApi();
        TargetSiteOnboardingProfileTemplate pciConcursos = CoreSourceOnboardingProfiles.pciConcursos();
        TargetSiteOnboardingProfileTemplate municipalInconfidentes = CoreSourceOnboardingProfiles.municipalInconfidentes();
        TargetSiteOnboardingProfileTemplate municipalPousoAlegre = CoreSourceOnboardingProfiles.municipalPousoAlegre();
        TargetSiteOnboardingProfileTemplate municipalMunhoz = CoreSourceOnboardingProfiles.municipalMunhoz();
        TargetSiteOnboardingProfileTemplate municipalCampinas = CoreSourceOnboardingProfiles.municipalCampinas();
        TargetSiteOnboardingProfileTemplate camaraSantaRitaSapucai = CoreSourceOnboardingProfiles.camaraSantaRitaSapucai();
        this.templatesByKey = Map.ofEntries(
                Map.entry(greenhouseBitso.profileKey(), greenhouseBitso),
                Map.entry(leverCiandt.profileKey(), leverCiandt),
                Map.entry(leverWatchguard.profileKey(), leverWatchguard),
                Map.entry(indeedBr.profileKey(), indeedBr),
                Map.entry(douApi.profileKey(), douApi),
                Map.entry(pciConcursos.profileKey(), pciConcursos),
                Map.entry(municipalInconfidentes.profileKey(), municipalInconfidentes),
                Map.entry(municipalPousoAlegre.profileKey(), municipalPousoAlegre),
                Map.entry(municipalMunhoz.profileKey(), municipalMunhoz),
                Map.entry(municipalCampinas.profileKey(), municipalCampinas),
                Map.entry(camaraSantaRitaSapucai.profileKey(), camaraSantaRitaSapucai)
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

    public Optional<TargetSiteOnboardingProfileTemplate> findBySiteCode(String siteCode) {
        return templatesByKey.values().stream()
                .filter(template -> template.targetSite().getSiteCode().equals(siteCode))
                .findFirst();
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

    private static TargetSiteOnboardingProfileTemplate fromLeverProfile(LeverBoardOnboardingProfile profile) {
        return new TargetSiteOnboardingProfileTemplate(
                "lever_" + profile.boardToken(),
                "LEVER",
                profile.boardToken(),
                profile.jobsApiUrl(),
                profile.targetSite(),
                profile.checklist()
        );
    }
}
