package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.TargetSiteEntity;

import java.time.Instant;

/**
 * Curated Gupy boards selected for implementation work.
 */
public final class GupyBoardOnboardingProfiles {

    private GupyBoardOnboardingProfiles() {
    }

    public static GupyBoardOnboardingProfile specialDogExtrema() {
        String apiUrl = "https://portal.api.gupy.io/api/v1/jobs?careerPageName=Special%20Dog%20Company&city=Extrema";

        TargetSiteEntity targetSite = TargetSiteEntity.builder()
                .siteCode("gupy_specialdog_extrema")
                .displayName("Special Dog Company Careers via Gupy - Extrema")
                .baseUrl(apiUrl)
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("n/a")
                .enabled(true)
                .createdAt(Instant.parse("2026-04-10T00:00:00Z"))
                .build();

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://specialdogcompany.gupy.io/robots.txt",
                true,
                true,
                "https://www.specialdog.com.br/privacidade",
                true,
                true,
                true,
                apiUrl,
                true,
                "Trilha privada de Extrema mapeada no board oficial da Special Dog Company com vagas reais na cidade.",
                "Gupy public jobs API: local board + city filtering on top of public payload facets",
                OnboardingLegalCategory.API_OFICIAL,
                "platform-team@local",
                "PUBLIC_ANONYMOUS",
                "Board Gupy da Special Dog revisado em 2026-04-10; robots do subdomínio permitem acesso público, o site corporativo expõe política de privacidade e a API pública foi validada com filtro local por careerPageName e city para isolar o board oficial e a localidade de Extrema."
        );

        return new GupyBoardOnboardingProfile(
                "Special Dog Company",
                apiUrl,
                targetSite,
                checklist
        );
    }
}
