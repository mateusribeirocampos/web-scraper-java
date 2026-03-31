package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.TargetSiteEntity;

import java.time.Instant;

/**
 * Curated Lever boards approved for implementation work.
 */
public final class LeverBoardOnboardingProfiles {

    private LeverBoardOnboardingProfiles() {
    }

    public static LeverBoardOnboardingProfile ciandt() {
        String apiUrl = "https://api.lever.co/v0/postings/ciandt?mode=json";

        TargetSiteEntity targetSite = TargetSiteEntity.builder()
                .siteCode("lever_ciandt")
                .displayName("CI&T Careers via Lever")
                .baseUrl(apiUrl)
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.PENDING_REVIEW)
                .selectorBundleVersion("n/a")
                .enabled(false)
                .createdAt(Instant.parse("2026-03-31T00:00:00Z"))
                .build();

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://jobs.lever.co/robots.txt",
                true,
                true,
                "",
                true,
                true,
                true,
                apiUrl,
                true,
                "Primeira trilha privada de Campinas; board publico Lever da CI&T validado para expansao hybrid tech hubs.",
                "Lever public postings API: 60 rpm conservative profile",
                OnboardingLegalCategory.API_OFICIAL,
                "platform-team@local",
                "PUBLIC_ANONYMOUS",
                "Lever public postings endpoint da CI&T revisado em 2026-03-31."
        );

        return new LeverBoardOnboardingProfile(
                "ciandt",
                apiUrl,
                targetSite,
                checklist
        );
    }
}
