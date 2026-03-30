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

    public static TargetSiteOnboardingProfileTemplate municipalInconfidentes() {
        String listingUrl = "https://inconfidentes.mg.gov.br/editais-concursos-e-processos-seletivos";
        return new TargetSiteOnboardingProfileTemplate(
                "municipal_inconfidentes",
                "MUNICIPAL_HTML",
                "inconfidentes",
                listingUrl,
                TargetSiteEntity.builder()
                        .siteCode("municipal_inconfidentes")
                        .displayName("Prefeitura de Inconfidentes - Editais")
                        .baseUrl(listingUrl)
                        .siteType(SiteType.TYPE_A)
                        .extractionMode(ExtractionMode.STATIC_HTML)
                        .jobCategory(JobCategory.PUBLIC_CONTEST)
                        .legalStatus(LegalStatus.APPROVED)
                        .selectorBundleVersion("inconfidentes_html_v1")
                        .enabled(true)
                        .createdAt(Instant.parse("2026-03-25T00:00:00Z"))
                        .build(),
                new SiteOnboardingChecklist(
                        "https://inconfidentes.mg.gov.br/robots.txt",
                        true,
                        true,
                        "https://inconfidentes.mg.gov.br/politica-de-privacidade",
                        true,
                        true,
                        true,
                        "",
                        true,
                        "Fonte oficial municipal para processos seletivos e editais locais, priorizando cargos e evidencias de escolaridade do proprio edital.",
                        "1 request every 10 seconds",
                        OnboardingLegalCategory.DADOS_PUBLICOS,
                        "platform-team@local",
                        "PUBLIC_ANONYMOUS",
                        "Pagina oficial de editais validada em 2026-03-26; robots.txt permite acesso publico e politica de privacidade publica revisada."
                )
        );
    }

    public static TargetSiteOnboardingProfileTemplate municipalPousoAlegre() {
        String listingUrl = "https://www.pousoalegre.mg.gov.br/concursos-publicos";
        return new TargetSiteOnboardingProfileTemplate(
                "municipal_pouso_alegre",
                "MUNICIPAL_HTML",
                "pouso_alegre",
                listingUrl,
                TargetSiteEntity.builder()
                        .siteCode("municipal_pouso_alegre")
                        .displayName("Prefeitura de Pouso Alegre - Concursos")
                        .baseUrl(listingUrl)
                        .siteType(SiteType.TYPE_A)
                        .extractionMode(ExtractionMode.STATIC_HTML)
                        .jobCategory(JobCategory.PUBLIC_CONTEST)
                        .legalStatus(LegalStatus.APPROVED)
                        .selectorBundleVersion("pouso_alegre_html_v1")
                        .enabled(true)
                        .createdAt(Instant.parse("2026-03-30T00:00:00Z"))
                        .build(),
                new SiteOnboardingChecklist(
                        "https://www.pousoalegre.mg.gov.br/robots.txt",
                        true,
                        true,
                        "https://pousoalegre.mg.gov.br/politica_privacidade",
                        true,
                        true,
                        true,
                        "",
                        true,
                        "Fonte oficial municipal estruturada de concursos e processos seletivos, com detalhe por pagina e anexos do tipo edital.",
                        "1 request every 10 seconds",
                        OnboardingLegalCategory.DADOS_PUBLICOS,
                        "platform-team@local",
                        "PUBLIC_ANONYMOUS",
                        "Portal oficial de concursos validado em 2026-03-30; robots.txt sem disallow e politica de privacidade publica revisada."
                )
        );
    }

    public static TargetSiteOnboardingProfileTemplate municipalMunhoz() {
        String listingUrl = "https://www.munhoz.mg.gov.br/concursos-publicos";
        return new TargetSiteOnboardingProfileTemplate(
                "municipal_munhoz",
                "MUNICIPAL_HTML",
                "munhoz",
                listingUrl,
                TargetSiteEntity.builder()
                        .siteCode("municipal_munhoz")
                        .displayName("Prefeitura de Munhoz - Concursos")
                        .baseUrl(listingUrl)
                        .siteType(SiteType.TYPE_A)
                        .extractionMode(ExtractionMode.STATIC_HTML)
                        .jobCategory(JobCategory.PUBLIC_CONTEST)
                        .legalStatus(LegalStatus.PENDING_REVIEW)
                        .selectorBundleVersion("munhoz_html_v1")
                        .enabled(false)
                        .createdAt(Instant.parse("2026-03-30T00:00:00Z"))
                        .build(),
                new SiteOnboardingChecklist(
                        "https://www.munhoz.mg.gov.br/robots.txt",
                        false,
                        false,
                        "https://www.munhoz.mg.gov.br/politica-de-privacidade",
                        false,
                        false,
                        false,
                        "",
                        true,
                        "Fonte oficial municipal estruturada de concursos e processos seletivos, com pagina de detalhe e anexos do tipo edital.",
                        "1 request every 10 seconds",
                        OnboardingLegalCategory.DADOS_PUBLICOS,
                        "platform-team@local",
                        "PUBLIC_ANONYMOUS",
                        "Portal de concursos de Munhoz identificado em 2026-03-30; implementação técnica iniciada e revisão legal/operacional ainda pendente."
                )
        );
    }

}
