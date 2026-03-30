package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.strategy.MunhozContestScraperStrategy;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.PublicContestPostingRepository;
import com.campos.webscraper.shared.ContestPostingFingerprintCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("MunhozContestImportUseCase")
class MunhozContestImportUseCaseTest {

    @Mock
    private PublicContestPostingRepository publicContestPostingRepository;

    @Mock
    private MunhozContestScraperStrategy strategy;

    @Mock
    private IdempotentPublicContestPersistenceService idempotentPersistenceService;

    @Test
    @DisplayName("should propagate scrape failures instead of persisting empty successful imports")
    void shouldPropagateScrapeFailuresInsteadOfPersistingEmptySuccessfulImports() {
        MunhozContestImportUseCase useCase = new MunhozContestImportUseCase(
                publicContestPostingRepository,
                strategy,
                new ContestPostingFingerprintCalculator(),
                idempotentPersistenceService
        );

        when(strategy.scrape(any())).thenReturn(ScrapeResult.failure(
                "municipal_munhoz",
                "Munhoz listing request failed with status 503"
        ));

        assertThatThrownBy(() -> useCase.execute(buildSite(), buildExecution(), buildCommand()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Munhoz scrape failed")
                .hasMessageContaining("503");

        verify(idempotentPersistenceService, never()).persist(any());
    }

    private static TargetSiteEntity buildSite() {
        Instant now = Instant.parse("2026-03-30T12:00:00Z");
        return TargetSiteEntity.builder()
                .id(277L)
                .siteCode("municipal_munhoz")
                .displayName("Prefeitura de Munhoz - Concursos")
                .baseUrl("https://www.munhoz.mg.gov.br/concursos-publicos")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("munhoz_html_v1")
                .enabled(true)
                .createdAt(now)
                .build();
    }

    private static CrawlExecutionEntity buildExecution() {
        Instant now = Instant.parse("2026-03-30T12:05:00Z");
        CrawlJobEntity crawlJob = CrawlJobEntity.builder()
                .id(288L)
                .targetSite(buildSite())
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .scheduledAt(now)
                .createdAt(now)
                .build();
        return CrawlExecutionEntity.builder()
                .id(299L)
                .crawlJob(crawlJob)
                .status(CrawlExecutionStatus.RUNNING)
                .startedAt(now)
                .pagesVisited(0)
                .itemsFound(0)
                .retryCount(0)
                .createdAt(now)
                .build();
    }

    private static ScrapeCommand buildCommand() {
        return new ScrapeCommand(
                "municipal_munhoz",
                "https://www.munhoz.mg.gov.br/concursos-publicos",
                ExtractionMode.STATIC_HTML,
                JobCategory.PUBLIC_CONTEST
        );
    }
}
