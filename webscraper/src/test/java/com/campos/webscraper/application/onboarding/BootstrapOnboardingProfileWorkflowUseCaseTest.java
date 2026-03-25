package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.application.usecase.BootstrapCrawlJobFromTargetSiteUseCase;
import com.campos.webscraper.application.usecase.BootstrappedCrawlJob;
import com.campos.webscraper.application.usecase.RunTargetSiteSmokeRunUseCase;
import com.campos.webscraper.application.usecase.TargetSiteSmokeRunResult;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.CrawlJobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("BootstrapOnboardingProfileWorkflowUseCase")
class BootstrapOnboardingProfileWorkflowUseCaseTest {

    @Mock
    private BootstrapTargetSiteFromProfileUseCase bootstrapTargetSiteFromProfileUseCase;

    @Mock
    private BootstrapCrawlJobFromTargetSiteUseCase bootstrapCrawlJobFromTargetSiteUseCase;

    @Mock
    private RunTargetSiteSmokeRunUseCase runTargetSiteSmokeRunUseCase;

    @Mock
    private CrawlJobRepository crawlJobRepository;

    @Test
    @DisplayName("should bootstrap target site and crawl job without smoke run")
    void shouldBootstrapTargetSiteAndCrawlJobWithoutSmokeRun() {
        when(bootstrapTargetSiteFromProfileUseCase.execute("greenhouse_bitso"))
                .thenReturn(new BootstrappedTargetSite(
                        "greenhouse_bitso",
                        BootstrapStatus.CREATED,
                        persistedSite()
                ));
        when(bootstrapCrawlJobFromTargetSiteUseCase.execute(7L))
                .thenReturn(new BootstrappedCrawlJob(BootstrapStatus.CREATED, persistedCrawlJob()));

        BootstrapOnboardingProfileWorkflowUseCase useCase = new BootstrapOnboardingProfileWorkflowUseCase(
                bootstrapTargetSiteFromProfileUseCase,
                bootstrapCrawlJobFromTargetSiteUseCase,
                runTargetSiteSmokeRunUseCase,
                crawlJobRepository
        );

        BootstrappedOnboardingWorkflowResult result = useCase.execute("greenhouse_bitso", false);

        assertThat(result.profileKey()).isEqualTo("greenhouse_bitso");
        assertThat(result.targetSite().bootstrapStatus()).isEqualTo(BootstrapStatus.CREATED);
        assertThat(result.crawlJob().bootstrapStatus()).isEqualTo(BootstrapStatus.CREATED);
        assertThat(result.smokeRunRequested()).isFalse();
        assertThat(result.smokeRun()).isNull();
        verify(bootstrapTargetSiteFromProfileUseCase).execute("greenhouse_bitso");
        verify(bootstrapCrawlJobFromTargetSiteUseCase).execute(7L);
        verify(runTargetSiteSmokeRunUseCase, never()).execute(7L);
    }

    @Test
    @DisplayName("should bootstrap target site crawl job and smoke run when requested")
    void shouldBootstrapTargetSiteCrawlJobAndSmokeRunWhenRequested() {
        when(bootstrapTargetSiteFromProfileUseCase.execute("greenhouse_bitso"))
                .thenReturn(new BootstrappedTargetSite(
                        "greenhouse_bitso",
                        BootstrapStatus.UPDATED,
                        persistedSite()
                ));
        when(bootstrapCrawlJobFromTargetSiteUseCase.execute(7L))
                .thenReturn(new BootstrappedCrawlJob(BootstrapStatus.UPDATED, persistedCrawlJob()));
        when(runTargetSiteSmokeRunUseCase.execute(7L))
                .thenReturn(new TargetSiteSmokeRunResult(
                        7L,
                        "greenhouse_bitso",
                        101L,
                        BootstrapStatus.UPDATED,
                        "DISPATCHED",
                        CrawlExecutionStatus.SUCCEEDED
                ));

        BootstrapOnboardingProfileWorkflowUseCase useCase = new BootstrapOnboardingProfileWorkflowUseCase(
                bootstrapTargetSiteFromProfileUseCase,
                bootstrapCrawlJobFromTargetSiteUseCase,
                runTargetSiteSmokeRunUseCase,
                crawlJobRepository
        );

        BootstrappedOnboardingWorkflowResult result = useCase.execute("greenhouse_bitso", true);

        assertThat(result.smokeRunRequested()).isTrue();
        assertThat(result.smokeRun()).isNotNull();
        assertThat(result.smokeRun().smokeRunStatus()).isEqualTo("DISPATCHED");
        verify(runTargetSiteSmokeRunUseCase).execute(7L);
    }

