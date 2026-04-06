package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.strategy.CamaraSantaRitaContestScraperStrategy;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("CamaraSantaRitaContestImportUseCase")
class CamaraSantaRitaContestImportUseCaseTest {

    @Mock
    private PublicContestPostingRepository publicContestPostingRepository;

    @Mock
    private CamaraSantaRitaContestScraperStrategy strategy;

    @Mock
    private IdempotentPublicContestPersistenceService idempotentPersistenceService;

    @Test
    @DisplayName("should propagate scrape failures instead of persisting empty successful imports")
    void shouldPropagateScrapeFailuresInsteadOfPersistingEmptySuccessfulImports() {
        CamaraSantaRitaContestImportUseCase useCase = new CamaraSantaRitaContestImportUseCase(
                publicContestPostingRepository,
                strategy,
                new ContestPostingFingerprintCalculator(),
                idempotentPersistenceService
        );

        when(strategy.scrape(any())).thenReturn(ScrapeResult.failure(
                "camara_santa_rita_sapucai",
                "Santa Rita Câmara request failed with status 503"
        ));

        assertThatThrownBy(() -> useCase.execute(buildSite(), buildExecution(), buildCommand()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Santa Rita Câmara scrape failed")
                .hasMessageContaining("503");

        verify(idempotentPersistenceService, never()).persist(any());
    }

    @Test
    @DisplayName("should persist normalized items when scrape succeeds")
    void shouldPersistNormalizedItemsWhenScrapeSucceeds() {
        CamaraSantaRitaContestImportUseCase useCase = new CamaraSantaRitaContestImportUseCase(
                publicContestPostingRepository,
                strategy,
                new ContestPostingFingerprintCalculator(),
                idempotentPersistenceService
        );

        when(strategy.scrape(any())).thenReturn(ScrapeResult.success(List.of(
                PublicContestPostingEntity.builder()
                        .externalId("camara_santa_rita_sapucai:01-2025")
                        .canonicalUrl("https://example.org/edital-01.pdf")
                        .contestName("Edital nº 01/2025: Processo Seletivo Estagiários Nível Superior")
                        .organizer("Câmara Municipal de Santa Rita do Sapucaí")
                        .positionTitle("Estagiários Nível Superior")
                        .governmentLevel(com.campos.webscraper.domain.enums.GovernmentLevel.MUNICIPAL)
                        .state("MG")
                        .educationLevel(com.campos.webscraper.domain.enums.EducationLevel.SUPERIOR)
                        .editalUrl("https://example.org/edital-01.pdf")
                        .publishedAt(java.time.LocalDate.parse("2025-01-06"))
                        .contestStatus(com.campos.webscraper.domain.enums.ContestStatus.REGISTRATION_CLOSED)
                        .payloadJson("{}")
                        .createdAt(Instant.parse("2026-04-05T12:00:00Z"))
                        .build()
        ), "camara_santa_rita_sapucai"));

        useCase.execute(buildSite(), buildExecution(), buildCommand());

        verify(idempotentPersistenceService).persist(any());
    }

    private static TargetSiteEntity buildSite() {
        return TargetSiteEntity.builder()
                .id(31L)
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
    }

    private static CrawlExecutionEntity buildExecution() {
        return CrawlExecutionEntity.builder()
                .id(701L)
                .status(CrawlExecutionStatus.RUNNING)
                .startedAt(Instant.parse("2026-04-05T12:00:00Z"))
                .pagesVisited(0)
                .itemsFound(0)
                .retryCount(0)
                .createdAt(Instant.parse("2026-04-05T12:00:00Z"))
                .build();
    }

    private static ScrapeCommand buildCommand() {
        return new ScrapeCommand(
                "camara_santa_rita_sapucai",
                "https://www.santaritadosapucai.mg.leg.br/transparencia/processos-seletivos-2025",
                ExtractionMode.STATIC_HTML,
                JobCategory.PUBLIC_CONTEST
        );
    }
}
