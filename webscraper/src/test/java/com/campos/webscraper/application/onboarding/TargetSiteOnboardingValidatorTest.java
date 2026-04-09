package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("TargetSiteOnboardingValidator")
class TargetSiteOnboardingValidatorTest {

    private final TargetSiteOnboardingValidator validator = new TargetSiteOnboardingValidator();

    @Test
    @DisplayName("should keep PCI pending review and disabled when public terms are not fully documented")
    void shouldKeepPciPendingReviewAndDisabledWhenPublicTermsAreNotFullyDocumented() {
        TargetSiteEntity pci = buildPciTargetSite(true, LegalStatus.APPROVED);

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://www.pciconcursos.com.br/robots.txt",
                true,
                true,
                "",
                false,
                false,
                true,
                "",
                true,
                "Fonte relevante para concursos de tecnologia e Java.",
                "1 request every 5 seconds",
                OnboardingLegalCategory.SCRAPING_PERMITIDO,
                "platform-team@local",
                "PUBLIC_ANONYMOUS",
                "Homepage footer exposes only privacy/cancellation; no dedicated ToS found yet."
        );

        TargetSiteOnboardingDecision decision = validator.assess(pci, checklist);

        assertThat(decision.productionReady()).isFalse();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.PENDING_REVIEW);
        assertThat(decision.targetSite().isEnabled()).isFalse();
        assertThat(decision.blockingReasons()).contains("terms of service not reviewed");
    }

    @Test
    @DisplayName("should mark site as prohibited when onboarding evidence forbids scraping")
    void shouldMarkSiteAsProhibitedWhenOnboardingEvidenceForbidsScraping() {
        TargetSiteEntity pci = buildPciTargetSite(true, LegalStatus.PENDING_REVIEW);

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://www.pciconcursos.com.br/robots.txt",
                true,
                false,
                "https://example.org/terms",
                true,
                false,
                true,
                "",
                true,
                "Fonte relevante para concursos de tecnologia e Java.",
                "1 request every 5 seconds",
                OnboardingLegalCategory.SCRAPING_PROIBIDO,
                "platform-team@local",
                "PUBLIC_ANONYMOUS",
                "Robots or terms explicitly reject automated collection."
        );

        TargetSiteOnboardingDecision decision = validator.assess(pci, checklist);

        assertThat(decision.productionReady()).isFalse();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.SCRAPING_PROIBIDO);
        assertThat(decision.targetSite().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("should keep prohibited status even when unrelated checklist fields are incomplete")
    void shouldKeepProhibitedStatusEvenWhenUnrelatedChecklistFieldsAreIncomplete() {
        TargetSiteEntity pci = buildPciTargetSite(true, LegalStatus.PENDING_REVIEW);

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://www.pciconcursos.com.br/robots.txt",
                true,
                false,
                "",
                false,
                false,
                true,
                "",
                true,
                "",
                "",
                OnboardingLegalCategory.SCRAPING_PROIBIDO,
                "",
                "PUBLIC_ANONYMOUS",
                ""
        );

        TargetSiteOnboardingDecision decision = validator.assess(pci, checklist);

        assertThat(decision.productionReady()).isFalse();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.SCRAPING_PROIBIDO);
        assertThat(decision.targetSite().isEnabled()).isFalse();
        assertThat(decision.blockingReasons()).contains("scraping explicitly blocked by onboarding evidence");
    }

    @Test
    @DisplayName("should approve target site only when checklist is complete and permissive")
    void shouldApproveTargetSiteOnlyWhenChecklistIsCompleteAndPermissive() {
        TargetSiteEntity pci = buildPciTargetSite(false, LegalStatus.PENDING_REVIEW);

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://www.pciconcursos.com.br/robots.txt",
                true,
                true,
                "https://example.org/terms",
                true,
                true,
                true,
                "",
                true,
                "Fonte relevante para concursos de tecnologia e Java.",
                "1 request every 5 seconds",
                OnboardingLegalCategory.SCRAPING_PERMITIDO,
                "platform-team@local",
                "PUBLIC_ANONYMOUS",
                "Robots reviewed, no official API, public listing with static HTML."
        );

        TargetSiteOnboardingDecision decision = validator.assess(pci, checklist);

        assertThat(decision.productionReady()).isTrue();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.APPROVED);
        assertThat(decision.targetSite().isEnabled()).isTrue();
        assertThat(decision.blockingReasons()).isEmpty();
    }

    @Test
    @DisplayName("should keep api onboarding approvable when only robots deny scraping")
    void shouldKeepApiOnboardingApprovableWhenOnlyRobotsDenyScraping() {
        TargetSiteEntity apiSite = buildApiTargetSite(false, LegalStatus.PENDING_REVIEW);

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://api.example.gov/robots.txt",
                true,
                false,
                "https://api.example.gov/terms",
                true,
                true,
                true,
                "https://api.example.gov/jobs",
                true,
                "Consumir API oficial governamental.",
                "API quota 60 rpm",
                OnboardingLegalCategory.API_OFICIAL,
                "platform-team@local",
                "API_TOKEN",
                "OpenAPI and official docs reviewed; no HTML scraping path involved."
        );

        TargetSiteOnboardingDecision decision = validator.assess(apiSite, checklist);

        assertThat(decision.productionReady()).isTrue();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.APPROVED);
        assertThat(decision.targetSite().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("should keep api official onboarding pending when terms review was not completed")
    void shouldKeepApiOfficialOnboardingPendingWhenTermsReviewWasNotCompleted() {
        TargetSiteEntity apiSite = buildApiTargetSite(false, LegalStatus.PENDING_REVIEW);

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://api.example.gov/robots.txt",
                true,
                false,
                "",
                false,
                false,
                true,
                "",
                true,
                "Consumir API oficial governamental.",
                "API quota 60 rpm",
                OnboardingLegalCategory.API_OFICIAL,
                "platform-team@local",
                "API_TOKEN",
                "Base legal documented by official API docs."
        );

        TargetSiteOnboardingDecision decision = validator.assess(apiSite, checklist);

        assertThat(decision.productionReady()).isFalse();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.PENDING_REVIEW);
        assertThat(decision.targetSite().isEnabled()).isFalse();
        assertThat(decision.blockingReasons()).contains("terms of service not reviewed");
    }

    @Test
    @DisplayName("should approve api official onboarding without requiring a dedicated terms url once reviewed")
    void shouldApproveApiOfficialOnboardingWithoutRequiringADedicatedTermsUrlOnceReviewed() {
        TargetSiteEntity apiSite = buildApiTargetSite(false, LegalStatus.PENDING_REVIEW);

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://api.example.gov/robots.txt",
                true,
                false,
                "",
                true,
                true,
                true,
                "https://api.example.gov/jobs",
                true,
                "Consumir API oficial governamental.",
                "API quota 60 rpm",
                OnboardingLegalCategory.API_OFICIAL,
                "platform-team@local",
                "API_TOKEN",
                "Base legal documented by official API docs."
        );

        TargetSiteOnboardingDecision decision = validator.assess(apiSite, checklist);

        assertThat(decision.productionReady()).isTrue();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.APPROVED);
        assertThat(decision.targetSite().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("should approve Campinas public contests source when official public evidence is reviewed")
    void shouldApproveCampinasPublicContestsSourceWhenOfficialPublicEvidenceIsReviewed() {
        TargetSiteEntity campinas = TargetSiteEntity.builder()
                .siteCode("municipal_campinas")
                .displayName("Prefeitura de Campinas - Concursos")
                .baseUrl("https://portal-api.campinas.sp.gov.br/jsonapi/node/site?filter%5Bdrupal_internal__nid%5D=113658")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.PENDING_REVIEW)
                .selectorBundleVersion("campinas_jsonapi_v1")
                .enabled(false)
                .createdAt(Instant.parse("2026-03-31T00:00:00Z"))
                .build();

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://campinas.sp.gov.br/robots.txt",
                true,
                true,
                "https://portal-api.campinas.sp.gov.br/node/1599",
                true,
                true,
                true,
                "https://portal-api.campinas.sp.gov.br/jsonapi/node/site?filter%5Bdrupal_internal__nid%5D=113658",
                true,
                "Fonte oficial da Prefeitura de Campinas para concursos e processos seletivos.",
                "1 request every 10 seconds",
                OnboardingLegalCategory.API_OFICIAL,
                "platform-team@local",
                "PUBLIC_ANONYMOUS",
                "JSONAPI oficial do portal Campinas revisado com servico institucional publico e sem restricao explicita adicional encontrada."
        );

        TargetSiteOnboardingDecision decision = validator.assess(campinas, checklist);

        assertThat(decision.productionReady()).isTrue();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.APPROVED);
        assertThat(decision.targetSite().isEnabled()).isTrue();
        assertThat(decision.blockingReasons()).isEmpty();
    }

    @Test
    @DisplayName("should approve Itajuba public chamber source when official public evidence is reviewed")
    void shouldApproveItajubaPublicChamberSourceWhenOfficialPublicEvidenceIsReviewed() {
        TargetSiteEntity itajuba = TargetSiteEntity.builder()
                .siteCode("camara_itajuba")
                .displayName("Câmara Municipal de Itajubá - Concurso Público")
                .baseUrl("https://itajuba.cam.mg.gov.br/site/camara-municipal-de-itajuba-lanca-concurso-publico-para-preenchimento-de-cargos-efetivos/")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.PENDING_REVIEW)
                .selectorBundleVersion("camara_itajuba_html_v1")
                .enabled(false)
                .createdAt(Instant.parse("2026-04-06T00:00:00Z"))
                .build();

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://itajuba.cam.mg.gov.br/robots.txt",
                true,
                true,
                "https://itajuba.cam.mg.gov.br/site/lgpd-lei-geral-de-protecao-de-dados-2/",
                true,
                true,
                true,
                "",
                true,
                "Fonte oficial legislativa para concurso público municipal.",
                "1 request every 10 seconds",
                OnboardingLegalCategory.DADOS_PUBLICOS,
                "platform-team@local",
                "PUBLIC_ANONYMOUS",
                "Portal institucional da Câmara de Itajubá revisado com robots.txt público, política LGPD acessível e página oficial do concurso."
        );

        TargetSiteOnboardingDecision decision = validator.assess(itajuba, checklist);

        assertThat(decision.productionReady()).isTrue();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.APPROVED);
        assertThat(decision.targetSite().isEnabled()).isTrue();
        assertThat(decision.blockingReasons()).isEmpty();
    }

    @Test
    @DisplayName("should approve municipal PDF source when official public evidence is reviewed")
    void shouldApproveMunicipalPdfSourceWhenOfficialPublicEvidenceIsReviewed() {
        TargetSiteEntity municipalPdf = TargetSiteEntity.builder()
                .siteCode("municipal_pdf_source")
                .displayName("Prefeitura Municipal - Processo Seletivo Simplificado")
                .baseUrl("https://municipio.example.gov.br/edital-001-2026.pdf")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.PENDING_REVIEW)
                .selectorBundleVersion("municipal_pdf_v1")
                .enabled(false)
                .createdAt(Instant.parse("2026-04-09T00:00:00Z"))
                .build();

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://municipio.example.gov.br/robots.txt",
                true,
                true,
                "https://municipio.example.gov.br/lgpd",
                true,
                true,
                true,
                "",
                true,
                "Fonte oficial municipal com edital PDF público estável.",
                "1 request every 10 seconds",
                OnboardingLegalCategory.DADOS_PUBLICOS,
                "platform-team@local",
                "PUBLIC_ANONYMOUS",
                "Portal oficial revisado com robots permissivo, LGPD pública e edital PDF oficial público."
        );

        TargetSiteOnboardingDecision decision = validator.assess(municipalPdf, checklist);

        assertThat(decision.productionReady()).isTrue();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.APPROVED);
        assertThat(decision.targetSite().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("should approve Alcoa Workday source when official API and privacy evidence are reviewed")
    void shouldApproveAlcoaWorkdaySourceWhenOfficialApiAndPrivacyEvidenceAreReviewed() {
        TargetSiteEntity alcoa = TargetSiteEntity.builder()
                .siteCode("alcoa_pocos_caldas_workday")
                .displayName("Alcoa Careers via Workday - Poços de Caldas")
                .baseUrl("https://alcoa.wd5.myworkdayjobs.com/wday/cxs/alcoa/Careers/jobs")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.PENDING_REVIEW)
                .selectorBundleVersion("n/a")
                .enabled(false)
                .createdAt(Instant.parse("2026-04-09T00:00:00Z"))
                .build();

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://alcoa.wd5.myworkdayjobs.com/robots.txt",
                true,
                true,
                "https://www.alcoa.com/global/en/general/privacy",
                true,
                true,
                true,
                "https://alcoa.wd5.myworkdayjobs.com/wday/cxs/alcoa/Careers/jobs",
                true,
                "Board oficial da Alcoa com vagas reais em Poços de Caldas.",
                "Workday public jobs API: filtered POST with conservative pagination",
                OnboardingLegalCategory.API_OFICIAL,
                "platform-team@local",
                "PUBLIC_ANONYMOUS",
                "Workday Alcoa revisado com robots permitindo /Careers/ e política pública de privacidade."
        );

        TargetSiteOnboardingDecision decision = validator.assess(alcoa, checklist);

        assertThat(decision.productionReady()).isTrue();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.APPROVED);
        assertThat(decision.targetSite().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("should approve CI&T Lever source when official API and privacy evidence are reviewed")
    void shouldApproveCiandtLeverSourceWhenOfficialApiAndPrivacyEvidenceAreReviewed() {
        TargetSiteEntity ciandt = TargetSiteEntity.builder()
                .siteCode("lever_ciandt")
                .displayName("CI&T Careers via Lever")
                .baseUrl("https://api.lever.co/v0/postings/ciandt?mode=json")
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
                "https://ciandt.com/br/pt-br/politica-de-privacidade",
                true,
                true,
                true,
                "https://api.lever.co/v0/postings/ciandt?mode=json",
                true,
                "Primeira trilha privada de Campinas; board publico Lever da CI&T validado para expansao hybrid tech hubs.",
                "Lever public postings API: 60 rpm conservative profile",
                OnboardingLegalCategory.API_OFICIAL,
                "platform-team@local",
                "PUBLIC_ANONYMOUS",
                "Jobs.lever.co revisado com Content-Signal search=yes e politica de privacidade publica da CI&T revisada para o fluxo de candidatura."
        );

        TargetSiteOnboardingDecision decision = validator.assess(ciandt, checklist);

        assertThat(decision.productionReady()).isTrue();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.APPROVED);
        assertThat(decision.targetSite().isEnabled()).isTrue();
        assertThat(decision.blockingReasons()).isEmpty();
    }

    @Test
    @DisplayName("should prohibit WatchGuard Lever source when reviewed terms deny automated access")
    void shouldProhibitWatchGuardLeverSourceWhenReviewedTermsDenyAutomatedAccess() {
        TargetSiteEntity watchguard = TargetSiteEntity.builder()
                .siteCode("lever_watchguard")
                .displayName("WatchGuard Careers via Lever")
                .baseUrl("https://api.lever.co/v0/postings/watchguard?mode=json")
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
                "https://www.watchguard.com/wgrd-trust-center/terms-of-use",
                true,
                false,
                true,
                "https://api.lever.co/v0/postings/watchguard?mode=json",
                true,
                "Primeira trilha privada de Santa Rita do Sapucai; board publico Lever da WatchGuard com vagas ligadas ao polo local.",
                "Lever public postings API: 60 rpm conservative profile",
                OnboardingLegalCategory.SCRAPING_PROIBIDO,
                "platform-team@local",
                "PUBLIC_ANONYMOUS",
                "Jobs.lever.co revisado com Content-Signal search=yes, mas os Terms of Use da WatchGuard proíbem scraping e aplicações sem autorização prévia."
        );

        TargetSiteOnboardingDecision decision = validator.assess(watchguard, checklist);

        assertThat(decision.productionReady()).isFalse();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.SCRAPING_PROIBIDO);
        assertThat(decision.targetSite().isEnabled()).isFalse();
        assertThat(decision.blockingReasons()).contains("scraping explicitly blocked by onboarding evidence");
    }

    @Test
    @DisplayName("should approve Santa Rita Camara public contests source when official public evidence is reviewed")
    void shouldApproveSantaRitaCamaraPublicContestsSourceWhenOfficialPublicEvidenceIsReviewed() {
        TargetSiteEntity camaraSantaRita = TargetSiteEntity.builder()
                .siteCode("camara_santa_rita_sapucai")
                .displayName("Câmara Municipal de Santa Rita do Sapucaí - Processos Seletivos")
                .baseUrl("https://www.santaritadosapucai.mg.leg.br/transparencia/processos-seletivos-2025")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.PENDING_REVIEW)
                .selectorBundleVersion("camara_santa_rita_html_v1")
                .enabled(false)
                .createdAt(Instant.parse("2026-04-05T00:00:00Z"))
                .build();

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://www.santaritadosapucai.mg.leg.br/robots.txt",
                true,
                true,
                "https://www.santaritadosapucai.mg.leg.br/transparencia",
                true,
                true,
                true,
                "",
                true,
                "Fonte oficial da Câmara Municipal para processos seletivos e editais legislativos locais.",
                "1 request every 10 seconds",
                OnboardingLegalCategory.DADOS_PUBLICOS,
                "platform-team@local",
                "PUBLIC_ANONYMOUS",
                "Portal institucional/transparencia revisado com robots permissivo e sem restricao explicita adicional encontrada."
        );

        TargetSiteOnboardingDecision decision = validator.assess(camaraSantaRita, checklist);

        assertThat(decision.productionReady()).isTrue();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.APPROVED);
        assertThat(decision.targetSite().isEnabled()).isTrue();
        assertThat(decision.blockingReasons()).isEmpty();
    }

    @Test
    @DisplayName("should keep api official onboarding pending when official endpoint is not documented")
    void shouldKeepApiOfficialOnboardingPendingWhenOfficialEndpointIsNotDocumented() {
        TargetSiteEntity apiSite = buildApiTargetSite(false, LegalStatus.PENDING_REVIEW);

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://api.example.gov/robots.txt",
                true,
                false,
                "",
                true,
                true,
                true,
                "",
                true,
                "Consumir API oficial governamental.",
                "API quota 60 rpm",
                OnboardingLegalCategory.API_OFICIAL,
                "platform-team@local",
                "API_TOKEN",
                "Base legal documented by official API docs."
        );

        TargetSiteOnboardingDecision decision = validator.assess(apiSite, checklist);

        assertThat(decision.productionReady()).isFalse();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.PENDING_REVIEW);
        assertThat(decision.targetSite().isEnabled()).isFalse();
        assertThat(decision.blockingReasons()).contains("official API endpoint not documented");
    }

    @Test
    @DisplayName("should prohibit api onboarding when reviewed terms deny automated access")
    void shouldProhibitApiOnboardingWhenReviewedTermsDenyAutomatedAccess() {
        TargetSiteEntity apiSite = buildApiTargetSite(false, LegalStatus.PENDING_REVIEW);

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://api.example.gov/robots.txt",
                true,
                true,
                "https://api.example.gov/terms",
                true,
                false,
                true,
                "https://api.example.gov/jobs",
                true,
                "Consumir API oficial governamental.",
                "API quota 60 rpm",
                OnboardingLegalCategory.API_OFICIAL,
                "platform-team@local",
                "API_TOKEN",
                "Official terms reviewed and automation denied."
        );

        TargetSiteOnboardingDecision decision = validator.assess(apiSite, checklist);

        assertThat(decision.productionReady()).isFalse();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.SCRAPING_PROIBIDO);
        assertThat(decision.targetSite().isEnabled()).isFalse();
        assertThat(decision.blockingReasons()).contains("scraping explicitly blocked by onboarding evidence");
    }

    @Test
    @DisplayName("should reject api official onboarding when site metadata says static html scraping")
    void shouldRejectApiOfficialOnboardingWhenSiteMetadataSaysStaticHtmlScraping() {
        TargetSiteEntity pci = buildPciTargetSite(false, LegalStatus.PENDING_REVIEW);

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://www.pciconcursos.com.br/robots.txt",
                true,
                true,
                "https://api.example.gov/terms",
                true,
                true,
                true,
                "https://api.example.gov/jobs",
                true,
                "Consumir API oficial governamental.",
                "API quota 60 rpm",
                OnboardingLegalCategory.API_OFICIAL,
                "platform-team@local",
                "API_TOKEN",
                "Checklist misclassified as API."
        );

        TargetSiteOnboardingDecision decision = validator.assess(pci, checklist);

        assertThat(decision.productionReady()).isFalse();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.PENDING_REVIEW);
        assertThat(decision.targetSite().isEnabled()).isFalse();
        assertThat(decision.blockingReasons())
                .contains("onboarding legal category does not match site extraction metadata");
    }

    @Test
    @DisplayName("should keep api official mismatch pending even when html robots deny crawling")
    void shouldKeepApiOfficialMismatchPendingEvenWhenHtmlRobotsDenyCrawling() {
        TargetSiteEntity pci = buildPciTargetSite(false, LegalStatus.PENDING_REVIEW);

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://www.pciconcursos.com.br/robots.txt",
                true,
                false,
                "",
                true,
                true,
                true,
                "https://api.example.gov/jobs",
                true,
                "Consumir API oficial governamental.",
                "API quota 60 rpm",
                OnboardingLegalCategory.API_OFICIAL,
                "platform-team@local",
                "API_TOKEN",
                "Checklist misclassified as API while persisted metadata still points to HTML."
        );

        TargetSiteOnboardingDecision decision = validator.assess(pci, checklist);

        assertThat(decision.productionReady()).isFalse();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.PENDING_REVIEW);
        assertThat(decision.targetSite().isEnabled()).isFalse();
        assertThat(decision.blockingReasons())
                .contains("onboarding legal category does not match site extraction metadata");
    }

    @Test
    @DisplayName("should mark public data html onboarding as prohibited when reviewed evidence denies automated access")
    void shouldMarkPublicDataHtmlOnboardingAsProhibitedWhenReviewedEvidenceDeniesAutomatedAccess() {
        TargetSiteEntity publicHtmlSite = buildPciTargetSite(false, LegalStatus.PENDING_REVIEW);

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://dados.gov.example/robots.txt",
                true,
                false,
                "https://dados.gov.example/termos",
                true,
                false,
                true,
                "",
                true,
                "Coleta de pagina governamental com dados publicos.",
                "1 request every 10 seconds",
                OnboardingLegalCategory.DADOS_PUBLICOS,
                "platform-team@local",
                "PUBLIC_ANONYMOUS",
                "Pagina publica governamental sem autenticacao."
        );

        TargetSiteOnboardingDecision decision = validator.assess(publicHtmlSite, checklist);

        assertThat(decision.productionReady()).isFalse();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.SCRAPING_PROIBIDO);
        assertThat(decision.targetSite().isEnabled()).isFalse();
        assertThat(decision.blockingReasons()).contains("scraping explicitly blocked by onboarding evidence");
    }

    @Test
    @DisplayName("should reject scraping onboarding categories on api-backed sites")
    void shouldRejectScrapingOnboardingCategoriesOnApiBackedSites() {
        TargetSiteEntity apiSite = buildApiTargetSite(false, LegalStatus.PENDING_REVIEW);

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://api.example.gov/robots.txt",
                true,
                true,
                "https://api.example.gov/terms",
                true,
                true,
                true,
                "https://api.example.gov/jobs",
                true,
                "Consumir API oficial governamental.",
                "API quota 60 rpm",
                OnboardingLegalCategory.SCRAPING_PERMITIDO,
                "platform-team@local",
                "API_TOKEN",
                "Checklist wrongly classified as scraping."
        );

        TargetSiteOnboardingDecision decision = validator.assess(apiSite, checklist);

        assertThat(decision.productionReady()).isFalse();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.PENDING_REVIEW);
        assertThat(decision.targetSite().isEnabled()).isFalse();
        assertThat(decision.blockingReasons())
                .contains("onboarding legal category does not match site extraction metadata");
    }

    @Test
    @DisplayName("should reject api official onboarding when api metadata is only partially configured")
    void shouldRejectApiOfficialOnboardingWhenApiMetadataIsOnlyPartiallyConfigured() {
        TargetSiteEntity inconsistentApiSite = TargetSiteEntity.builder()
                .id(10L)
                .siteCode("broken_api_site")
                .displayName("Broken API Site")
                .baseUrl("https://api.example.gov")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.PENDING_REVIEW)
                .selectorBundleVersion("n/a")
                .enabled(false)
                .createdAt(Instant.parse("2026-03-13T00:00:00Z"))
                .build();

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://api.example.gov/robots.txt",
                true,
                true,
                "",
                true,
                true,
                true,
                "https://api.example.gov/jobs",
                true,
                "Consumir API oficial governamental.",
                "API quota 60 rpm",
                OnboardingLegalCategory.API_OFICIAL,
                "platform-team@local",
                "API_TOKEN",
                "API metadata drifted from persisted configuration."
        );

        TargetSiteOnboardingDecision decision = validator.assess(inconsistentApiSite, checklist);

        assertThat(decision.productionReady()).isFalse();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.PENDING_REVIEW);
        assertThat(decision.targetSite().isEnabled()).isFalse();
        assertThat(decision.blockingReasons())
                .contains("site extraction metadata is internally inconsistent")
                .contains("onboarding legal category does not match site extraction metadata");
    }

    @Test
    @DisplayName("should keep inconsistent api metadata pending even when robots deny html scraping")
    void shouldKeepInconsistentApiMetadataPendingEvenWhenRobotsDenyHtmlScraping() {
        TargetSiteEntity inconsistentApiSite = TargetSiteEntity.builder()
                .id(11L)
                .siteCode("broken_api_site_robots")
                .displayName("Broken API Site Robots")
                .baseUrl("https://api.example.gov")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.PENDING_REVIEW)
                .selectorBundleVersion("n/a")
                .enabled(false)
                .createdAt(Instant.parse("2026-03-13T00:00:00Z"))
                .build();

        SiteOnboardingChecklist checklist = new SiteOnboardingChecklist(
                "https://api.example.gov/robots.txt",
                true,
                false,
                "",
                true,
                true,
                true,
                "https://api.example.gov/jobs",
                true,
                "Consumir API oficial governamental.",
                "API quota 60 rpm",
                OnboardingLegalCategory.API_OFICIAL,
                "platform-team@local",
                "API_TOKEN",
                "API metadata drifted and robots disallow HTML."
        );

        TargetSiteOnboardingDecision decision = validator.assess(inconsistentApiSite, checklist);

        assertThat(decision.productionReady()).isFalse();
        assertThat(decision.targetSite().getLegalStatus()).isEqualTo(LegalStatus.PENDING_REVIEW);
        assertThat(decision.targetSite().isEnabled()).isFalse();
        assertThat(decision.blockingReasons())
                .contains("site extraction metadata is internally inconsistent")
                .contains("onboarding legal category does not match site extraction metadata");
    }

    private static TargetSiteEntity buildPciTargetSite(boolean enabled, LegalStatus legalStatus) {
        return TargetSiteEntity.builder()
                .id(8L)
                .siteCode("pci_concursos")
                .displayName("PCI Concursos")
                .baseUrl("https://www.pciconcursos.com.br")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(legalStatus)
                .selectorBundleVersion("pci_concursos_v1")
                .enabled(enabled)
                .createdAt(Instant.parse("2026-03-13T00:00:00Z"))
                .build();
    }

    private static TargetSiteEntity buildApiTargetSite(boolean enabled, LegalStatus legalStatus) {
        return TargetSiteEntity.builder()
                .id(9L)
                .siteCode("dou_api")
                .displayName("DOU API")
                .baseUrl("https://www.in.gov.br")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(legalStatus)
                .selectorBundleVersion("n/a")
                .enabled(enabled)
                .createdAt(Instant.parse("2026-03-13T00:00:00Z"))
                .build();
    }
}
