package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.strategy.ExtremaContestScraperStrategy;
import com.campos.webscraper.domain.enums.ContestStatus;
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
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("ExtremaContestImportUseCase")
class ExtremaContestImportUseCaseTest {

    @Mock
    private PublicContestPostingRepository publicContestPostingRepository;

    @Mock
    private ExtremaContestScraperStrategy strategy;

    @Mock
    private IdempotentPublicContestPersistenceService idempotentPersistenceService;

    @Test
    @DisplayName("should propagate scrape failures instead of persisting empty successful imports")
    void shouldPropagateScrapeFailuresInsteadOfPersistingEmptySuccessfulImports() {
        ExtremaContestImportUseCase useCase = new ExtremaContestImportUseCase(
                publicContestPostingRepository,
                strategy,
                new ContestPostingFingerprintCalculator(),
                idempotentPersistenceService
        );

        when(strategy.scrape(any())).thenReturn(ScrapeResult.failure(
                "municipal_extrema",
                "Failed to fetch official Extrema education page"
        ));

        assertThatThrownBy(() -> useCase.execute(buildSite(), buildExecution(), buildCommand()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Extrema municipal scrape failed");

        verify(idempotentPersistenceService, never()).persist(any());
    }

    @Test
    @DisplayName("should persist normalized contest item when scrape succeeds")
    void shouldPersistNormalizedContestItemWhenScrapeSucceeds() {
        ExtremaContestImportUseCase useCase = new ExtremaContestImportUseCase(
                publicContestPostingRepository,
                strategy,
                new ContestPostingFingerprintCalculator(),
                idempotentPersistenceService
        );

        when(strategy.scrape(any())).thenReturn(ScrapeResult.success(List.of(
                PublicContestPostingEntity.builder()
                        .externalId("municipal_extrema:003-2025")
                        .canonicalUrl("https://www.extrema.mg.gov.br/secretarias/educacao/processo-seletivo-publico")
                        .contestName("Processo seletivo público simplificado para contratação de cargos/funções públicas para o quadro da Educação de Extrema")
                        .organizer("Prefeitura Municipal de Extrema")
                        .positionTitle("Professor de Educação Básica")
                        .state("MG")
                        .contestStatus(ContestStatus.REGISTRATION_CLOSED)
                        .publishedAt(LocalDate.parse("2025-07-17"))
                        .payloadJson("{}")
                        .createdAt(Instant.parse("2026-04-10T12:00:00Z"))
                        .build()
        ), "municipal_extrema"));

        useCase.execute(buildSite(), buildExecution(), buildCommand());

        verify(idempotentPersistenceService).persist(any());
    }

    private static TargetSiteEntity buildSite() {
        return TargetSiteEntity.builder()
                .id(71L)
                .siteCode("municipal_extrema")
                .displayName("Prefeitura de Extrema - Educação")
                .baseUrl("https://www.extrema.mg.gov.br/secretarias/educacao")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("extrema_html_v1")
                .enabled(true)
                .createdAt(Instant.parse("2026-04-10T00:00:00Z"))
                .build();
    }

    private static CrawlExecutionEntity buildExecution() {
        return CrawlExecutionEntity.builder()
                .id(931L)
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
                "municipal_extrema",
                "https://www.extrema.mg.gov.br/secretarias/educacao",
                ExtractionMode.STATIC_HTML,
                JobCategory.PUBLIC_CONTEST
        );
    }
}
