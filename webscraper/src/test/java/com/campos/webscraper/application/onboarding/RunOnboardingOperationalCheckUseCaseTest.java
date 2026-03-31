package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.application.usecase.BootstrappedCrawlJob;
import com.campos.webscraper.application.usecase.TargetSiteSmokeRunResult;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.domain.enums.DedupStatus;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.JobContractType;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.CrawlExecutionRepository;
import com.campos.webscraper.domain.repository.CrawlJobRepository;
import com.campos.webscraper.domain.repository.JobPostingRepository;
import com.campos.webscraper.domain.repository.PublicContestPostingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("RunOnboardingOperationalCheckUseCase")
class RunOnboardingOperationalCheckUseCaseTest {

    @Mock
    private BootstrapOnboardingProfileWorkflowUseCase bootstrapOnboardingProfileWorkflowUseCase;

    @Mock
    private CrawlJobRepository crawlJobRepository;

    @Mock
    private CrawlExecutionRepository crawlExecutionRepository;

    @Mock
    private JobPostingRepository jobPostingRepository;

    @Mock
    private PublicContestPostingRepository publicContestPostingRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-25T12:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("should return consolidated operational summary with smoke run execution and postings sample")
    void shouldReturnConsolidatedOperationalSummaryWithSmokeRunExecutionAndPostingsSample() {
        TargetSiteEntity site = targetSite();
        CrawlJobEntity canonicalJob = canonicalJob(site);
        CrawlJobEntity transientJob = transientJob(site);
        BootstrappedOnboardingWorkflowResult workflow = new BootstrappedOnboardingWorkflowResult(
                "greenhouse_bitso",
                new BootstrappedTargetSite("greenhouse_bitso", BootstrapStatus.CREATED, site),
                new BootstrappedCrawlJob(BootstrapStatus.CREATED, canonicalJob),
                true,
                new TargetSiteSmokeRunResult(
                        7L,
                        "greenhouse_bitso",
                        101L,
                        BootstrapStatus.UPDATED,
                        "DISPATCHED",
                        CrawlExecutionStatus.SUCCEEDED
                )
        );

        when(bootstrapOnboardingProfileWorkflowUseCase.execute("greenhouse_bitso", true)).thenReturn(workflow);
        when(crawlJobRepository.findById(101L)).thenReturn(Optional.of(transientJob));
        CrawlExecutionEntity observedExecution = CrawlExecutionEntity.builder()
                .id(501L)
                .crawlJob(transientJob)
                .status(CrawlExecutionStatus.SUCCEEDED)
                .itemsFound(12)
                .startedAt(Instant.parse("2026-03-25T12:00:00Z"))
                .finishedAt(Instant.parse("2026-03-25T12:00:10Z"))
                .createdAt(Instant.parse("2026-03-25T12:00:10Z"))
                .build();
        when(crawlExecutionRepository.findByCrawlJob(transientJob)).thenReturn(List.of(observedExecution));
        when(jobPostingRepository.findTop5ByCrawlExecutionAndPublishedAtGreaterThanEqualOrderByPublishedAtDesc(
                org.mockito.ArgumentMatchers.eq(observedExecution),
                org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(List.of(
                posting(site, observedExecution, 1L, "Java Backend Developer"),
                posting(site, observedExecution, 2L, "Software Engineer")
        ));
        when(jobPostingRepository.countByCrawlExecutionAndPublishedAtGreaterThanEqual(
                org.mockito.ArgumentMatchers.eq(observedExecution),
                org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(2L);

        RunOnboardingOperationalCheckUseCase useCase = new RunOnboardingOperationalCheckUseCase(
                bootstrapOnboardingProfileWorkflowUseCase,
                crawlJobRepository,
                crawlExecutionRepository,
                jobPostingRepository,
                publicContestPostingRepository,
                clock
        );

        OnboardingOperationalCheckResult result = useCase.execute("greenhouse_bitso", true, 60);

        assertThat(result.profileKey()).isEqualTo("greenhouse_bitso");
        assertThat(result.executionSummary()).isNotNull();
        assertThat(result.executionSummary().crawlJobId()).isEqualTo(101L);
        assertThat(result.executionSummary().status()).isEqualTo("SUCCEEDED");
        assertThat(result.recentPostingsCount()).isEqualTo(2);
        assertThat(result.recentPostingsSample()).extracting(OnboardingRecentPostingSample::title)
                .containsExactly("Java Backend Developer", "Software Engineer");
    }

    @Test
    @DisplayName("should return null execution summary when no execution was observed")
    void shouldReturnNullExecutionSummaryWhenNoExecutionWasObserved() {
        TargetSiteEntity site = targetSite();
        CrawlJobEntity canonicalJob = canonicalJob(site);
        BootstrappedOnboardingWorkflowResult workflow = new BootstrappedOnboardingWorkflowResult(
                "greenhouse_bitso",
                new BootstrappedTargetSite("greenhouse_bitso", BootstrapStatus.UPDATED, site),
                new BootstrappedCrawlJob(BootstrapStatus.UPDATED, canonicalJob),
                false,
                null
        );

        when(bootstrapOnboardingProfileWorkflowUseCase.execute("greenhouse_bitso", false)).thenReturn(workflow);
        when(crawlJobRepository.findById(11L)).thenReturn(Optional.of(canonicalJob));
        when(crawlExecutionRepository.findByCrawlJob(canonicalJob)).thenReturn(List.of());
        RunOnboardingOperationalCheckUseCase useCase = new RunOnboardingOperationalCheckUseCase(
                bootstrapOnboardingProfileWorkflowUseCase,
                crawlJobRepository,
                crawlExecutionRepository,
                jobPostingRepository,
                publicContestPostingRepository,
                clock
        );

        OnboardingOperationalCheckResult result = useCase.execute("greenhouse_bitso", false, 60);

        assertThat(result.executionSummary()).isNull();
        assertThat(result.recentPostingsCount()).isZero();
        assertThat(result.recentPostingsSample()).isEmpty();
    }

    @Test
    @DisplayName("should preserve execution summary when smoke run was skipped in flight")
    void shouldPreserveExecutionSummaryWhenSmokeRunWasSkippedInFlight() {
        TargetSiteEntity site = targetSite();
        CrawlJobEntity canonicalJob = canonicalJob(site);
        BootstrappedOnboardingWorkflowResult workflow = new BootstrappedOnboardingWorkflowResult(
                "greenhouse_bitso",
                new BootstrappedTargetSite("greenhouse_bitso", BootstrapStatus.UPDATED, site),
                new BootstrappedCrawlJob(BootstrapStatus.UPDATED, canonicalJob),
                true,
                new TargetSiteSmokeRunResult(
                        7L,
                        "greenhouse_bitso",
                        11L,
                        BootstrapStatus.UPDATED,
                        "SKIPPED_IN_FLIGHT",
                        null
                )
        );

        when(bootstrapOnboardingProfileWorkflowUseCase.execute("greenhouse_bitso", true)).thenReturn(workflow);
        when(crawlJobRepository.findById(11L)).thenReturn(Optional.of(canonicalJob));
        CrawlExecutionEntity runningExecution = CrawlExecutionEntity.builder()
                .id(777L)
                .crawlJob(canonicalJob)
                .status(CrawlExecutionStatus.RUNNING)
                .itemsFound(5)
                .startedAt(Instant.parse("2026-03-25T12:00:00Z"))
                .createdAt(Instant.parse("2026-03-25T12:00:01Z"))
                .build();
        CrawlExecutionEntity olderExecution = CrawlExecutionEntity.builder()
                .id(700L)
                .crawlJob(canonicalJob)
                .status(CrawlExecutionStatus.SUCCEEDED)
                .itemsFound(4)
                .startedAt(Instant.parse("2026-03-25T11:40:00Z"))
                .finishedAt(Instant.parse("2026-03-25T11:40:30Z"))
                .createdAt(Instant.parse("2026-03-25T11:40:00Z"))
                .build();
        when(crawlExecutionRepository.findByCrawlJob(canonicalJob)).thenReturn(List.of(olderExecution, runningExecution));
        when(jobPostingRepository.findTop5ByCrawlExecutionAndPublishedAtGreaterThanEqualOrderByPublishedAtDesc(
                org.mockito.ArgumentMatchers.eq(runningExecution),
                org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(List.of());
        when(jobPostingRepository.countByCrawlExecutionAndPublishedAtGreaterThanEqual(
                org.mockito.ArgumentMatchers.eq(runningExecution),
                org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(0L);

        RunOnboardingOperationalCheckUseCase useCase = new RunOnboardingOperationalCheckUseCase(
                bootstrapOnboardingProfileWorkflowUseCase,
                crawlJobRepository,
                crawlExecutionRepository,
                jobPostingRepository,
                publicContestPostingRepository,
                clock
        );

        OnboardingOperationalCheckResult result = useCase.execute("greenhouse_bitso", true, 60);

        assertThat(result.executionSummary()).isNotNull();
        assertThat(result.executionSummary().crawlExecutionId()).isEqualTo(777L);
        assertThat(result.executionSummary().status()).isEqualTo("RUNNING");
    }

    @Test
    @DisplayName("should ignore stale historical execution when new smoke run execution has not materialized yet")
    void shouldIgnoreStaleHistoricalExecutionWhenNewSmokeRunExecutionHasNotMaterializedYet() {
        TargetSiteEntity site = targetSite();
        CrawlJobEntity canonicalJob = canonicalJob(site);
        CrawlJobEntity transientJob = transientJob(site);
        BootstrappedOnboardingWorkflowResult workflow = new BootstrappedOnboardingWorkflowResult(
                "greenhouse_bitso",
                new BootstrappedTargetSite("greenhouse_bitso", BootstrapStatus.CREATED, site),
                new BootstrappedCrawlJob(BootstrapStatus.CREATED, canonicalJob),
                true,
                new TargetSiteSmokeRunResult(
                        7L,
                        "greenhouse_bitso",
                        101L,
                        BootstrapStatus.UPDATED,
                        "DISPATCHED",
                        CrawlExecutionStatus.SUCCEEDED
                )
        );

        CrawlExecutionEntity staleExecution = CrawlExecutionEntity.builder()
                .id(499L)
                .crawlJob(transientJob)
                .status(CrawlExecutionStatus.SUCCEEDED)
                .itemsFound(99)
                .startedAt(Instant.parse("2026-03-25T11:30:00Z"))
                .finishedAt(Instant.parse("2026-03-25T11:30:30Z"))
                .createdAt(Instant.parse("2026-03-25T11:30:00Z"))
                .build();

        when(bootstrapOnboardingProfileWorkflowUseCase.execute("greenhouse_bitso", true)).thenReturn(workflow);
        when(crawlJobRepository.findById(101L)).thenReturn(Optional.of(transientJob));
        when(crawlExecutionRepository.findByCrawlJob(transientJob)).thenReturn(List.of(staleExecution));

        RunOnboardingOperationalCheckUseCase useCase = new RunOnboardingOperationalCheckUseCase(
                bootstrapOnboardingProfileWorkflowUseCase,
                crawlJobRepository,
                crawlExecutionRepository,
                jobPostingRepository,
                publicContestPostingRepository,
                clock
        );

        OnboardingOperationalCheckResult result = useCase.execute("greenhouse_bitso", true, 60);

        assertThat(result.executionSummary()).isNull();
        assertThat(result.recentPostingsCount()).isZero();
        assertThat(result.recentPostingsSample()).isEmpty();
    }

    @Test
    @DisplayName("should use public contest postings for public contest operational checks")
    void shouldUsePublicContestPostingsForPublicContestOperationalChecks() {
        TargetSiteEntity site = targetSite(JobCategory.PUBLIC_CONTEST, "municipal_pouso_alegre");
        CrawlJobEntity canonicalJob = canonicalJob(site);
        BootstrappedOnboardingWorkflowResult workflow = new BootstrappedOnboardingWorkflowResult(
                "municipal_pouso_alegre",
                new BootstrappedTargetSite("municipal_pouso_alegre", BootstrapStatus.UPDATED, site),
                new BootstrappedCrawlJob(BootstrapStatus.UPDATED, canonicalJob),
                false,
                null
        );
        CrawlExecutionEntity execution = CrawlExecutionEntity.builder()
                .id(888L)
                .crawlJob(canonicalJob)
                .status(CrawlExecutionStatus.SUCCEEDED)
                .itemsFound(6)
                .startedAt(Instant.parse("2026-03-25T12:00:00Z"))
                .finishedAt(Instant.parse("2026-03-25T12:00:08Z"))
                .createdAt(Instant.parse("2026-03-25T12:00:01Z"))
                .build();

        when(bootstrapOnboardingProfileWorkflowUseCase.execute("municipal_pouso_alegre", false)).thenReturn(workflow);
        when(crawlJobRepository.findById(11L)).thenReturn(Optional.of(canonicalJob));
        when(crawlExecutionRepository.findByCrawlJob(canonicalJob)).thenReturn(List.of(execution));
        when(publicContestPostingRepository.findTop5ByCrawlExecutionAndPublishedAtGreaterThanEqualOrderByPublishedAtDesc(
                org.mockito.ArgumentMatchers.eq(execution),
                org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(List.of(
                publicContestPosting(site, execution, 10L, "Processo Seletivo 005/2026", "Prefeitura de Pouso Alegre")
        ));
        when(publicContestPostingRepository.countByCrawlExecutionAndPublishedAtGreaterThanEqual(
                org.mockito.ArgumentMatchers.eq(execution),
                org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(1L);

        RunOnboardingOperationalCheckUseCase useCase = new RunOnboardingOperationalCheckUseCase(
                bootstrapOnboardingProfileWorkflowUseCase,
                crawlJobRepository,
                crawlExecutionRepository,
                jobPostingRepository,
                publicContestPostingRepository,
                clock
        );

        OnboardingOperationalCheckResult result = useCase.execute("municipal_pouso_alegre", false, 60);

        assertThat(result.executionSummary()).isNotNull();
        assertThat(result.recentPostingsCount()).isEqualTo(1);
        assertThat(result.recentPostingsSample()).extracting(OnboardingRecentPostingSample::title)
                .containsExactly("Processo Seletivo 005/2026");
        assertThat(result.recentPostingsSample()).extracting(OnboardingRecentPostingSample::organization)
                .containsExactly("Prefeitura de Pouso Alegre");
    }

    private static TargetSiteEntity targetSite() {
        return targetSite(JobCategory.PRIVATE_SECTOR, "greenhouse_bitso");
    }

    private static TargetSiteEntity targetSite(JobCategory jobCategory, String siteCode) {
        return TargetSiteEntity.builder()
                .id(7L)
                .siteCode(siteCode)
                .displayName("Target Site")
                .baseUrl("https://example.org")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(jobCategory)
                .legalStatus(LegalStatus.PENDING_REVIEW)
                .selectorBundleVersion("n/a")
                .enabled(false)
                .createdAt(Instant.parse("2026-03-25T11:00:00Z"))
                .updatedAt(Instant.parse("2026-03-25T11:05:00Z"))
                .build();
    }

    private static CrawlJobEntity canonicalJob(TargetSiteEntity site) {
        return CrawlJobEntity.builder()
                .id(11L)
                .targetSite(site)
                .scheduledAt(Instant.parse("2026-03-25T12:01:00Z"))
                .schedulerManaged(true)
                .createdAt(Instant.parse("2026-03-25T11:00:00Z"))
                .build();
    }

    private static CrawlJobEntity transientJob(TargetSiteEntity site) {
        return CrawlJobEntity.builder()
                .id(101L)
                .targetSite(site)
                .scheduledAt(Instant.parse("2026-03-25T12:00:00Z"))
                .schedulerManaged(false)
                .createdAt(Instant.parse("2026-03-25T12:00:00Z"))
                .build();
    }

    private static JobPostingEntity posting(
            TargetSiteEntity site,
            CrawlExecutionEntity crawlExecution,
            Long id,
            String title
    ) {
        return JobPostingEntity.builder()
                .id(id)
                .targetSite(site)
                .crawlExecution(crawlExecution)
                .externalId("external-" + id)
                .canonicalUrl("https://example.org/jobs/" + id)
                .title(title)
                .company("Example Co")
                .location("Remote")
                .remote(true)
                .contractType(JobContractType.CLT)
                .description("desc")
                .techStackTags("Java,Spring")
                .publishedAt(java.time.LocalDate.now().minusDays(90))
                .fingerprintHash("fingerprint-" + id)
                .dedupStatus(DedupStatus.NEW)
                .createdAt(Instant.parse("2026-03-25T12:00:00Z"))
                .build();
    }

    private static PublicContestPostingEntity publicContestPosting(
            TargetSiteEntity site,
            CrawlExecutionEntity crawlExecution,
            Long id,
            String contestName,
            String organizer
    ) {
        return PublicContestPostingEntity.builder()
                .id(id)
                .targetSite(site)
                .crawlExecution(crawlExecution)
                .externalId("contest-" + id)
                .canonicalUrl("https://example.org/contests/" + id)
                .contestName(contestName)
                .organizer(organizer)
                .contestStatus(ContestStatus.OPEN)
                .publishedAt(LocalDate.parse("2026-03-20"))
                .fingerprintHash("contest-fingerprint-" + id)
                .dedupStatus(DedupStatus.NEW)
                .createdAt(Instant.parse("2026-03-25T12:00:00Z"))
                .build();
    }
}
