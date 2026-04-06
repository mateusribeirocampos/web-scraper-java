package com.campos.webscraper.application.orchestrator;

import com.campos.webscraper.application.usecase.DouContestImportUseCase;
import com.campos.webscraper.application.usecase.GreenhouseJobImportUseCase;
import com.campos.webscraper.application.usecase.GupyJobImportUseCase;
import com.campos.webscraper.application.usecase.InconfidentesContestImportUseCase;
import com.campos.webscraper.application.usecase.IndeedJobImportUseCase;
import com.campos.webscraper.application.usecase.LeverJobImportUseCase;
import com.campos.webscraper.application.usecase.PciConcursosImportUseCase;
import com.campos.webscraper.application.usecase.PousoAlegreContestImportUseCase;
import com.campos.webscraper.application.usecase.MunhozContestImportUseCase;
import com.campos.webscraper.application.usecase.CampinasContestImportUseCase;
import com.campos.webscraper.application.usecase.CamaraSantaRitaContestImportUseCase;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.shared.UnsupportedSiteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("ImportingCrawlJobExecutionRunner")
class ImportingCrawlJobExecutionRunnerTest {

    @Mock
    private IndeedJobImportUseCase indeedJobImportUseCase;

    @Mock
    private GreenhouseJobImportUseCase greenhouseJobImportUseCase;

    @Mock
    private LeverJobImportUseCase leverJobImportUseCase;

    @Mock
    private GupyJobImportUseCase gupyJobImportUseCase;

    @Mock
    private DouContestImportUseCase douContestImportUseCase;

    @Mock
    private PciConcursosImportUseCase pciConcursosImportUseCase;

    @Mock
    private InconfidentesContestImportUseCase inconfidentesContestImportUseCase;

    @Mock
    private PousoAlegreContestImportUseCase pousoAlegreContestImportUseCase;

    @Mock
    private MunhozContestImportUseCase munhozContestImportUseCase;

    @Mock
    private CampinasContestImportUseCase campinasContestImportUseCase;

    @Mock
    private CamaraSantaRitaContestImportUseCase camaraSantaRitaContestImportUseCase;

    @Test
    @DisplayName("should execute greenhouse import and return execution counters")
    void shouldExecuteGreenhouseImportAndReturnExecutionCounters() {
        CrawlJobEntity crawlJob = buildPrivateJob("greenhouse_bitso");
        CrawlExecutionEntity crawlExecution = buildExecution(crawlJob);
        when(greenhouseJobImportUseCase.execute(any(), any(), any()))
                .thenReturn(List.of(JobPostingEntity.builder().externalId("a").build(), JobPostingEntity.builder().externalId("b").build()));

        ImportingCrawlJobExecutionRunner runner = newRunner();

        CrawlExecutionOutcome outcome = runner.run(crawlJob, crawlExecution);

        assertThat(outcome.pagesVisited()).isEqualTo(1);
        assertThat(outcome.itemsFound()).isEqualTo(2);
        verify(greenhouseJobImportUseCase).execute(any(), any(), any());
    }

    @Test
    @DisplayName("should execute dou import for public contest jobs")
    void shouldExecuteDouImportForPublicContestJobs() {
        CrawlJobEntity crawlJob = buildPublicJob("dou-api");
        CrawlExecutionEntity crawlExecution = buildExecution(crawlJob);
        when(douContestImportUseCase.execute(any(), any(), any()))
                .thenReturn(List.of(PublicContestPostingEntity.builder().externalId("dou-1").build()));

        ImportingCrawlJobExecutionRunner runner = newRunner();

        CrawlExecutionOutcome outcome = runner.run(crawlJob, crawlExecution);

        assertThat(outcome.pagesVisited()).isEqualTo(1);
        assertThat(outcome.itemsFound()).isEqualTo(1);
        verify(douContestImportUseCase).execute(any(), any(), any());
    }

    @Test
    @DisplayName("should execute pci concursos import for static public contest jobs")
    void shouldExecutePciConcursosImportForStaticPublicContestJobs() {
        CrawlJobEntity crawlJob = buildStaticPublicJob("pci_concursos");
        CrawlExecutionEntity crawlExecution = buildExecution(crawlJob);
        when(pciConcursosImportUseCase.execute(any(), any(), any()))
                .thenReturn(List.of(PublicContestPostingEntity.builder().externalId("pci-1").build()));

        ImportingCrawlJobExecutionRunner runner = newRunner();

        CrawlExecutionOutcome outcome = runner.run(crawlJob, crawlExecution);

        assertThat(outcome.pagesVisited()).isEqualTo(1);
        assertThat(outcome.itemsFound()).isEqualTo(1);
        verify(pciConcursosImportUseCase).execute(any(), any(), any());
    }

