package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.TargetSiteEntity;

import java.time.Instant;

/**
 * Curated onboarding templates for core source families already supported in production code.
 */
public final class CoreSourceOnboardingProfiles {

    private CoreSourceOnboardingProfiles() {
    }

    public static TargetSiteOnboardingProfileTemplate indeedBr() {
        String connectorBaseUrl = "https://to.indeed.com";
        return new TargetSiteOnboardingProfileTemplate(
                "indeed-br",
                "INDEED",
                "indeed-br",
                connectorBaseUrl,
                TargetSiteEntity.builder()
                        .siteCode("indeed-br")
                        .displayName("Indeed Brasil")
                        .baseUrl(connectorBaseUrl)
                        .siteType(SiteType.TYPE_E)
                        .extractionMode(ExtractionMode.API)
                        .jobCategory(JobCategory.PRIVATE_SECTOR)
                        .legalStatus(LegalStatus.PENDING_REVIEW)
                        .selectorBundleVersion("n/a")
                        .enabled(false)
                        .createdAt(Instant.parse("2026-03-24T00:00:00Z"))
                        .build(),
                new SiteOnboardingChecklist(
                        "https://br.indeed.com/robots.txt",
                        true,
                        true,
                        "https://www.indeed.com/legal",
                        true,
                        true,
                        true,
                        connectorBaseUrl,
                        true,
                        "Fonte API-first de vagas privadas com alto valor historico para a busca Java/backend.",
                        "Indeed API: conservative provider-managed quota",
                        OnboardingLegalCategory.API_OFICIAL,
                        "platform-team@local",
                        "API_KEY",
                        "Conector Indeed disponível em https://to.indeed.com e revisado como endpoint oficial base da integração."
                )
        );
    }

    public static TargetSiteOnboardingProfileTemplate douApi() {
        String runnableApiUrl = "https://www.in.gov.br/api/dou";
        return new TargetSiteOnboardingProfileTemplate(
                "dou-api",
                "DOU",
                "dou-api",
                runnableApiUrl,
                TargetSiteEntity.builder()
                        .siteCode("dou-api")
                        .displayName("Diário Oficial da União API")
                        .baseUrl(runnableApiUrl)
                        .siteType(SiteType.TYPE_E)
                        .extractionMode(ExtractionMode.API)
                        .jobCategory(JobCategory.PUBLIC_CONTEST)
                        .legalStatus(LegalStatus.PENDING_REVIEW)
                        .selectorBundleVersion("n/a")
                        .enabled(false)
                        .createdAt(Instant.parse("2026-03-24T00:00:00Z"))
                        .build(),
                new SiteOnboardingChecklist(
                        "https://www.in.gov.br/robots.txt",
                        true,
                        true,
                        "https://www.in.gov.br/termos-de-uso",
                        true,
                        true,
                        true,
                        runnableApiUrl,
                        true,
                        "Fonte oficial governamental para concursos e atos federais.",
                        "DOU API/public endpoint: 30 rpm conservative",
                        OnboardingLegalCategory.API_OFICIAL,
                        "platform-team@local",
                        "PUBLIC_ANONYMOUS",
                        "Endpoint público do DOU revisado como fonte oficial para concursos."
                )
        );
    }

    public static TargetSiteOnboardingProfileTemplate pciConcursos() {
        return new TargetSiteOnboardingProfileTemplate(
                "pci_concursos",
                "PCI_CONCURSOS",
                "pci_concursos",
                "https://www.pciconcursos.com.br",
                TargetSiteEntity.builder()
                        .siteCode("pci_concursos")
                        .displayName("PCI Concursos")
                        .baseUrl("https://www.pciconcursos.com.br")
                        .siteType(SiteType.TYPE_A)
                        .extractionMode(ExtractionMode.STATIC_HTML)
                        .jobCategory(JobCategory.PUBLIC_CONTEST)
                        .legalStatus(LegalStatus.PENDING_REVIEW)
                        .selectorBundleVersion("pci_concursos_v1")
                        .enabled(false)
                        .createdAt(Instant.parse("2026-03-24T00:00:00Z"))
                        .build(),
                new SiteOnboardingChecklist(
                        "https://www.pciconcursos.com.br/robots.txt",
                        true,
                        true,
                        "https://www.pciconcursos.com.br/termos-de-uso",
                        true,
                        true,
                        true,
                        "",
                        true,
                        "Fonte complementar para concursos publicos em tecnologia com HTML estático conhecido.",
                        "1 request every 5 seconds",
                        OnboardingLegalCategory.DADOS_PUBLICOS,
                        "platform-team@local",
                        "PUBLIC_ANONYMOUS",
                        "HTML público validado com selector bundle pci_concursos_v1."
                )
        );
    }
}
