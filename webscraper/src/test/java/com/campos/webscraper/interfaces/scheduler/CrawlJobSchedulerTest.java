package com.campos.webscraper.interfaces.scheduler;

import com.campos.webscraper.application.orchestrator.CrawlJobDispatcher;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the scheduled trigger that dispatches due crawl jobs.
 *
 * TDD RED: written before the scheduler exists.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("CrawlJobScheduler")
class CrawlJobSchedulerTest {

    @Mock
    private CrawlJobRepository crawlJobRepository;

    @Mock
    private CrawlJobDispatcher crawlJobDispatcher;

    @Test
    @DisplayName("should dispatch enabled jobs scheduled up to now ordered by scheduledAt")
    void shouldDispatchEnabledJobsScheduledUpToNowOrderedByScheduledAt() {
        Instant now = Instant.parse("2026-03-12T18:00:00Z");
        Clock fixedClock = Clock.fixed(now, ZoneOffset.UTC);
        CrawlJobEntity firstJob = buildJob(1L, now.minusSeconds(300));
        CrawlJobEntity secondJob = buildJob(2L, now.minusSeconds(60));

        when(crawlJobRepository.findByTargetSiteEnabledTrueAndScheduledAtLessThanEqualOrderByScheduledAtAsc(now))
                .thenReturn(List.of(firstJob, secondJob));

        CrawlJobScheduler scheduler = new CrawlJobScheduler(crawlJobRepository, crawlJobDispatcher, fixedClock);

        scheduler.triggerDueJobs();

        verify(crawlJobRepository).findByTargetSiteEnabledTrueAndScheduledAtLessThanEqualOrderByScheduledAtAsc(now);
        verify(crawlJobDispatcher).dispatch(firstJob);
        verify(crawlJobDispatcher).dispatch(secondJob);
    }

    @Test
    @DisplayName("should not dispatch anything when there are no due enabled jobs")
    void shouldNotDispatchAnythingWhenThereAreNoDueEnabledJobs() {
        Instant now = Instant.parse("2026-03-12T18:00:00Z");
        Clock fixedClock = Clock.fixed(now, ZoneOffset.UTC);

        when(crawlJobRepository.findByTargetSiteEnabledTrueAndScheduledAtLessThanEqualOrderByScheduledAtAsc(now))
                .thenReturn(List.of());

        CrawlJobScheduler scheduler = new CrawlJobScheduler(crawlJobRepository, crawlJobDispatcher, fixedClock);

        scheduler.triggerDueJobs();

        verify(crawlJobRepository).findByTargetSiteEnabledTrueAndScheduledAtLessThanEqualOrderByScheduledAtAsc(now);
        verifyNoInteractions(crawlJobDispatcher);
    }

    private static CrawlJobEntity buildJob(Long id, Instant scheduledAt) {
        return CrawlJobEntity.builder()
                .id(id)
                .targetSite(TargetSiteEntity.builder()
                        .siteCode("indeed-br")
                        .displayName("Indeed Brasil")
                        .baseUrl("https://br.indeed.com")
                        .siteType(SiteType.TYPE_E)
                        .extractionMode(ExtractionMode.API)
                        .jobCategory(JobCategory.PRIVATE_SECTOR)
                        .legalStatus(LegalStatus.APPROVED)
                        .selectorBundleVersion("n/a")
                        .enabled(true)
                        .createdAt(scheduledAt.minusSeconds(600))
                        .build())
                .scheduledAt(scheduledAt)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .createdAt(scheduledAt.minusSeconds(60))
                .build();
    }
}
