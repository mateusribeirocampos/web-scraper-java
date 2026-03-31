package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.strategy.CampinasContestScraperStrategy;
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
@DisplayName("CampinasContestImportUseCase")
class CampinasContestImportUseCaseTest {

    @Mock
    private PublicContestPostingRepository publicContestPostingRepository;

    @Mock
    private CampinasContestScraperStrategy strategy;

    @Mock
    private IdempotentPublicContestPersistenceService idempotentPersistenceService;

    @Test
    @DisplayName("should propagate Campinas scrape failures instead of persisting empty successful imports")
    void shouldPropagateCampinasScrapeFailuresInsteadOfPersistingEmptySuccessfulImports() {
        CampinasContestImportUseCase useCase = new CampinasContestImportUseCase(
                publicContestPostingRepository,
                strategy,
                new ContestPostingFingerprintCalculator(),
                idempotentPersistenceService
        );

        when(strategy.scrape(any())).thenReturn(ScrapeResult.failure(
                "municipal_campinas",
                "Campinas official contests request failed with status 503"
        ));

        assertThatThrownBy(() -> useCase.execute(buildSite(), buildExecution(), buildCommand()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Campinas scrape failed")
                .hasMessageContaining("503");

        verify(idempotentPersistenceService, never()).persist(any());
    }

    private static TargetSiteEntity buildSite() {
        Instant now = Instant.parse("2026-03-31T12:00:00Z");
        return TargetSiteEntity.builder()
                .id(211L)
                .siteCode("municipal_campinas")
                .displayName("Prefeitura de Campinas - Concursos")
                .baseUrl("https://portal-api.campinas.sp.gov.br/jsonapi/node/site?filter%5Bdrupal_internal__nid%5D=113658")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("campinas_jsonapi_v1")
                .enabled(true)
                .createdAt(now)
                .build();
    }

    private static CrawlExecutionEntity buildExecution() {
        Instant now = Instant.parse("2026-03-31T12:05:00Z");
        CrawlJobEntity crawlJob = CrawlJobEntity.builder()
                .id(212L)
                .targetSite(buildSite())
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .scheduledAt(now)
                .createdAt(now)
                .build();
        return CrawlExecutionEntity.builder()
                .id(213L)
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
                "municipal_campinas",
                "https://portal-api.campinas.sp.gov.br/jsonapi/node/site?filter%5Bdrupal_internal__nid%5D=113658",
                ExtractionMode.API,
                JobCategory.PUBLIC_CONTEST
        );
    }
}
