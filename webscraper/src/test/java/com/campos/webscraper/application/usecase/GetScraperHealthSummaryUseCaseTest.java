package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.queue.CrawlJobQueueName;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.QueueMessageStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.CrawlExecutionRepository;
import com.campos.webscraper.domain.repository.PersistentQueueMessageRepository;
import com.campos.webscraper.interfaces.dto.ScraperHealthSummaryResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("GetScraperHealthSummaryUseCase")
class GetScraperHealthSummaryUseCaseTest {

    @Mock
    private CrawlExecutionRepository crawlExecutionRepository;

    @Mock
    private PersistentQueueMessageRepository persistentQueueMessageRepository;

    @Test
    @DisplayName("should aggregate execution queue counts and recent executions")
    void shouldAggregateExecutionQueueCountsAndRecentExecutions() {
        Instant now = Instant.parse("2026-03-21T18:00:00Z");
        Clock fixedClock = Clock.fixed(now, ZoneOffset.UTC);

        when(crawlExecutionRepository.countByStatus(CrawlExecutionStatus.PENDING)).thenReturn(0L);
        when(crawlExecutionRepository.countByStatus(CrawlExecutionStatus.RUNNING)).thenReturn(1L);
        when(crawlExecutionRepository.countByStatus(CrawlExecutionStatus.SUCCEEDED)).thenReturn(12L);
        when(crawlExecutionRepository.countByStatus(CrawlExecutionStatus.FAILED)).thenReturn(2L);
        when(crawlExecutionRepository.countByStatus(CrawlExecutionStatus.DEAD_LETTER)).thenReturn(1L);

        for (CrawlJobQueueName queueName : CrawlJobQueueName.values()) {
            for (QueueMessageStatus status : QueueMessageStatus.values()) {
                when(persistentQueueMessageRepository.countByQueueNameAndStatus(queueName, status)).thenReturn(0L);
            }
        }
        when(persistentQueueMessageRepository.countByQueueNameAndStatus(CrawlJobQueueName.API_JOBS, QueueMessageStatus.READY))
                .thenReturn(3L);
        when(persistentQueueMessageRepository.countByQueueNameAndStatus(CrawlJobQueueName.DYNAMIC_BROWSER_JOBS, QueueMessageStatus.RETRY_WAIT))
                .thenReturn(1L);

        when(crawlExecutionRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(List.of(
                CrawlExecutionEntity.builder()
                        .id(501L)
                        .crawlJob(CrawlJobEntity.builder()
                                .id(42L)
                                .targetSite(TargetSiteEntity.builder()
                                        .siteCode("greenhouse_bitso")
                                        .displayName("greenhouse_bitso")
                                        .baseUrl("https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true")
                                        .siteType(SiteType.TYPE_E)
                                        .extractionMode(ExtractionMode.API)
                                        .jobCategory(JobCategory.PRIVATE_SECTOR)
                                        .legalStatus(LegalStatus.APPROVED)
                                        .selectorBundleVersion("n/a")
                                        .enabled(true)
                                        .createdAt(now.minusSeconds(600))
                                        .build())
                                .scheduledAt(now.minusSeconds(120))
                                .jobCategory(JobCategory.PRIVATE_SECTOR)
                                .createdAt(now.minusSeconds(180))
                                .build())
                        .status(CrawlExecutionStatus.SUCCEEDED)
                        .itemsFound(7)
                        .startedAt(now.minusSeconds(30))
                        .finishedAt(now.minusSeconds(10))
                        .createdAt(now.minusSeconds(30))
                        .build()
        ));

        GetScraperHealthSummaryUseCase useCase = new GetScraperHealthSummaryUseCase(
                crawlExecutionRepository,
                persistentQueueMessageRepository,
                fixedClock
        );

        ScraperHealthSummaryResponse response = useCase.execute();

        assertThat(response.generatedAt()).isEqualTo(now);
        assertThat(response.executionCounts())
                .anySatisfy(item -> {
                    assertThat(item.status()).isEqualTo("SUCCEEDED");
                    assertThat(item.count()).isEqualTo(12L);
                })
                .anySatisfy(item -> {
                    assertThat(item.status()).isEqualTo("RUNNING");
                    assertThat(item.count()).isEqualTo(1L);
                });
        assertThat(response.queueCounts())
                .anySatisfy(item -> {
                    assertThat(item.queueName()).isEqualTo("API_JOBS");
                    assertThat(item.status()).isEqualTo("READY");
                    assertThat(item.count()).isEqualTo(3L);
                })
                .anySatisfy(item -> {
                    assertThat(item.queueName()).isEqualTo("DYNAMIC_BROWSER_JOBS");
                    assertThat(item.status()).isEqualTo("RETRY_WAIT");
                    assertThat(item.count()).isEqualTo(1L);
                });
        assertThat(response.recentExecutions()).singleElement().satisfies(item -> {
            assertThat(item.executionId()).isEqualTo(501L);
            assertThat(item.crawlJobId()).isEqualTo(42L);
            assertThat(item.siteCode()).isEqualTo("greenhouse_bitso");
            assertThat(item.status()).isEqualTo("SUCCEEDED");
            assertThat(item.itemsFound()).isEqualTo(7);
        });
    }
}
