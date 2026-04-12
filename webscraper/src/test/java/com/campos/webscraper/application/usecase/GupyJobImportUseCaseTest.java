package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.strategy.GupyJobScraperStrategy;
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
@DisplayName("GupyJobImportUseCase")
class GupyJobImportUseCaseTest {

    @Mock
    private JobPostingRepository jobPostingRepository;

    @Mock
    private GupyJobScraperStrategy strategy;

    @Mock
    private IdempotentJobPostingPersistenceService idempotentPersistenceService;

    @Test
    @DisplayName("should propagate scrape failures instead of persisting empty successful imports")
    void shouldPropagateScrapeFailuresInsteadOfPersistingEmptySuccessfulImports() {
        GupyJobImportUseCase useCase = new GupyJobImportUseCase(
                jobPostingRepository,
                strategy,
                new JobPostingFingerprintCalculator(),
                idempotentPersistenceService
        );

        when(strategy.scrape(any())).thenReturn(ScrapeResult.failure(
                "gupy_specialdog_extrema",
                "Gupy API returned 503 Service Unavailable"
        ));

        assertThatThrownBy(() -> useCase.execute(buildSite(), buildExecution(), buildCommand()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Gupy scrape failed")
                .hasMessageContaining("503");

        verify(idempotentPersistenceService, never()).persist(any());
    }

    @Test
    @DisplayName("should persist normalized items when Special Dog scrape succeeds")
    void shouldPersistNormalizedItemsWhenSpecialDogScrapeSucceeds() {
        GupyJobImportUseCase useCase = new GupyJobImportUseCase(
                jobPostingRepository,
                strategy,
                new JobPostingFingerprintCalculator(),
                idempotentPersistenceService
        );

        when(strategy.scrape(any())).thenReturn(ScrapeResult.success(List.of(
                JobPostingEntity.builder()
                        .externalId("98765")
                        .canonicalUrl("https://portal.gupy.io/job/98765")
                        .title("Desenvolvedor Java Júnior")
                        .company("Special Dog Company")
                        .location("Extrema, MG")
                        .remote(false)
                        .publishedAt(LocalDate.parse("2026-04-10"))
                        .payloadJson("{}")
                        .createdAt(Instant.parse("2026-04-10T12:00:00Z"))
                        .build()
        ), "gupy_specialdog_extrema"));

        useCase.execute(buildSite(), buildExecution(), buildCommand());

        verify(idempotentPersistenceService).persist(any());
    }

    private static TargetSiteEntity buildSite() {
        return TargetSiteEntity.builder()
                .id(61L)
                .siteCode("gupy_specialdog_extrema")
                .displayName("Special Dog Company Careers via Gupy - Extrema")
                .baseUrl("https://portal.api.gupy.io/api/v1/jobs?careerPageName=Special%20Dog%20Company&city=Extrema")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("n/a")
                .enabled(true)
                .createdAt(Instant.parse("2026-04-10T00:00:00Z"))
                .build();
    }

    private static CrawlExecutionEntity buildExecution() {
        return CrawlExecutionEntity.builder()
                .id(901L)
                .status(CrawlExecutionStatus.RUNNING)
                .startedAt(Instant.parse("2026-04-10T12:00:00Z"))
                .pagesVisited(0)
                .itemsFound(0)
                .retryCount(0)
                .createdAt(Instant.parse("2026-04-10T12:00:00Z"))
                .build();
    }

    private static ScrapeCommand buildCommand() {
        return new ScrapeCommand(
                "gupy_specialdog_extrema",
                "https://portal.api.gupy.io/api/v1/jobs?careerPageName=Special%20Dog%20Company&city=Extrema",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR
        );
    }
}
