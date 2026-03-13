package com.campos.webscraper.application.orchestrator;

import com.campos.webscraper.application.usecase.DouContestImportUseCase;
import com.campos.webscraper.application.usecase.GreenhouseJobImportUseCase;
import com.campos.webscraper.application.usecase.IndeedJobImportUseCase;
import com.campos.webscraper.application.usecase.PciConcursosImportUseCase;
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
    private DouContestImportUseCase douContestImportUseCase;

    @Mock
    private PciConcursosImportUseCase pciConcursosImportUseCase;

    @Test
    @DisplayName("should execute greenhouse import and return execution counters")
    void shouldExecuteGreenhouseImportAndReturnExecutionCounters() {
        CrawlJobEntity crawlJob = buildPrivateJob("greenhouse_bitso");
        CrawlExecutionEntity crawlExecution = buildExecution(crawlJob);
        when(greenhouseJobImportUseCase.execute(any(), any(), any()))
                .thenReturn(List.of(JobPostingEntity.builder().externalId("a").build(), JobPostingEntity.builder().externalId("b").build()));

        ImportingCrawlJobExecutionRunner runner = new ImportingCrawlJobExecutionRunner(
                indeedJobImportUseCase,
                greenhouseJobImportUseCase,
                douContestImportUseCase,
                pciConcursosImportUseCase
        );

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

        ImportingCrawlJobExecutionRunner runner = new ImportingCrawlJobExecutionRunner(
                indeedJobImportUseCase,
                greenhouseJobImportUseCase,
                douContestImportUseCase,
                pciConcursosImportUseCase
        );

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

        ImportingCrawlJobExecutionRunner runner = new ImportingCrawlJobExecutionRunner(
                indeedJobImportUseCase,
                greenhouseJobImportUseCase,
                douContestImportUseCase,
                pciConcursosImportUseCase
        );

        CrawlExecutionOutcome outcome = runner.run(crawlJob, crawlExecution);

        assertThat(outcome.pagesVisited()).isEqualTo(1);
        assertThat(outcome.itemsFound()).isEqualTo(1);
        verify(pciConcursosImportUseCase).execute(any(), any(), any());
    }

    @Test
    @DisplayName("should fail for unsupported sites")
    void shouldFailForUnsupportedSites() {
        CrawlJobEntity crawlJob = buildPrivateJob("lever_demo");
        CrawlExecutionEntity crawlExecution = buildExecution(crawlJob);

        ImportingCrawlJobExecutionRunner runner = new ImportingCrawlJobExecutionRunner(
                indeedJobImportUseCase,
                greenhouseJobImportUseCase,
                douContestImportUseCase,
                pciConcursosImportUseCase
        );

        assertThatThrownBy(() -> runner.run(crawlJob, crawlExecution))
                .isInstanceOf(UnsupportedSiteException.class)
                .hasMessageContaining("lever_demo");
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
