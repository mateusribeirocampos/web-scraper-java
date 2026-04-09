package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.strategy.PocosCaldasContestScraperStrategy;
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
@DisplayName("PocosCaldasContestImportUseCase")
class PocosCaldasContestImportUseCaseTest {

    @Mock
    private PublicContestPostingRepository publicContestPostingRepository;

    @Mock
    private PocosCaldasContestScraperStrategy strategy;

    @Mock
    private IdempotentPublicContestPersistenceService idempotentPersistenceService;

    @Test
    @DisplayName("should propagate scrape failures instead of persisting empty successful imports")
    void shouldPropagateScrapeFailuresInsteadOfPersistingEmptySuccessfulImports() {
        PocosCaldasContestImportUseCase useCase = new PocosCaldasContestImportUseCase(
                publicContestPostingRepository,
                strategy,
                new ContestPostingFingerprintCalculator(),
                idempotentPersistenceService
        );

        when(strategy.scrape(any())).thenReturn(ScrapeResult.failure(
                "municipal_pocos_caldas",
                "Failed to fetch PDF"
        ));

        assertThatThrownBy(() -> useCase.execute(buildSite(), buildExecution(), buildCommand()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Poços de Caldas municipal scrape failed");

        verify(idempotentPersistenceService, never()).persist(any());
    }

    @Test
    @DisplayName("should persist normalized contest item when scrape succeeds")
    void shouldPersistNormalizedContestItemWhenScrapeSucceeds() {
        PocosCaldasContestImportUseCase useCase = new PocosCaldasContestImportUseCase(
                publicContestPostingRepository,
                strategy,
                new ContestPostingFingerprintCalculator(),
                idempotentPersistenceService
        );

        when(strategy.scrape(any())).thenReturn(ScrapeResult.success(List.of(
                PublicContestPostingEntity.builder()
                        .externalId("municipal_pocos_caldas:processo-seletivo-001-2025")
                        .canonicalUrl("https://pocosdecaldas.mg.gov.br/wp-content/uploads/2025/09/EDITAL-DE-PROCESSO-SELETIVO-001-2025.pdf")
                        .contestName("Edital de Processo Seletivo Simplificado nº 001/2025")
                        .organizer("Prefeitura Municipal de Poços de Caldas")
                        .positionTitle("Processo seletivo simplificado para múltiplos cargos")
                        .state("MG")
                        .contestStatus(ContestStatus.OPEN)
                        .publishedAt(LocalDate.parse("2025-09-01"))
                        .payloadJson("{}")
                        .createdAt(Instant.parse("2026-04-09T12:00:00Z"))
                        .build()
        ), "municipal_pocos_caldas"));

        useCase.execute(buildSite(), buildExecution(), buildCommand());

        verify(idempotentPersistenceService).persist(any());
    }

    private static TargetSiteEntity buildSite() {
        return TargetSiteEntity.builder()
                .id(61L)
                .siteCode("municipal_pocos_caldas")
                .displayName("Prefeitura de Poços de Caldas - Processo Seletivo Simplificado")
                .baseUrl("https://pocosdecaldas.mg.gov.br/wp-content/uploads/2025/09/EDITAL-DE-PROCESSO-SELETIVO-001-2025.pdf")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("pocos_caldas_pdf_v1")
                .enabled(true)
                .createdAt(Instant.parse("2026-04-09T00:00:00Z"))
                .build();
    }

    private static CrawlExecutionEntity buildExecution() {
        return CrawlExecutionEntity.builder()
                .id(921L)
                .status(CrawlExecutionStatus.RUNNING)
                .startedAt(Instant.parse("2026-04-09T12:00:00Z"))
                .pagesVisited(0)
                .itemsFound(0)
                .retryCount(0)
                .createdAt(Instant.parse("2026-04-09T12:00:00Z"))
                .build();
    }

    private static ScrapeCommand buildCommand() {
        return new ScrapeCommand(
                "municipal_pocos_caldas",
                "https://pocosdecaldas.mg.gov.br/wp-content/uploads/2025/09/EDITAL-DE-PROCESSO-SELETIVO-001-2025.pdf",
                ExtractionMode.STATIC_HTML,
                JobCategory.PUBLIC_CONTEST
        );
    }
}
