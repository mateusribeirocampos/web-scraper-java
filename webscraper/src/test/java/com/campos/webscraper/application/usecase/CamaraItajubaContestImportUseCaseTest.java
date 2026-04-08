package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.strategy.CamaraItajubaContestScraperStrategy;
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
@DisplayName("CamaraItajubaContestImportUseCase")
class CamaraItajubaContestImportUseCaseTest {

    @Mock
    private PublicContestPostingRepository publicContestPostingRepository;

    @Mock
    private CamaraItajubaContestScraperStrategy strategy;

    @Mock
    private IdempotentPublicContestPersistenceService idempotentPersistenceService;

    @Test
    @DisplayName("should propagate scrape failures instead of persisting empty successful imports")
    void shouldPropagateScrapeFailuresInsteadOfPersistingEmptySuccessfulImports() {
        CamaraItajubaContestImportUseCase useCase = new CamaraItajubaContestImportUseCase(
                publicContestPostingRepository,
                strategy,
                new ContestPostingFingerprintCalculator(),
                idempotentPersistenceService
        );

        when(strategy.scrape(any())).thenReturn(ScrapeResult.failure(
                "camara_itajuba",
                "Itajubá Câmara request failed with status 503"
        ));

        assertThatThrownBy(() -> useCase.execute(buildSite(), buildExecution(), buildCommand()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Itajubá Câmara scrape failed")
                .hasMessageContaining("503");

        verify(idempotentPersistenceService, never()).persist(any());
    }

    @Test
    @DisplayName("should persist normalized items when scrape succeeds")
    void shouldPersistNormalizedItemsWhenScrapeSucceeds() {
        CamaraItajubaContestImportUseCase useCase = new CamaraItajubaContestImportUseCase(
                publicContestPostingRepository,
                strategy,
                new ContestPostingFingerprintCalculator(),
                idempotentPersistenceService
        );

        when(strategy.scrape(any())).thenReturn(ScrapeResult.success(List.of(
                PublicContestPostingEntity.builder()
                        .externalId("camara_itajuba:concurso-publico-2023")
                        .canonicalUrl("https://itajuba.cam.mg.gov.br/site/wp-content/uploads/2023/12/EDITAL_CMI_2023.pdf")
                        .contestName("Concurso Público 2023")
                        .organizer("Câmara Municipal de Itajubá")
                        .positionTitle("Cargos efetivos diversos")
                        .governmentLevel(com.campos.webscraper.domain.enums.GovernmentLevel.MUNICIPAL)
                        .state("MG")
                        .educationLevel(com.campos.webscraper.domain.enums.EducationLevel.UNKNOWN)
                        .editalUrl("https://itajuba.cam.mg.gov.br/site/wp-content/uploads/2023/12/EDITAL_CMI_2023.pdf")
                        .publishedAt(java.time.LocalDate.parse("2023-12-13"))
                        .contestStatus(com.campos.webscraper.domain.enums.ContestStatus.REGISTRATION_CLOSED)
                        .payloadJson("{}")
                        .createdAt(Instant.parse("2026-04-06T12:00:00Z"))
                        .build()
        ), "camara_itajuba"));

        useCase.execute(buildSite(), buildExecution(), buildCommand());

        verify(idempotentPersistenceService).persist(any());
    }

    private static TargetSiteEntity buildSite() {
        return TargetSiteEntity.builder()
                .id(41L)
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
    }

    private static CrawlExecutionEntity buildExecution() {
        return CrawlExecutionEntity.builder()
                .id(801L)
                .status(CrawlExecutionStatus.RUNNING)
                .startedAt(Instant.parse("2026-04-06T12:00:00Z"))
                .pagesVisited(0)
                .itemsFound(0)
                .retryCount(0)
                .createdAt(Instant.parse("2026-04-06T12:00:00Z"))
                .build();
    }

    private static ScrapeCommand buildCommand() {
        return new ScrapeCommand(
                "camara_itajuba",
                "https://itajuba.cam.mg.gov.br/site/camara-municipal-de-itajuba-lanca-concurso-publico-para-preenchimento-de-cargos-efetivos/",
                ExtractionMode.STATIC_HTML,
                JobCategory.PUBLIC_CONTEST
        );
    }
}
