package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.onboarding.BootstrapStatus;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.CrawlJobRepository;
import com.campos.webscraper.domain.repository.TargetSiteRepository;
import com.campos.webscraper.shared.TargetSiteNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("BootstrapCrawlJobFromTargetSiteUseCase")
class BootstrapCrawlJobFromTargetSiteUseCaseTest {

    @Mock
    private TargetSiteRepository targetSiteRepository;

    @Mock
    private CrawlJobRepository crawlJobRepository;

    @Test
    @DisplayName("should create crawl job for persisted target site when none exists")
    void shouldCreateCrawlJobForPersistedTargetSiteWhenNoneExists() {
        TargetSiteEntity site = persistedSite(7L, "greenhouse_bitso", JobCategory.PRIVATE_SECTOR);
        when(targetSiteRepository.findById(7L)).thenReturn(Optional.of(site));
        when(crawlJobRepository.findFirstByTargetSiteIdAndSchedulerManagedTrueOrderByCreatedAtAsc(7L))
                .thenReturn(Optional.empty());
        when(crawlJobRepository.save(any(CrawlJobEntity.class))).thenAnswer(invocation -> {
            CrawlJobEntity candidate = invocation.getArgument(0);
            return CrawlJobEntity.builder()
                    .id(101L)
                    .targetSite(candidate.getTargetSite())
                    .scheduledAt(candidate.getScheduledAt())
                    .jobCategory(candidate.getJobCategory())
                    .schedulerManaged(candidate.isSchedulerManaged())
                    .createdAt(candidate.getCreatedAt())
                    .build();
        });

        BootstrapCrawlJobFromTargetSiteUseCase useCase = new BootstrapCrawlJobFromTargetSiteUseCase(
                targetSiteRepository,
                crawlJobRepository,
                Clock.fixed(Instant.parse("2026-03-24T20:00:00Z"), ZoneOffset.UTC)
        );

        BootstrappedCrawlJob result = useCase.execute(7L);

        assertThat(result.bootstrapStatus()).isEqualTo(BootstrapStatus.CREATED);
        assertThat(result.crawlJob().getId()).isEqualTo(101L);
        assertThat(result.crawlJob().getTargetSite().getId()).isEqualTo(7L);
        assertThat(result.crawlJob().getJobCategory()).isNull();
        assertThat(result.crawlJob().isSchedulerManaged()).isTrue();
        assertThat(result.crawlJob().getScheduledAt()).isEqualTo(Instant.parse("2026-03-24T20:00:00Z"));
        assertThat(result.crawlJob().getCreatedAt()).isEqualTo(Instant.parse("2026-03-24T20:00:00Z"));
    }

