package com.campos.webscraper.application.queue;

import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("CrawlJobQueue")
class CrawlJobQueueTest {

    @Test
    @DisplayName("should enqueue and consume crawl jobs in fifo order within the same queue")
    void shouldEnqueueAndConsumeCrawlJobsInFifoOrderWithinTheSameQueue() {
        InMemoryCrawlJobQueue queue = new InMemoryCrawlJobQueue(
                Clock.fixed(Instant.parse("2026-03-13T18:00:00Z"), ZoneOffset.UTC)
        );

        CrawlJobEntity first = buildJob(1L, "indeed-br", ExtractionMode.API);
        CrawlJobEntity second = buildJob(2L, "greenhouse_bitso", ExtractionMode.API);

        queue.enqueue(first, CrawlJobQueueName.API_JOBS);
        queue.enqueue(second, CrawlJobQueueName.API_JOBS);

        assertThat(queue.consume(CrawlJobQueueName.API_JOBS)).get()
                .extracting(EnqueuedCrawlJob::crawlJobId)
                .isEqualTo(1L);
        assertThat(queue.consume(CrawlJobQueueName.API_JOBS)).get()
                .extracting(EnqueuedCrawlJob::crawlJobId)
                .isEqualTo(2L);
        assertThat(queue.consume(CrawlJobQueueName.API_JOBS)).isEmpty();
    }

    @Test
    @DisplayName("should route api static and browser jobs to distinct named queues")
    void shouldRouteApiStaticAndBrowserJobsToDistinctNamedQueues() {
        CrawlJobQueueRouter router = new CrawlJobQueueRouter();

        CrawlJobEntity apiJob = buildJob(10L, "indeed-br", ExtractionMode.API);
        CrawlJobEntity staticJob = buildJob(11L, "pci_concursos", ExtractionMode.STATIC_HTML);
        CrawlJobEntity browserJob = buildJob(12L, "dynamic-site", ExtractionMode.BROWSER_AUTOMATION);

        assertThat(router.route(apiJob)).isEqualTo(CrawlJobQueueName.API_JOBS);
        assertThat(router.route(staticJob)).isEqualTo(CrawlJobQueueName.STATIC_SCRAPE_JOBS);
        assertThat(router.route(browserJob)).isEqualTo(CrawlJobQueueName.DYNAMIC_BROWSER_JOBS);
    }

    @Test
    @DisplayName("should route transient crawl job without persisted id")
    void shouldRouteTransientCrawlJobWithoutPersistedId() {
        CrawlJobQueueRouter router = new CrawlJobQueueRouter();
        CrawlJobEntity transientJob = buildJobWithoutId("indeed-br", ExtractionMode.API);

        assertThat(router.route(transientJob)).isEqualTo(CrawlJobQueueName.API_JOBS);
    }

    @Test
    @DisplayName("should enqueue transient crawl job without persisted id")
    void shouldEnqueueTransientCrawlJobWithoutPersistedId() {
        InMemoryCrawlJobQueue queue = new InMemoryCrawlJobQueue(
                Clock.fixed(Instant.parse("2026-03-13T18:00:00Z"), ZoneOffset.UTC)
        );
        CrawlJobEntity transientJob = buildJobWithoutId("indeed-br", ExtractionMode.API);

        EnqueuedCrawlJob message = queue.enqueue(transientJob, CrawlJobQueueName.API_JOBS);

        assertThat(message.crawlJobId()).isNull();
        assertThat(message.targetSiteCode()).isEqualTo("indeed-br");
        assertThat(message.queueName()).isEqualTo(CrawlJobQueueName.API_JOBS);
    }

    @Test
    @DisplayName("should preserve the queue already assigned on an enqueued message")
    void shouldPreserveTheQueueAlreadyAssignedOnAnEnqueuedMessage() {
        CrawlJobQueueRouter router = new CrawlJobQueueRouter();
        EnqueuedCrawlJob deadLetterMessage = new EnqueuedCrawlJob(
                null,
                90L,
                1090L,
                "indeed-br",
                "https://br.indeed.com/jobs?q=java",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR,
                Instant.parse("2026-03-13T18:00:00Z"),
                CrawlJobQueueName.DEAD_LETTER_JOBS,
                Instant.parse("2026-03-13T18:01:00Z"),
                0,
                Instant.parse("2026-03-13T18:01:00Z")
        );

        assertThat(router.route(deadLetterMessage)).isEqualTo(CrawlJobQueueName.DEAD_LETTER_JOBS);
    }