    @Test
    @DisplayName("should execute lever import and return execution counters")
    void shouldExecuteLeverImportAndReturnExecutionCounters() {
        CrawlJobEntity crawlJob = buildPrivateJob("lever_watchguard");
        CrawlExecutionEntity crawlExecution = buildExecution(crawlJob);
        when(leverJobImportUseCase.execute(any(), any(), any()))
                .thenReturn(List.of(JobPostingEntity.builder().externalId("lever-1").build()));

        ImportingCrawlJobExecutionRunner runner = newRunner();

        CrawlExecutionOutcome outcome = runner.run(crawlJob, crawlExecution);

        assertThat(outcome.pagesVisited()).isEqualTo(1);
        assertThat(outcome.itemsFound()).isEqualTo(1);
        verify(leverJobImportUseCase).execute(any(), any(), any());
    }

    @Test
    @DisplayName("should fail for unsupported sites")
    void shouldFailForUnsupportedSites() {
        CrawlJobEntity crawlJob = buildPrivateJob("ats_demo");
        CrawlExecutionEntity crawlExecution = buildExecution(crawlJob);

        ImportingCrawlJobExecutionRunner runner = newRunner();

        assertThatThrownBy(() -> runner.run(crawlJob, crawlExecution))
                .isInstanceOf(UnsupportedSiteException.class)
                .hasMessageContaining("ats_demo");
    }

    @Test
    @DisplayName("should execute gupy import for gupy sites")
    void shouldExecuteGupyImportForGupySites() {
        CrawlJobEntity crawlJob = buildPrivateJob("gupy_java");
        CrawlExecutionEntity crawlExecution = buildExecution(crawlJob);
        when(gupyJobImportUseCase.execute(any(), any(), any()))
                .thenReturn(List.of(JobPostingEntity.builder().externalId("gupy-1").build()));

        ImportingCrawlJobExecutionRunner runner = newRunner();

        CrawlExecutionOutcome outcome = runner.run(crawlJob, crawlExecution);

        assertThat(outcome.pagesVisited()).isEqualTo(1);
        assertThat(outcome.itemsFound()).isEqualTo(1);
        verify(gupyJobImportUseCase).execute(any(), any(), any());
    }

    @Test
    @DisplayName("should execute Inconfidentes import for municipal static public contest jobs")
    void shouldExecuteInconfidentesImportForMunicipalStaticPublicContestJobs() {
        CrawlJobEntity crawlJob = buildStaticPublicJob("municipal_inconfidentes");
        CrawlExecutionEntity crawlExecution = buildExecution(crawlJob);
        when(inconfidentesContestImportUseCase.execute(any(), any(), any()))
                .thenReturn(List.of(PublicContestPostingEntity.builder().externalId("inconfidentes-1").build()));

        ImportingCrawlJobExecutionRunner runner = newRunner();

        CrawlExecutionOutcome outcome = runner.run(crawlJob, crawlExecution);

        assertThat(outcome.pagesVisited()).isEqualTo(1);
        assertThat(outcome.itemsFound()).isEqualTo(1);
        verify(inconfidentesContestImportUseCase).execute(any(), any(), any());
    }

    @Test
    @DisplayName("should execute Pouso Alegre import for municipal static public contest jobs")
    void shouldExecutePousoAlegreImportForMunicipalStaticPublicContestJobs() {
        CrawlJobEntity crawlJob = buildStaticPublicJob("municipal_pouso_alegre");
        CrawlExecutionEntity crawlExecution = buildExecution(crawlJob);
        when(pousoAlegreContestImportUseCase.execute(any(), any(), any()))
                .thenReturn(List.of(PublicContestPostingEntity.builder().externalId("pouso-alegre-1").build()));

        ImportingCrawlJobExecutionRunner runner = newRunner();

        CrawlExecutionOutcome outcome = runner.run(crawlJob, crawlExecution);

        assertThat(outcome.pagesVisited()).isEqualTo(1);
        assertThat(outcome.itemsFound()).isEqualTo(1);
        verify(pousoAlegreContestImportUseCase).execute(any(), any(), any());
    }

    @Test
    @DisplayName("should execute Munhoz import for municipal static public contest jobs")
    void shouldExecuteMunhozImportForMunicipalStaticPublicContestJobs() {
        CrawlJobEntity crawlJob = buildStaticPublicJob("municipal_munhoz");
        CrawlExecutionEntity crawlExecution = buildExecution(crawlJob);
        when(munhozContestImportUseCase.execute(any(), any(), any()))
                .thenReturn(List.of(PublicContestPostingEntity.builder().externalId("munhoz-1").build()));

        ImportingCrawlJobExecutionRunner runner = newRunner();

        CrawlExecutionOutcome outcome = runner.run(crawlJob, crawlExecution);

        assertThat(outcome.pagesVisited()).isEqualTo(1);
        assertThat(outcome.itemsFound()).isEqualTo(1);
        verify(munhozContestImportUseCase).execute(any(), any(), any());
    }

    @Test
    @DisplayName("should execute Campinas import for municipal official public contest api jobs")
    void shouldExecuteCampinasImportForMunicipalOfficialPublicContestApiJobs() {
        CrawlJobEntity crawlJob = buildPublicJob("municipal_campinas");
        CrawlExecutionEntity crawlExecution = buildExecution(crawlJob);
        when(campinasContestImportUseCase.execute(any(), any(), any()))
                .thenReturn(List.of(PublicContestPostingEntity.builder().externalId("campinas-1").build()));

        ImportingCrawlJobExecutionRunner runner = newRunner();

        CrawlExecutionOutcome outcome = runner.run(crawlJob, crawlExecution);

        assertThat(outcome.pagesVisited()).isEqualTo(1);
        assertThat(outcome.itemsFound()).isEqualTo(1);
        verify(campinasContestImportUseCase).execute(any(), any(), any());
    }