    @Test
    @DisplayName("should update existing crawl job while preserving operational scheduling")
    void shouldUpdateExistingCrawlJobWhilePreservingOperationalScheduling() {
        TargetSiteEntity site = persistedSite(7L, "greenhouse_bitso", JobCategory.PRIVATE_SECTOR);
        CrawlJobEntity existing = CrawlJobEntity.builder()
                .id(11L)
                .targetSite(persistedSite(7L, "legacy_site", JobCategory.PUBLIC_CONTEST))
                .scheduledAt(Instant.parse("2026-03-26T12:00:00Z"))
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .schedulerManaged(false)
                .createdAt(Instant.parse("2026-03-20T10:00:00Z"))
                .build();

        when(targetSiteRepository.findById(7L)).thenReturn(Optional.of(site));
        when(crawlJobRepository.findFirstByTargetSiteIdAndSchedulerManagedTrueOrderByCreatedAtAsc(7L))
                .thenReturn(Optional.of(existing));
        when(crawlJobRepository.save(any(CrawlJobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BootstrapCrawlJobFromTargetSiteUseCase useCase = new BootstrapCrawlJobFromTargetSiteUseCase(
                targetSiteRepository,
                crawlJobRepository,
                Clock.fixed(Instant.parse("2026-03-24T20:00:00Z"), ZoneOffset.UTC)
        );

        BootstrappedCrawlJob result = useCase.execute(7L);

        assertThat(result.bootstrapStatus()).isEqualTo(BootstrapStatus.UPDATED);
        assertThat(result.crawlJob().getId()).isEqualTo(11L);
        assertThat(result.crawlJob().getTargetSite().getSiteCode()).isEqualTo("greenhouse_bitso");
        assertThat(result.crawlJob().getScheduledAt()).isEqualTo(Instant.parse("2026-03-26T12:00:00Z"));
        assertThat(result.crawlJob().isSchedulerManaged()).isFalse();
        assertThat(result.crawlJob().getJobCategory()).isNull();
        assertThat(result.crawlJob().getCreatedAt()).isEqualTo(Instant.parse("2026-03-20T10:00:00Z"));
    }

    @Test
    @DisplayName("should treat duplicate create race as updated crawl job by reloading target site id")
    void shouldTreatDuplicateCreateRaceAsUpdatedCrawlJobByReloadingTargetSiteId() {
        TargetSiteEntity site = persistedSite(7L, "greenhouse_bitso", JobCategory.PRIVATE_SECTOR);
        CrawlJobEntity persisted = CrawlJobEntity.builder()
                .id(17L)
                .targetSite(site)
                .scheduledAt(Instant.parse("2026-03-24T20:00:00Z"))
                .jobCategory(null)
                .schedulerManaged(true)
                .createdAt(Instant.parse("2026-03-24T20:00:00Z"))
                .build();

        when(targetSiteRepository.findById(7L)).thenReturn(Optional.of(site));
        when(crawlJobRepository.findFirstByTargetSiteIdAndSchedulerManagedTrueOrderByCreatedAtAsc(7L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(persisted));
        when(crawlJobRepository.save(any(CrawlJobEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        BootstrapCrawlJobFromTargetSiteUseCase useCase = new BootstrapCrawlJobFromTargetSiteUseCase(
                targetSiteRepository,
                crawlJobRepository,
                Clock.fixed(Instant.parse("2026-03-24T20:00:00Z"), ZoneOffset.UTC)
        );

        BootstrappedCrawlJob result = useCase.execute(7L);

        assertThat(result.bootstrapStatus()).isEqualTo(BootstrapStatus.UPDATED);
        assertThat(result.crawlJob().getId()).isEqualTo(17L);
    }

    @Test
    @DisplayName("should fail when target site does not exist")
    void shouldFailWhenTargetSiteDoesNotExist() {
        when(targetSiteRepository.findById(999L)).thenReturn(Optional.empty());

        BootstrapCrawlJobFromTargetSiteUseCase useCase = new BootstrapCrawlJobFromTargetSiteUseCase(
                targetSiteRepository,
                crawlJobRepository,
                Clock.fixed(Instant.parse("2026-03-24T20:00:00Z"), ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> useCase.execute(999L))
                .isInstanceOf(TargetSiteNotFoundException.class)
                .hasMessage("Target site not found: 999");
    }

    @Test
    @DisplayName("should create scheduler-managed crawl job even when only transient jobs exist for the site")
    void shouldCreateSchedulerManagedCrawlJobEvenWhenOnlyTransientJobsExistForTheSite() {
        TargetSiteEntity site = persistedSite(7L, "greenhouse_bitso", JobCategory.PRIVATE_SECTOR);
        when(targetSiteRepository.findById(7L)).thenReturn(Optional.of(site));
        when(crawlJobRepository.findFirstByTargetSiteIdAndSchedulerManagedTrueOrderByCreatedAtAsc(7L))
                .thenReturn(Optional.empty());
        when(crawlJobRepository.save(any(CrawlJobEntity.class))).thenAnswer(invocation -> {
            CrawlJobEntity candidate = invocation.getArgument(0);
            return CrawlJobEntity.builder()
                    .id(111L)
                    .targetSite(candidate.getTargetSite())
                    .scheduledAt(candidate.getScheduledAt())
                    .jobCategory(candidate.getJobCategory())
                    .schedulerManaged(candidate.isSchedulerManaged())
                    .createdAt(candidate.getCreatedAt())
                    .build();
        });

        BootstrapCrawlJobFromTargetSiteUseCase useCase = new BootstrapCrawlJobFromTargetSiteUseCase(
                targetSiteRepository,
                crawlJobRepository,
                Clock.fixed(Instant.parse("2026-03-24T20:00:00Z"), ZoneOffset.UTC)
        );

        BootstrappedCrawlJob result = useCase.execute(7L);

        assertThat(result.bootstrapStatus()).isEqualTo(BootstrapStatus.CREATED);
        assertThat(result.crawlJob().isSchedulerManaged()).isTrue();
        assertThat(result.crawlJob().getId()).isEqualTo(111L);
    }

    private static TargetSiteEntity persistedSite(Long id, String siteCode, JobCategory category) {
        return TargetSiteEntity.builder()
                .id(id)
                .siteCode(siteCode)
                .displayName("Bitso Careers via Greenhouse")
                .baseUrl("https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(category)
                .legalStatus(LegalStatus.PENDING_REVIEW)
                .selectorBundleVersion("n/a")
                .enabled(false)
                .createdAt(Instant.parse("2026-03-24T18:00:00Z"))
                .updatedAt(Instant.parse("2026-03-24T18:30:00Z"))
                .build();
    }
}