    @Test
    @DisplayName("should requeue an existing envelope into a different queue without losing its payload")
    void shouldRequeueAnExistingEnvelopeIntoADifferentQueueWithoutLosingItsPayload() {
        InMemoryCrawlJobQueue queue = new InMemoryCrawlJobQueue();
        EnqueuedCrawlJob message = new EnqueuedCrawlJob(
                null,
                90L,
                1090L,
                "indeed-br",
                "https://br.indeed.com/jobs?q=java",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR,
                Instant.parse("2026-03-13T18:00:00Z"),
                CrawlJobQueueName.API_JOBS,
                Instant.parse("2026-03-13T18:01:00Z"),
                0,
                Instant.parse("2026-03-13T18:01:00Z")
        );

        queue.enqueue(message, CrawlJobQueueName.DEAD_LETTER_JOBS);

        EnqueuedCrawlJob consumed = queue.consume(CrawlJobQueueName.DEAD_LETTER_JOBS).orElseThrow();
        assertThat(consumed.queueName()).isEqualTo(CrawlJobQueueName.DEAD_LETTER_JOBS);
        assertThat(consumed.targetUrl()).isEqualTo("https://br.indeed.com/jobs?q=java");
        assertThat(consumed.crawlJobId()).isEqualTo(90L);
    }

    @Test
    @DisplayName("should materialize queue payload without carrying the JPA entity graph")
    void shouldMaterializeQueuePayloadWithoutCarryingTheJpaEntityGraph() {
        InMemoryCrawlJobQueue queue = new InMemoryCrawlJobQueue(
                Clock.fixed(Instant.parse("2026-03-13T18:00:00Z"), ZoneOffset.UTC)
        );

        CrawlJobEntity crawlJob = buildJob(33L, "greenhouse_bitso", ExtractionMode.API);

        EnqueuedCrawlJob message = queue.enqueue(crawlJob, CrawlJobQueueName.API_JOBS);

        assertThat(message.crawlJobId()).isEqualTo(33L);
        assertThat(message.targetSiteCode()).isEqualTo("greenhouse_bitso");
        assertThat(message.extractionMode()).isEqualTo(ExtractionMode.API);
        assertThat(message.jobCategory()).isEqualTo(JobCategory.PRIVATE_SECTOR);
        assertThat(message.scheduledAt()).isEqualTo(Instant.parse("2026-03-13T18:00:00Z"));
    }

    @Test
    @DisplayName("should preserve effective job category inferred from target site in queue payload")
    void shouldPreserveEffectiveJobCategoryInferredFromTargetSiteInQueuePayload() {
        InMemoryCrawlJobQueue queue = new InMemoryCrawlJobQueue(
                Clock.fixed(Instant.parse("2026-03-13T18:00:00Z"), ZoneOffset.UTC)
        );

        CrawlJobEntity crawlJob = buildJobWithoutExplicitCategory(44L, "pci_concursos", ExtractionMode.STATIC_HTML);

        EnqueuedCrawlJob message = queue.enqueue(crawlJob, CrawlJobQueueName.STATIC_SCRAPE_JOBS);

        assertThat(message.jobCategory()).isEqualTo(JobCategory.PRIVATE_SECTOR);
    }

