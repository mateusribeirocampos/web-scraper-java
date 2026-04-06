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
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("n/a")
                .enabled(true)
                .createdAt(Instant.parse("2026-03-31T00:00:00Z"))
                .build();

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://jobs.lever.co/robots.txt",
                true,
                true,
                "https://ciandt.com/br/pt-br/politica-de-privacidade",
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
                "Lever public postings endpoint da CI&T revisado em 2026-04-06; jobs.lever.co expõe Content-Signal search=yes e a politica de privacidade publica da CI&T cobre dados fornecidos quando a pessoa se candidata a uma vaga."
        );

        return new LeverBoardOnboardingProfile(
                "ciandt",
                apiUrl,
                targetSite,
                checklist
        );
    }

    public static LeverBoardOnboardingProfile watchguard() {
        String apiUrl = "https://api.lever.co/v0/postings/watchguard?mode=json";

        TargetSiteEntity targetSite = TargetSiteEntity.builder()
                .siteCode("lever_watchguard")
                .displayName("WatchGuard Careers via Lever")
                .baseUrl(apiUrl)
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.PENDING_REVIEW)
                .selectorBundleVersion("n/a")
                .enabled(false)
                .createdAt(Instant.parse("2026-04-04T00:00:00Z"))
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
                "Primeira trilha privada de Santa Rita do Sapucai; board publico Lever da WatchGuard com vagas ligadas ao polo local.",
                "Lever public postings API: 60 rpm conservative profile",
                OnboardingLegalCategory.API_OFICIAL,
                "platform-team@local",
                "PUBLIC_ANONYMOUS",
                "Lever public postings endpoint da WatchGuard revisado em 2026-04-04."
        );

        return new LeverBoardOnboardingProfile(
                "watchguard",
                apiUrl,
                targetSite,
                checklist
        );
    }
}
