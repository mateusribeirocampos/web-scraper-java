package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.TargetSiteEntity;

import java.time.Instant;

/**
 * Curated Workday boards selected for implementation work.
 */
public final class WorkdayBoardOnboardingProfiles {

    private WorkdayBoardOnboardingProfiles() {
    }

    public static WorkdayBoardOnboardingProfile airbusHelibras() {
        String apiUrl = "https://ag.wd3.myworkdayjobs.com/wday/cxs/ag/Airbus/jobs";

        TargetSiteEntity targetSite = TargetSiteEntity.builder()
                .siteCode("airbus_helibras_workday")
                .displayName("Airbus / Helibras Careers via Workday")
                .baseUrl(apiUrl)
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("n/a")
                .enabled(true)
                .createdAt(Instant.parse("2026-04-08T00:00:00Z"))
                .build();

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://ag.wd3.myworkdayjobs.com/robots.txt",
                true,
                true,
                "",
                true,
                true,
                true,
                apiUrl,
                true,
                "Trilha privada de Itajubá mapeada no board oficial da Airbus com vagas da Helibras na cidade.",
                "Workday public jobs API: filtered POST with conservative pagination",
                OnboardingLegalCategory.API_OFICIAL,
                "platform-team@local",
                "PUBLIC_ANONYMOUS",
                "Board Workday Airbus revisado em 2026-04-08; robots do domínio wd3 permitem a trilha /Airbus/, a página oficial de careers da Airbus aponta para o board e a revisão legal foi fechada sem restrição explícita específica para a API pública de vagas."
        );

        return new WorkdayBoardOnboardingProfile(
                "Airbus",
                apiUrl,
                targetSite,
                checklist
        );
    }
}
