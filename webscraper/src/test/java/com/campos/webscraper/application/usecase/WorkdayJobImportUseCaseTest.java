package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.strategy.WorkdayJobScraperStrategy;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.JobPostingRepository;
import com.campos.webscraper.shared.JobPostingFingerprintCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkdayJobImportUseCase")
class WorkdayJobImportUseCaseTest {

    @Mock
    private JobPostingRepository jobPostingRepository;

    @Mock
    private WorkdayJobScraperStrategy strategy;

    @Mock
    private IdempotentJobPostingPersistenceService idempotentPersistenceService;

    @Test
    @DisplayName("should propagate scrape failures instead of persisting empty successful imports")
    void shouldPropagateScrapeFailuresInsteadOfPersistingEmptySuccessfulImports() {
        WorkdayJobImportUseCase useCase = new WorkdayJobImportUseCase(
                jobPostingRepository,
                strategy,
                new JobPostingFingerprintCalculator(),
                idempotentPersistenceService
        );

        when(strategy.scrape(any())).thenReturn(ScrapeResult.failure(
                "airbus_helibras_workday",
                "Workday jobs request failed with status 503"
        ));

        assertThatThrownBy(() -> useCase.execute(buildSite(), buildExecution(), buildCommand()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Workday scrape failed")
                .hasMessageContaining("503");

        verify(idempotentPersistenceService, never()).persist(any());
    }

    @Test
    @DisplayName("should persist normalized items when scrape succeeds")
    void shouldPersistNormalizedItemsWhenScrapeSucceeds() {
        WorkdayJobImportUseCase useCase = new WorkdayJobImportUseCase(
                jobPostingRepository,
                strategy,
                new JobPostingFingerprintCalculator(),
                idempotentPersistenceService
        );

        when(strategy.scrape(any())).thenReturn(ScrapeResult.success(List.of(
                JobPostingEntity.builder()
                        .externalId("JR10397592")
                        .canonicalUrl("https://ag.wd3.myworkdayjobs.com/en-US/Airbus/job/Itajub/Estgio-Tcnico-em-Produo_JR10397592")
                        .title("Estágio Técnico em Produção")
                        .company("Helibras / Airbus")
                        .location("Itajubá")
                        .remote(false)
                        .publishedAt(LocalDate.parse("2026-04-08"))
                        .payloadJson("{}")
                        .createdAt(Instant.parse("2026-04-08T12:00:00Z"))
                        .build()
        ), "airbus_helibras_workday"));

        useCase.execute(buildSite(), buildExecution(), buildCommand());

        verify(idempotentPersistenceService).persist(any());
    }

    @Test
    @DisplayName("should persist Alcoa Workday items when scrape succeeds")
    void shouldPersistAlcoaWorkdayItemsWhenScrapeSucceeds() {
        WorkdayJobImportUseCase useCase = new WorkdayJobImportUseCase(
                jobPostingRepository,
                strategy,
                new JobPostingFingerprintCalculator(),
                idempotentPersistenceService
        );

        when(strategy.scrape(any())).thenReturn(ScrapeResult.success(List.of(
                JobPostingEntity.builder()
                        .externalId("Req-36617")
                        .canonicalUrl("https://alcoa.wd5.myworkdayjobs.com/en-US/Careers/job/Brazil-MG-Poos-de-Caldas/Operadora-or--de-Refinaria-A_Req-36617")
                        .title("Operadora(or) de Refinaria A")
                        .company("Alcoa")
                        .location("Brazil, MG, Poços de Caldas")
                        .remote(false)
                        .publishedAt(LocalDate.parse("2026-04-03"))
                        .payloadJson("{}")
                        .createdAt(Instant.parse("2026-04-09T12:00:00Z"))
                        .build()
        ), "alcoa_pocos_caldas_workday"));

        useCase.execute(buildAlcoaSite(), buildExecution(), buildAlcoaCommand());

        verify(idempotentPersistenceService).persist(any());
    }

    private static TargetSiteEntity buildSite() {
        return TargetSiteEntity.builder()
                .id(51L)
                .siteCode("airbus_helibras_workday")
                .displayName("Airbus / Helibras Careers via Workday")
                .baseUrl("https://ag.wd3.myworkdayjobs.com/wday/cxs/ag/Airbus/jobs")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.PENDING_REVIEW)
                .selectorBundleVersion("n/a")
                .enabled(false)
                .createdAt(Instant.parse("2026-04-08T00:00:00Z"))
                .build();
    }

    private static CrawlExecutionEntity buildExecution() {
        return CrawlExecutionEntity.builder()
                .id(901L)
                .status(CrawlExecutionStatus.RUNNING)
                .startedAt(Instant.parse("2026-04-08T12:00:00Z"))
                .pagesVisited(0)
                .itemsFound(0)
                .retryCount(0)
                .createdAt(Instant.parse("2026-04-08T12:00:00Z"))
                .build();
    }

    private static ScrapeCommand buildCommand() {
        return new ScrapeCommand(
                "airbus_helibras_workday",
                "https://ag.wd3.myworkdayjobs.com/wday/cxs/ag/Airbus/jobs",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR
        );
    }

    private static TargetSiteEntity buildAlcoaSite() {
        return TargetSiteEntity.builder()
                .id(52L)
                .siteCode("alcoa_pocos_caldas_workday")
                .displayName("Alcoa Careers via Workday - Poços de Caldas")
                .baseUrl("https://alcoa.wd5.myworkdayjobs.com/wday/cxs/alcoa/Careers/jobs")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("n/a")
                .enabled(true)
                .createdAt(Instant.parse("2026-04-09T00:00:00Z"))
                .build();
    }

    private static ScrapeCommand buildAlcoaCommand() {
        return new ScrapeCommand(
                "alcoa_pocos_caldas_workday",
                "https://alcoa.wd5.myworkdayjobs.com/wday/cxs/alcoa/Careers/jobs",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR
        );
    }
}
