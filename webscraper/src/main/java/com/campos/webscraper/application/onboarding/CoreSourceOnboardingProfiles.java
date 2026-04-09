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
                        .legalStatus(LegalStatus.APPROVED)
                        .selectorBundleVersion("munhoz_html_v1")
                        .enabled(true)
                        .createdAt(Instant.parse("2026-03-30T00:00:00Z"))
                        .build(),
                new SiteOnboardingChecklist(
                        "https://www.munhoz.mg.gov.br/robots.txt",
                        true,
                        true,
                        "https://www.munhoz.mg.gov.br/termo_de_uso",
                        true,
                        true,
                        true,
                        "",
                        true,
                        "Fonte oficial municipal estruturada de concursos e processos seletivos, com pagina de detalhe e anexos do tipo edital.",
                        "1 request every 10 seconds",
                        OnboardingLegalCategory.DADOS_PUBLICOS,
                        "platform-team@local",
                        "PUBLIC_ANONYMOUS",
                        "Portal oficial de concursos validado em 2026-03-30; robots.txt publico respondeu 200 OK e a pagina LGPD/termo de uso publica foi revisada."
                )
        );
    }

    public static TargetSiteOnboardingProfileTemplate municipalCampinas() {
        String apiUrl = "https://portal-api.campinas.sp.gov.br/jsonapi/node/site?filter%5Bdrupal_internal__nid%5D=113658";
        return new TargetSiteOnboardingProfileTemplate(
                "municipal_campinas",
                "MUNICIPAL_API",
                "campinas",
                apiUrl,
                TargetSiteEntity.builder()
                        .siteCode("municipal_campinas")
                        .displayName("Prefeitura de Campinas - Concursos Públicos e Processos Seletivos")
                        .baseUrl(apiUrl)
                        .siteType(SiteType.TYPE_E)
                        .extractionMode(ExtractionMode.API)
                        .jobCategory(JobCategory.PUBLIC_CONTEST)
                        .legalStatus(LegalStatus.APPROVED)
                        .selectorBundleVersion("campinas_jsonapi_v1")
                        .enabled(true)
                        .createdAt(Instant.parse("2026-03-31T00:00:00Z"))
                        .build(),
                new SiteOnboardingChecklist(
                        "https://campinas.sp.gov.br/robots.txt",
                        true,
                        true,
                        "https://portal-api.campinas.sp.gov.br/node/1599",
                        true,
                        true,
                        true,
                        apiUrl,
                        true,
                        "Fonte oficial da Prefeitura de Campinas para concursos e processos seletivos, atualmente exposta por node JSONAPI com alerta oficial outbound.",
                        "1 request every 10 seconds",
                        OnboardingLegalCategory.API_OFICIAL,
                        "platform-team@local",
                        "PUBLIC_ANONYMOUS",
                        "JSONAPI oficial do portal Campinas revisado em 2026-04-06; robots.txt permite leitura publica, o servico institucional 'Concursos e Empregos' permanece acessivel sem autenticacao e nao foi encontrada restricao explicita adicional ao consumo da fonte oficial."
                )
        );
    }

    public static TargetSiteOnboardingProfileTemplate camaraSantaRitaSapucai() {
        String listingUrl = "https://www.santaritadosapucai.mg.leg.br/transparencia/processos-seletivos-2025";
        return new TargetSiteOnboardingProfileTemplate(
                "camara_santa_rita_sapucai",
                "LEGISLATIVE_HTML",
                "camara_santa_rita_sapucai",
                listingUrl,
                TargetSiteEntity.builder()
                        .siteCode("camara_santa_rita_sapucai")
                        .displayName("Câmara Municipal de Santa Rita do Sapucaí - Processos Seletivos")
                        .baseUrl(listingUrl)
                        .siteType(SiteType.TYPE_A)
                        .extractionMode(ExtractionMode.STATIC_HTML)
                        .jobCategory(JobCategory.PUBLIC_CONTEST)
                        .legalStatus(LegalStatus.APPROVED)
                        .selectorBundleVersion("camara_santa_rita_html_v1")
                        .enabled(true)
                        .createdAt(Instant.parse("2026-04-05T00:00:00Z"))
                        .build(),
                new SiteOnboardingChecklist(
                        "https://www.santaritadosapucai.mg.leg.br/robots.txt",
                        true,
                        true,
                        "https://www.santaritadosapucai.mg.leg.br/transparencia",
                        true,
                        true,
                        true,
                        "",
                        true,
                        "Fonte oficial da Câmara Municipal para processos seletivos e editais legislativos locais, com cronograma e anexos PDF no mesmo HTML.",
                        "1 request every 10 seconds",
                        OnboardingLegalCategory.DADOS_PUBLICOS,
                        "platform-team@local",
                        "PUBLIC_ANONYMOUS",
                        "Fonte oficial da Câmara revisada em 2026-04-06; robots.txt permite acesso anonimo e nao foi encontrada restricao explicita adicional no portal institucional/transparencia para esta coleta publica."
                )
        );
    }

    public static TargetSiteOnboardingProfileTemplate camaraItajuba() {
        String listingUrl = "https://itajuba.cam.mg.gov.br/site/camara-municipal-de-itajuba-lanca-concurso-publico-para-preenchimento-de-cargos-efetivos/";
        return new TargetSiteOnboardingProfileTemplate(
                "camara_itajuba",
                "LEGISLATIVE_HTML",
                "camara_itajuba",
                listingUrl,
                TargetSiteEntity.builder()
                        .siteCode("camara_itajuba")
                        .displayName("Câmara Municipal de Itajubá - Concurso Público")
                        .baseUrl(listingUrl)
                        .siteType(SiteType.TYPE_A)
                        .extractionMode(ExtractionMode.STATIC_HTML)
                        .jobCategory(JobCategory.PUBLIC_CONTEST)
                        .legalStatus(LegalStatus.APPROVED)
                        .selectorBundleVersion("camara_itajuba_html_v1")
                        .enabled(true)
                        .createdAt(Instant.parse("2026-04-06T00:00:00Z"))
                        .build(),
                new SiteOnboardingChecklist(
                        "https://itajuba.cam.mg.gov.br/robots.txt",
                        true,
                        true,
                        "https://itajuba.cam.mg.gov.br/site/lgpd-lei-geral-de-protecao-de-dados-2/",
                        true,
                        true,
                        true,
                        "",
                        true,
                        "Fonte oficial legislativa para o concurso público da Câmara Municipal de Itajubá, com página institucional e anexo PDF do edital.",
                        "1 request every 10 seconds",
                        OnboardingLegalCategory.DADOS_PUBLICOS,
                        "platform-team@local",
                        "PUBLIC_ANONYMOUS",
                        "Portal institucional da Câmara de Itajubá revisado em 2026-04-08 com robots.txt público, página oficial do concurso e política de privacidade/LGPD acessível no mesmo domínio."
                )
        );
    }

    public static TargetSiteOnboardingProfileTemplate municipalPocosCaldas() {
        String listingUrl = "https://descomplica.pocosdecaldas.mg.gov.br/info.php?c=609";
        return new TargetSiteOnboardingProfileTemplate(
                "municipal_pocos_caldas",
                "MUNICIPAL_PDF",
                "pocos_caldas",
                listingUrl,
                TargetSiteEntity.builder()
                        .siteCode("municipal_pocos_caldas")
                        .displayName("Prefeitura de Poços de Caldas - Processo Seletivo Simplificado")
                        .baseUrl(listingUrl)
                        .siteType(SiteType.TYPE_A)
                        .extractionMode(ExtractionMode.STATIC_HTML)
                        .jobCategory(JobCategory.PUBLIC_CONTEST)
                        .legalStatus(LegalStatus.PENDING_REVIEW)
                        .selectorBundleVersion("pocos_caldas_pdf_v1")
                        .enabled(false)
                        .createdAt(Instant.parse("2026-04-09T00:00:00Z"))
                        .build(),
                new SiteOnboardingChecklist(
                        "https://pocosdecaldas.mg.gov.br/robots.txt",
                        true,
                        true,
                        "https://pocosdecaldas.mg.gov.br/lgpd-lei-geral-de-protecao-de-dados/",
                        true,
                        true,
                        false,
                        listingUrl,
                        true,
                        "Fonte oficial municipal de Poços de Caldas ancorada na listagem institucional de concursos públicos, com descoberta automática do edital PDF canônico.",
                        "1 request every 10 seconds",
                        OnboardingLegalCategory.DADOS_PUBLICOS,
                        "platform-team@local",
                        "PUBLIC_ANONYMOUS",
                        "Portal oficial revisado em 2026-04-09; robots.txt permite acesso público, a página institucional LGPD está acessível e a listagem 'Concursos Públicos' do Descomplica Poços permanece como âncora oficial para descoberta de editais. O edital PDF 001/2025 segue como evidência pública auditada da rodada, mas a vigência operacional da fonte ainda não foi revalidada para um edital corrente."
                )
        );
    }

}
