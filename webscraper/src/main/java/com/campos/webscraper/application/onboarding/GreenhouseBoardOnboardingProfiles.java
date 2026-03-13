package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.TargetSiteEntity;

import java.time.Instant;

/**
 * Curated Greenhouse boards approved for implementation work.
 */
public final class GreenhouseBoardOnboardingProfiles {

    private GreenhouseBoardOnboardingProfiles() {
    }

    public static GreenhouseBoardOnboardingProfile bitso() {
        String apiUrl = "https://boards-api.greenhouse.io/v1/boards/bitso/jobs";

        TargetSiteEntity targetSite = TargetSiteEntity.builder()
                .siteCode("greenhouse_bitso")
                .displayName("Bitso Careers via Greenhouse")
                .baseUrl("https://boards.greenhouse.io/bitso")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.PENDING_REVIEW)
                .selectorBundleVersion("n/a")
                .enabled(false)
                .createdAt(Instant.parse("2026-03-13T00:00:00Z"))
                .build();

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://boards.greenhouse.io/robots.txt",
                true,
                true,
                "",
                true,
                true,
                true,
                apiUrl,
                false,
                "Selecionado para expandir vagas PME com foco atual em Java/backend via ATS publico.",
                "Greenhouse public board API: 60 rpm conservative profile",
                OnboardingLegalCategory.API_OFICIAL,
                "platform-team@local",
                "PUBLIC_ANONYMOUS",
                "Greenhouse Job Board API publica revisada; board token bitso validado em 2026-03-13."
        );

        return new GreenhouseBoardOnboardingProfile(
                "bitso",
                apiUrl,
                targetSite,
                checklist
        );
    }
}
