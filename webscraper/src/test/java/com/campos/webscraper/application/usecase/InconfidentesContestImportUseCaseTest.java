package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.strategy.InconfidentesContestScraperStrategy;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.CrawlJobEntity;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("InconfidentesContestImportUseCase")
class InconfidentesContestImportUseCaseTest {

    @Mock
    private PublicContestPostingRepository publicContestPostingRepository;

    @Mock
    private InconfidentesContestScraperStrategy strategy;

    @Mock
    private IdempotentPublicContestPersistenceService idempotentPersistenceService;

    @Test
    @DisplayName("should propagate scrape failures instead of persisting empty successful imports")
    void shouldPropagateScrapeFailuresInsteadOfPersistingEmptySuccessfulImports() {
        InconfidentesContestImportUseCase useCase = new InconfidentesContestImportUseCase(
                publicContestPostingRepository,
                strategy,
                new ContestPostingFingerprintCalculator(),
                idempotentPersistenceService
        );

        when(strategy.scrape(any())).thenReturn(ScrapeResult.failure(
                "municipal_inconfidentes",
                "Inconfidentes request failed with status 503"
        ));

        assertThatThrownBy(() -> useCase.execute(buildSite(), buildExecution(), buildCommand()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Inconfidentes scrape failed")
                .hasMessageContaining("503");

        verify(idempotentPersistenceService, never()).persist(any());
    }

    @Test
    @DisplayName("should update mutable fields when the same contest is republished under a new edital url")
    void shouldUpdateMutableFieldsWhenTheSameContestIsRepublishedUnderANewEditalUrl() {
        InconfidentesContestImportUseCase useCase = new InconfidentesContestImportUseCase(
                publicContestPostingRepository,
                strategy,
                new ContestPostingFingerprintCalculator(),
                idempotentPersistenceService
        );

        PublicContestPostingEntity candidate = PublicContestPostingEntity.builder()
                .externalId("municipal_inconfidentes:educacao-edital-008-2026")
                .canonicalUrl("https://inconfidentes.mg.gov.br/editais-concursos-e-processos-seletivos#municipal_inconfidentes:educacao-edital-008-2026")
                .contestName("EDITAL 008/2026 - PROCESSO SELETIVO 008/2026 - CONTRATACAO DE PROFESSOR")
                .organizer("Prefeitura Municipal de Inconfidentes - DEPARTAMENTO DE EDUCACAO")
                .positionTitle("Professor")
                .governmentLevel(com.campos.webscraper.domain.enums.GovernmentLevel.MUNICIPAL)
                .state("MG")
                .educationLevel(com.campos.webscraper.domain.enums.EducationLevel.SUPERIOR)
                .editalUrl("https://ecrie.com.br/edital-retificado.pdf")
                .publishedAt(java.time.LocalDate.parse("2026-01-01"))
                .contestStatus(com.campos.webscraper.domain.enums.ContestStatus.REGISTRATION_CLOSED)
                .payloadJson("{\"editalUrl\":\"https://ecrie.com.br/edital-retificado.pdf\"}")
                .createdAt(Instant.parse("2026-03-26T12:10:00Z"))
                .build();

        PublicContestPostingEntity existing = PublicContestPostingEntity.builder()
                .id(501L)
                .crawlExecution(buildExecution())
                .targetSite(buildSite())
                .externalId("municipal_inconfidentes:educacao-edital-008-2026")
                .canonicalUrl("https://inconfidentes.mg.gov.br/editais-concursos-e-processos-seletivos#municipal_inconfidentes:educacao-edital-008-2026")
                .contestName(candidate.getContestName())
                .organizer(candidate.getOrganizer())
                .positionTitle(candidate.getPositionTitle())
                .governmentLevel(candidate.getGovernmentLevel())
                .state(candidate.getState())
                .educationLevel(candidate.getEducationLevel())
                .editalUrl("https://ecrie.com.br/edital-original.pdf")
                .publishedAt(candidate.getPublishedAt())
                .contestStatus(candidate.getContestStatus())
                .fingerprintHash("fp-008")
                .dedupStatus(com.campos.webscraper.domain.enums.DedupStatus.NEW)
                .payloadJson("{\"editalUrl\":\"https://ecrie.com.br/edital-original.pdf\"}")
                .createdAt(Instant.parse("2026-03-25T12:10:00Z"))
                .build();

        PublicContestPostingEntity savedUpdated = PublicContestPostingEntity.builder()
                .id(existing.getId())
                .crawlExecution(buildExecution())
                .targetSite(buildSite())
                .externalId(existing.getExternalId())
                .canonicalUrl(existing.getCanonicalUrl())
                .contestName(existing.getContestName())
                .organizer(existing.getOrganizer())
                .positionTitle(existing.getPositionTitle())
                .governmentLevel(existing.getGovernmentLevel())
                .state(existing.getState())
                .educationLevel(existing.getEducationLevel())
                .editalUrl(candidate.getEditalUrl())
                .publishedAt(existing.getPublishedAt())
                .contestStatus(existing.getContestStatus())
                .fingerprintHash("fp-008")
                .dedupStatus(com.campos.webscraper.domain.enums.DedupStatus.UPDATED)
                .payloadJson(candidate.getPayloadJson())
                .createdAt(existing.getCreatedAt())
                .updatedAt(Instant.parse("2026-03-26T12:11:00Z"))
                .build();

        when(strategy.scrape(any())).thenReturn(ScrapeResult.success(List.of(candidate), "municipal_inconfidentes"));
        when(publicContestPostingRepository.findByTargetSiteAndExternalId(any(), anyString())).thenReturn(Optional.of(existing));
        when(publicContestPostingRepository.save(any())).thenReturn(savedUpdated);

        useCase.execute(buildSite(), buildExecution(), buildCommand());

        verify(idempotentPersistenceService).persist(List.of());
        verify(publicContestPostingRepository).save(argThat(updated ->
                updated.getId().equals(501L)
                        && "https://ecrie.com.br/edital-retificado.pdf".equals(updated.getEditalUrl())
                        && updated.getPayloadJson().contains("edital-retificado.pdf")
                        && updated.getDedupStatus() == com.campos.webscraper.domain.enums.DedupStatus.UPDATED
        ));
    }

    @Test
    @DisplayName("should keep existing posting untouched when recrawl finds no material changes")
    void shouldKeepExistingPostingUntouchedWhenRecrawlFindsNoMaterialChanges() {
        InconfidentesContestImportUseCase useCase = new InconfidentesContestImportUseCase(
                publicContestPostingRepository,
                strategy,
                new ContestPostingFingerprintCalculator(),
                idempotentPersistenceService
        );

        PublicContestPostingEntity candidate = PublicContestPostingEntity.builder()
                .externalId("municipal_inconfidentes:educacao-edital-008-2026")
                .canonicalUrl("https://inconfidentes.mg.gov.br/editais-concursos-e-processos-seletivos#municipal_inconfidentes:educacao-edital-008-2026")
                .contestName("EDITAL 008/2026 - PROCESSO SELETIVO 008/2026 - CONTRATACAO DE PROFESSOR")
                .organizer("Prefeitura Municipal de Inconfidentes - DEPARTAMENTO DE EDUCACAO")
                .positionTitle("Professor")
                .governmentLevel(com.campos.webscraper.domain.enums.GovernmentLevel.MUNICIPAL)
                .state("MG")
                .educationLevel(com.campos.webscraper.domain.enums.EducationLevel.SUPERIOR)
                .editalUrl("https://ecrie.com.br/edital-original.pdf")
                .publishedAt(java.time.LocalDate.parse("2026-01-01"))
                .contestStatus(com.campos.webscraper.domain.enums.ContestStatus.REGISTRATION_CLOSED)
                .payloadJson("{\"editalUrl\":\"https://ecrie.com.br/edital-original.pdf\"}")
                .createdAt(Instant.parse("2026-03-26T12:10:00Z"))
                .build();

        PublicContestPostingEntity existing = PublicContestPostingEntity.builder()
                .id(501L)
                .crawlExecution(buildExecution())
                .targetSite(buildSite())
                .externalId(candidate.getExternalId())
                .canonicalUrl(candidate.getCanonicalUrl())
                .contestName(candidate.getContestName())
                .organizer(candidate.getOrganizer())
                .positionTitle(candidate.getPositionTitle())
                .governmentLevel(candidate.getGovernmentLevel())
                .state(candidate.getState())
                .educationLevel(candidate.getEducationLevel())
                .editalUrl(candidate.getEditalUrl())
                .publishedAt(candidate.getPublishedAt())
                .contestStatus(candidate.getContestStatus())
                .fingerprintHash("fp-008")
                .dedupStatus(com.campos.webscraper.domain.enums.DedupStatus.NEW)
                .payloadJson(candidate.getPayloadJson())
                .createdAt(Instant.parse("2026-03-25T12:10:00Z"))
                .build();

        when(strategy.scrape(any())).thenReturn(ScrapeResult.success(List.of(candidate), "municipal_inconfidentes"));
        when(publicContestPostingRepository.findByTargetSiteAndExternalId(any(), anyString())).thenReturn(Optional.of(existing));

        useCase.execute(buildSite(), buildExecution(), buildCommand());

        verify(idempotentPersistenceService).persist(List.of());
        verify(publicContestPostingRepository, never()).save(any());
    }

    @Test
    @DisplayName("should match existing contest by stable external id when title wording changes")
    void shouldMatchExistingContestByStableExternalIdWhenTitleWordingChanges() {
        InconfidentesContestImportUseCase useCase = new InconfidentesContestImportUseCase(
                publicContestPostingRepository,
                strategy,
                new ContestPostingFingerprintCalculator(),
                idempotentPersistenceService
        );

        PublicContestPostingEntity candidate = PublicContestPostingEntity.builder()
                .externalId("municipal_inconfidentes:departamento-de-educacao-011-2026")
                .canonicalUrl("https://inconfidentes.mg.gov.br/editais-concursos-e-processos-seletivos#municipal_inconfidentes:departamento-de-educacao-011-2026")
                .contestName("EDITAL 011/2026 - PROCESSO SELETIVO 011/2026 - CONTRATACAO DE PROFESSOR RETIFICADO")
                .organizer("Prefeitura Municipal de Inconfidentes - DEPARTAMENTO DE EDUCACAO")
                .positionTitle("Professor")
                .governmentLevel(com.campos.webscraper.domain.enums.GovernmentLevel.MUNICIPAL)
                .state("MG")
                .educationLevel(com.campos.webscraper.domain.enums.EducationLevel.SUPERIOR)
                .editalUrl("https://ecrie.com.br/edital-011-retificado.pdf")
                .publishedAt(java.time.LocalDate.parse("2026-01-01"))
                .contestStatus(com.campos.webscraper.domain.enums.ContestStatus.REGISTRATION_CLOSED)
                .payloadJson("{\"contestTitle\":\"EDITAL 011/2026 - PROCESSO SELETIVO 011/2026 - CONTRATACAO DE PROFESSOR RETIFICADO\"}")
                .createdAt(Instant.parse("2026-03-26T12:10:00Z"))
                .fingerprintHash("new-fingerprint")
                .build();

        PublicContestPostingEntity existing = PublicContestPostingEntity.builder()
                .id(777L)
                .crawlExecution(buildExecution())
                .targetSite(buildSite())
                .externalId(candidate.getExternalId())
                .canonicalUrl(candidate.getCanonicalUrl())
                .contestName("EDITAL 011/2026 - PROCESSO SELETIVO 011/2026 - CONTRATACAO DE PROFESSOR")
                .organizer(candidate.getOrganizer())
                .positionTitle(candidate.getPositionTitle())
                .governmentLevel(candidate.getGovernmentLevel())
                .state(candidate.getState())
                .educationLevel(candidate.getEducationLevel())
                .editalUrl("https://ecrie.com.br/edital-011.pdf")
                .publishedAt(candidate.getPublishedAt())
                .contestStatus(candidate.getContestStatus())
                .fingerprintHash("old-fingerprint")
                .dedupStatus(com.campos.webscraper.domain.enums.DedupStatus.NEW)
                .payloadJson("{\"contestTitle\":\"EDITAL 011/2026 - PROCESSO SELETIVO 011/2026 - CONTRATACAO DE PROFESSOR\"}")
                .createdAt(Instant.parse("2026-03-25T12:10:00Z"))
                .build();

        when(strategy.scrape(any())).thenReturn(ScrapeResult.success(List.of(candidate), "municipal_inconfidentes"));
        when(publicContestPostingRepository.findByTargetSiteAndExternalId(any(), anyString())).thenReturn(Optional.of(existing));
        when(publicContestPostingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(buildSite(), buildExecution(), buildCommand());

        verify(publicContestPostingRepository, never()).findByFingerprintHash(anyString());
        verify(publicContestPostingRepository).save(argThat(updated ->
                updated.getId().equals(777L)
                        && updated.getExternalId().equals(existing.getExternalId())
                        && updated.getContestName().contains("RETIFICADO")
                        && updated.getEditalUrl().contains("retificado")
        ));
    }

    @Test
    @DisplayName("should deduplicate repeated stable external ids within the same scrape batch")
    void shouldDeduplicateRepeatedStableExternalIdsWithinTheSameScrapeBatch() {
        InconfidentesContestImportUseCase useCase = new InconfidentesContestImportUseCase(
                publicContestPostingRepository,
                strategy,
                new ContestPostingFingerprintCalculator(),
                idempotentPersistenceService
        );

        PublicContestPostingEntity original = PublicContestPostingEntity.builder()
                .externalId("municipal_inconfidentes:departamento-de-educacao-012-2026")
                .canonicalUrl("https://inconfidentes.mg.gov.br/editais-concursos-e-processos-seletivos#municipal_inconfidentes:departamento-de-educacao-012-2026")
                .contestName("EDITAL 012/2026 - PROCESSO SELETIVO 012/2026 - CONTRATACAO DE PROFESSOR")
                .organizer("Prefeitura Municipal de Inconfidentes - DEPARTAMENTO DE EDUCACAO")
                .positionTitle("Professor")
                .governmentLevel(com.campos.webscraper.domain.enums.GovernmentLevel.MUNICIPAL)
                .state("MG")
                .educationLevel(com.campos.webscraper.domain.enums.EducationLevel.SUPERIOR)
                .editalUrl("https://ecrie.com.br/edital-012.pdf")
                .publishedAt(java.time.LocalDate.parse("2026-01-01"))
                .contestStatus(com.campos.webscraper.domain.enums.ContestStatus.OPEN)
                .payloadJson("{\"contestTitle\":\"EDITAL 012/2026 - PROCESSO SELETIVO 012/2026 - CONTRATACAO DE PROFESSOR\"}")
                .createdAt(Instant.parse("2026-03-26T12:10:00Z"))
                .fingerprintHash("fp-original")
                .build();

        PublicContestPostingEntity retificado = PublicContestPostingEntity.builder()
                .externalId(original.getExternalId())
                .canonicalUrl(original.getCanonicalUrl())
                .contestName("EDITAL 012/2026 - PROCESSO SELETIVO 012/2026 - CONTRATACAO DE PROFESSOR RETIFICADO")
                .organizer(original.getOrganizer())
                .positionTitle(original.getPositionTitle())
                .governmentLevel(original.getGovernmentLevel())
                .state(original.getState())
                .educationLevel(original.getEducationLevel())
                .editalUrl("https://ecrie.com.br/edital-012-retificado.pdf")
                .publishedAt(original.getPublishedAt())
                .contestStatus(original.getContestStatus())
                .payloadJson("{\"contestTitle\":\"EDITAL 012/2026 - PROCESSO SELETIVO 012/2026 - CONTRATACAO DE PROFESSOR RETIFICADO\"}")
                .createdAt(Instant.parse("2026-03-26T12:11:00Z"))
                .fingerprintHash("fp-retificado")
                .build();

        when(strategy.scrape(any())).thenReturn(ScrapeResult.success(List.of(original, retificado), "municipal_inconfidentes"));
        when(publicContestPostingRepository.findByTargetSiteAndExternalId(any(), anyString())).thenReturn(Optional.empty());
        when(publicContestPostingRepository.findByFingerprintHash(anyString())).thenReturn(Optional.empty());
        when(idempotentPersistenceService.persist(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(buildSite(), buildExecution(), buildCommand());

        verify(idempotentPersistenceService).persist(argThat(items ->
                items.size() == 1
                        && items.getFirst().getExternalId().equals(original.getExternalId())
                        && items.getFirst().getContestName().contains("RETIFICADO")
                        && items.getFirst().getEditalUrl().contains("retificado")
        ));
        verify(publicContestPostingRepository, never()).save(any());
    }

    private static TargetSiteEntity buildSite() {
        Instant now = Instant.parse("2026-03-26T12:00:00Z");
        return TargetSiteEntity.builder()
                .id(77L)
                .siteCode("municipal_inconfidentes")
                .displayName("Prefeitura de Inconfidentes - Editais")
                .baseUrl("https://inconfidentes.mg.gov.br/editais-concursos-e-processos-seletivos")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("inconfidentes_html_v1")
                .enabled(true)
                .createdAt(now)
                .build();
    }

    private static CrawlExecutionEntity buildExecution() {
        Instant now = Instant.parse("2026-03-26T12:05:00Z");
        CrawlJobEntity crawlJob = CrawlJobEntity.builder()
                .id(88L)
                .targetSite(buildSite())
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .scheduledAt(now)
                .createdAt(now)
                .build();
        return CrawlExecutionEntity.builder()
                .id(99L)
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
                "municipal_inconfidentes",
                "https://inconfidentes.mg.gov.br/editais-concursos-e-processos-seletivos",
                ExtractionMode.STATIC_HTML,
                JobCategory.PUBLIC_CONTEST
        );
    }
}