    @Test
    @DisplayName("should support concurrent producers and consumers without dropping jobs")
    void shouldSupportConcurrentProducersAndConsumersWithoutDroppingJobs() throws Exception {
        InMemoryCrawlJobQueue queue = new InMemoryCrawlJobQueue();
        int totalJobs = 200;
        ExecutorService executor = Executors.newFixedThreadPool(4);
        ConcurrentLinkedQueue<Long> consumedIds = new ConcurrentLinkedQueue<>();
        AtomicInteger consumedCount = new AtomicInteger();

        try {
            executor.submit(() -> enqueueRange(queue, 1, 100));
            executor.submit(() -> enqueueRange(queue, 101, 200));
            executor.submit(() -> consumeUntilDone(queue, consumedIds, consumedCount, totalJobs));
            executor.submit(() -> consumeUntilDone(queue, consumedIds, consumedCount, totalJobs));

            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(consumedIds).hasSize(totalJobs);
        assertThat(new ArrayList<>(consumedIds)).doesNotHaveDuplicates();
        assertThat(queue.consume(CrawlJobQueueName.API_JOBS)).isEmpty();
    }

    @Test
    @DisplayName("should consume a ready message behind a delayed head without starving the queue")
    void shouldConsumeAReadyMessageBehindADelayedHeadWithoutStarvingTheQueue() {
        InMemoryCrawlJobQueue queue = new InMemoryCrawlJobQueue(
                Clock.fixed(Instant.parse("2026-03-13T18:00:00Z"), ZoneOffset.UTC)
        );
        EnqueuedCrawlJob delayed = new EnqueuedCrawlJob(
                null,
                1L,
                1001L,
                "indeed-br",
                "https://br.indeed.com/jobs?q=java",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR,
                Instant.parse("2026-03-13T18:00:00Z"),
                CrawlJobQueueName.API_JOBS,
                Instant.parse("2026-03-13T18:00:00Z"),
                1,
                Instant.parse("2026-03-13T18:05:00Z")
        );
        EnqueuedCrawlJob ready = new EnqueuedCrawlJob(
                null,
                2L,
                1002L,
                "greenhouse_bitso",
                "https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR,
                Instant.parse("2026-03-13T18:00:00Z"),
                CrawlJobQueueName.API_JOBS,
                Instant.parse("2026-03-13T17:59:50Z"),
                0,
                Instant.parse("2026-03-13T17:59:50Z")
        );

        queue.enqueue(delayed, CrawlJobQueueName.API_JOBS);
        queue.enqueue(ready, CrawlJobQueueName.API_JOBS);

        assertThat(queue.consume(CrawlJobQueueName.API_JOBS)).contains(ready);
        assertThat(queue.consume(CrawlJobQueueName.API_JOBS)).isEmpty();
    }

    private static void enqueueRange(InMemoryCrawlJobQueue queue, int fromInclusive, int toInclusive) {
        for (int i = fromInclusive; i <= toInclusive; i++) {
            queue.enqueue(buildJob((long) i, "indeed-br", ExtractionMode.API), CrawlJobQueueName.API_JOBS);
        }
    }

    private static void consumeUntilDone(
            InMemoryCrawlJobQueue queue,
            ConcurrentLinkedQueue<Long> consumedIds,
            AtomicInteger consumedCount,
            int totalJobs
    ) {
        while (consumedCount.get() < totalJobs) {
            var next = queue.consume(CrawlJobQueueName.API_JOBS);
            if (next.isPresent()) {
                if (consumedCount.incrementAndGet() <= totalJobs) {
                    consumedIds.add(next.get().crawlJobId());
                }
                continue;
            }
            Thread.yield();
        }
    }

    private static CrawlJobEntity buildJob(Long id, String siteCode, ExtractionMode extractionMode) {
        Instant now = Instant.parse("2026-03-13T18:00:00Z");
        return CrawlJobEntity.builder()
                .id(id)
                .targetSite(TargetSiteEntity.builder()
                        .id(id + 1_000)
                        .siteCode(siteCode)
                        .displayName(siteCode)
                        .baseUrl("https://" + siteCode + ".example.com")
                        .siteType(extractionMode == ExtractionMode.API ? SiteType.TYPE_E : SiteType.TYPE_A)
                        .extractionMode(extractionMode)
                        .jobCategory(JobCategory.PRIVATE_SECTOR)
                        .legalStatus(LegalStatus.APPROVED)
                        .selectorBundleVersion("n/a")
                        .enabled(true)
                        .createdAt(now.minusSeconds(600))
                        .build())
                .scheduledAt(now)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .createdAt(now.minusSeconds(60))
                .build();
    }

    private static CrawlJobEntity buildJobWithoutExplicitCategory(Long id, String siteCode, ExtractionMode extractionMode) {
        Instant now = Instant.parse("2026-03-13T18:00:00Z");
        return CrawlJobEntity.builder()
                .id(id)
                .targetSite(TargetSiteEntity.builder()
                        .id(id + 1_000)
                        .siteCode(siteCode)
                        .displayName(siteCode)
                        .baseUrl("https://" + siteCode + ".example.com")
                        .siteType(extractionMode == ExtractionMode.API ? SiteType.TYPE_E : SiteType.TYPE_A)
                        .extractionMode(extractionMode)
                        .jobCategory(JobCategory.PRIVATE_SECTOR)
                        .legalStatus(LegalStatus.APPROVED)
                        .selectorBundleVersion("n/a")
                        .enabled(true)
                        .createdAt(now.minusSeconds(600))
                        .build())
                .scheduledAt(now)
                .createdAt(now.minusSeconds(60))
                .build();
    }

    private static CrawlJobEntity buildJobWithoutId(String siteCode, ExtractionMode extractionMode) {
        Instant now = Instant.parse("2026-03-13T18:00:00Z");
        return CrawlJobEntity.builder()
                .targetSite(TargetSiteEntity.builder()
                        .siteCode(siteCode)
                        .displayName(siteCode)
                        .baseUrl("https://" + siteCode + ".example.com")
                        .siteType(extractionMode == ExtractionMode.API ? SiteType.TYPE_E : SiteType.TYPE_A)
                        .extractionMode(extractionMode)
                        .jobCategory(JobCategory.PRIVATE_SECTOR)
                        .legalStatus(LegalStatus.APPROVED)
                        .selectorBundleVersion("n/a")
                        .enabled(true)
                        .createdAt(now.minusSeconds(600))
                        .build())
                .scheduledAt(now)
                .createdAt(now.minusSeconds(60))
                .build();
    }
}