    @Test
    @DisplayName("should return refreshed canonical crawl job after smoke run updates schedule")
    void shouldReturnRefreshedCanonicalCrawlJobAfterSmokeRunUpdatesSchedule() {
        CrawlJobEntity initialCanonical = persistedCrawlJob();
        CrawlJobEntity refreshedCanonical = CrawlJobEntity.builder()
                .id(11L)
                .targetSite(initialCanonical.getTargetSite())
                .scheduledAt(Instant.parse("2026-03-25T10:01:00Z"))
                .schedulerManaged(true)
                .createdAt(initialCanonical.getCreatedAt())
                .build();

        when(bootstrapTargetSiteFromProfileUseCase.execute("greenhouse_bitso"))
                .thenReturn(new BootstrappedTargetSite(
                        "greenhouse_bitso",
                        BootstrapStatus.UPDATED,
                        persistedSite()
                ));
        when(bootstrapCrawlJobFromTargetSiteUseCase.execute(7L))
                .thenReturn(new BootstrappedCrawlJob(BootstrapStatus.UPDATED, initialCanonical));
        when(runTargetSiteSmokeRunUseCase.execute(7L))
                .thenReturn(new TargetSiteSmokeRunResult(
                        7L,
                        "greenhouse_bitso",
                        101L,
                        BootstrapStatus.UPDATED,
                        "DISPATCHED",
                        CrawlExecutionStatus.SUCCEEDED
                ));
        when(crawlJobRepository.findFirstByTargetSiteIdAndSchedulerManagedTrueOrderByCreatedAtAsc(7L))
                .thenReturn(Optional.of(refreshedCanonical));

        BootstrapOnboardingProfileWorkflowUseCase useCase = new BootstrapOnboardingProfileWorkflowUseCase(
                bootstrapTargetSiteFromProfileUseCase,
                bootstrapCrawlJobFromTargetSiteUseCase,
                runTargetSiteSmokeRunUseCase,
                crawlJobRepository
        );

        BootstrappedOnboardingWorkflowResult result = useCase.execute("greenhouse_bitso", true);

        assertThat(result.crawlJob().bootstrapStatus()).isEqualTo(BootstrapStatus.UPDATED);
        assertThat(result.crawlJob().crawlJob().getScheduledAt()).isEqualTo(Instant.parse("2026-03-25T10:01:00Z"));
        verify(crawlJobRepository).findFirstByTargetSiteIdAndSchedulerManagedTrueOrderByCreatedAtAsc(7L);
    }

    private static TargetSiteEntity persistedSite() {
        return TargetSiteEntity.builder()
                .id(7L)
                .siteCode("greenhouse_bitso")
                .displayName("Bitso Careers via Greenhouse")
                .baseUrl("https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.PENDING_REVIEW)
                .selectorBundleVersion("n/a")
                .enabled(false)
                .createdAt(Instant.parse("2026-03-24T18:00:00Z"))
                .updatedAt(Instant.parse("2026-03-24T18:30:00Z"))
                .build();
    }

    private static CrawlJobEntity persistedCrawlJob() {
        return CrawlJobEntity.builder()
                .id(11L)
                .targetSite(TargetSiteEntity.builder()
                        .id(7L)
                        .siteCode("greenhouse_bitso")
                        .jobCategory(JobCategory.PRIVATE_SECTOR)
                        .build())
                .scheduledAt(Instant.parse("2026-03-24T20:00:00Z"))
                .schedulerManaged(true)
                .createdAt(Instant.parse("2026-03-24T20:00:00Z"))
                .build();
    }
}