    @Test
    @DisplayName("should execute Santa Rita Câmara import for legislative static public contest jobs")
    void shouldExecuteSantaRitaCamaraImportForLegislativeStaticPublicContestJobs() {
        CrawlJobEntity crawlJob = buildStaticPublicJob("camara_santa_rita_sapucai");
        CrawlExecutionEntity crawlExecution = buildExecution(crawlJob);
        when(camaraSantaRitaContestImportUseCase.execute(any(), any(), any()))
                .thenReturn(List.of(PublicContestPostingEntity.builder().externalId("camara-1").build()));

        ImportingCrawlJobExecutionRunner runner = newRunner();

        CrawlExecutionOutcome outcome = runner.run(crawlJob, crawlExecution);

        assertThat(outcome.pagesVisited()).isEqualTo(1);
        assertThat(outcome.itemsFound()).isEqualTo(1);
        verify(camaraSantaRitaContestImportUseCase).execute(any(), any(), any());
    }

    private ImportingCrawlJobExecutionRunner newRunner() {
        return new ImportingCrawlJobExecutionRunner(
                indeedJobImportUseCase,
                greenhouseJobImportUseCase,
                leverJobImportUseCase,
                gupyJobImportUseCase,
                douContestImportUseCase,
                pciConcursosImportUseCase,
                inconfidentesContestImportUseCase,
                pousoAlegreContestImportUseCase,
                munhozContestImportUseCase,
                campinasContestImportUseCase,
                camaraSantaRitaContestImportUseCase
        );
    }

    private static CrawlJobEntity buildPrivateJob(String siteCode) {
        Instant now = Instant.parse("2026-03-13T18:00:00Z");
        return CrawlJobEntity.builder()
                .id(50L)
                .targetSite(TargetSiteEntity.builder()
                        .id(500L)
                        .siteCode(siteCode)
                        .displayName(siteCode)
                        .baseUrl("https://example.com/" + siteCode)
                        .siteType(SiteType.TYPE_E)
                        .extractionMode(ExtractionMode.API)
                        .jobCategory(JobCategory.PRIVATE_SECTOR)
                        .legalStatus(LegalStatus.APPROVED)
                        .selectorBundleVersion("n/a")
                        .enabled(true)
                        .createdAt(now.minusSeconds(60))
                        .build())
                .scheduledAt(now)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .createdAt(now.minusSeconds(30))
                .build();
    }

    private static CrawlJobEntity buildPublicJob(String siteCode) {
        Instant now = Instant.parse("2026-03-13T18:00:00Z");
        return CrawlJobEntity.builder()
                .id(51L)
                .targetSite(TargetSiteEntity.builder()
                        .id(501L)
                        .siteCode(siteCode)
                        .displayName(siteCode)
                        .baseUrl("https://example.com/" + siteCode)
                        .siteType(SiteType.TYPE_E)
                        .extractionMode(ExtractionMode.API)
                        .jobCategory(JobCategory.PUBLIC_CONTEST)
                        .legalStatus(LegalStatus.APPROVED)
                        .selectorBundleVersion("n/a")
                        .enabled(true)
                        .createdAt(now.minusSeconds(60))
                        .build())
                .scheduledAt(now)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .createdAt(now.minusSeconds(30))
                .build();
    }

    private static CrawlJobEntity buildStaticPublicJob(String siteCode) {
        Instant now = Instant.parse("2026-03-13T18:00:00Z");
        return CrawlJobEntity.builder()
                .id(52L)
                .targetSite(TargetSiteEntity.builder()
                        .id(502L)
                        .siteCode(siteCode)
                        .displayName(siteCode)
                        .baseUrl("https://example.com/" + siteCode)
                        .siteType(SiteType.TYPE_A)
                        .extractionMode(ExtractionMode.STATIC_HTML)
                        .jobCategory(JobCategory.PUBLIC_CONTEST)
                        .legalStatus(LegalStatus.APPROVED)
                        .selectorBundleVersion("pci_concursos_v1")
                        .enabled(true)
                        .createdAt(now.minusSeconds(60))
                        .build())
                .scheduledAt(now)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .createdAt(now.minusSeconds(30))
                .build();
    }

    private static CrawlExecutionEntity buildExecution(CrawlJobEntity crawlJob) {
        Instant now = Instant.parse("2026-03-13T18:05:00Z");
        return CrawlExecutionEntity.builder()
                .id(900L)
                .crawlJob(crawlJob)
                .status(CrawlExecutionStatus.RUNNING)
                .startedAt(now)
                .pagesVisited(0)
                .itemsFound(0)
                .retryCount(0)
                .createdAt(now)
                .build();
    }
}
