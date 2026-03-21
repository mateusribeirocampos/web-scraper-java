package com.campos.webscraper.application.orchestrator;

import com.campos.webscraper.application.queue.CrawlJobQueue;
import com.campos.webscraper.application.queue.CrawlJobQueueName;
import com.campos.webscraper.application.queue.EnqueuedCrawlJob;
import com.campos.webscraper.application.queue.InFlightCrawlJobRegistry;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.CrawlJobRepository;
import com.campos.webscraper.domain.repository.TargetSiteRepository;
import com.campos.webscraper.shared.CrawlJobNotFoundException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Consumes queued crawl jobs and hands them off to the execution dispatcher.
 */
@Component
public class CrawlJobWorker {

    static final int MAX_RETRIES = 3;
    static final Duration RETRY_BACKOFF = Duration.ofSeconds(30);

    private final CrawlJobQueue crawlJobQueue;
    private final CrawlJobRepository crawlJobRepository;
    private final TargetSiteRepository targetSiteRepository;
    private final CrawlJobDispatcher crawlJobDispatcher;
    private final InFlightCrawlJobRegistry inFlightCrawlJobRegistry;
    private final CrawlObservabilityService crawlObservabilityService;

    public CrawlJobWorker(
            CrawlJobQueue crawlJobQueue,
            CrawlJobRepository crawlJobRepository,
            TargetSiteRepository targetSiteRepository,
            CrawlJobDispatcher crawlJobDispatcher,
            InFlightCrawlJobRegistry inFlightCrawlJobRegistry,
            CrawlObservabilityService crawlObservabilityService
    ) {
        this.crawlJobQueue = Objects.requireNonNull(crawlJobQueue, "crawlJobQueue must not be null");
        this.crawlJobRepository = Objects.requireNonNull(crawlJobRepository, "crawlJobRepository must not be null");
        this.targetSiteRepository = Objects.requireNonNull(targetSiteRepository, "targetSiteRepository must not be null");
        this.crawlJobDispatcher = Objects.requireNonNull(crawlJobDispatcher, "crawlJobDispatcher must not be null");
        this.inFlightCrawlJobRegistry = Objects.requireNonNull(
                inFlightCrawlJobRegistry,
                "inFlightCrawlJobRegistry must not be null"
        );
        this.crawlObservabilityService = Objects.requireNonNull(
                crawlObservabilityService,
                "crawlObservabilityService must not be null"
        );
    }

    public boolean consumeNext(CrawlJobQueueName queueName) {
        Objects.requireNonNull(queueName, "queueName must not be null");

        return crawlJobQueue.consume(queueName)
                .map(message -> {
                    CrawlJobEntity crawlJob = null;
                    try {
                        crawlJob = resolveCrawlJob(message);
                        if (!crawlJob.getTargetSite().isEnabled()) {
                            crawlJobQueue.moveToDeadLetter(message, "Target site disabled after enqueue");
                            crawlObservabilityService.recordWorkerOutcome(queueName, message, "dead_letter_disabled_site");
                            releaseClaim(crawlJob, message.crawlJobId());
                            return false;
                        }
                        CrawlExecutionStatus executionStatus = crawlJobDispatcher.dispatch(crawlJob);
                        if (executionStatus == CrawlExecutionStatus.FAILED) {
                            crawlObservabilityService.recordWorkerOutcome(queueName, message, "retry_scheduled");
                            requeueForRetry(message, crawlJob);
                            return false;
                        }
                        if (executionStatus == CrawlExecutionStatus.DEAD_LETTER) {
                            crawlJobQueue.moveToDeadLetter(retryMessage(message, crawlJob), "Dispatcher moved execution to dead letter");
                            crawlObservabilityService.recordWorkerOutcome(queueName, message, "dead_letter_dispatcher");
                            releaseClaim(crawlJob, message.crawlJobId());
                            return false;
                        }
                        crawlJobQueue.markDone(message);
                        crawlObservabilityService.recordWorkerOutcome(queueName, message, "done");
                        releaseClaim(crawlJob, message.crawlJobId());
                        return true;
                    } catch (CrawlJobNotFoundException | QueuedCrawlJobResolutionException exception) {
                        crawlJobQueue.moveToDeadLetter(message, exception.getMessage());
                        crawlObservabilityService.recordWorkerOutcome(queueName, message, "dead_letter_resolution");
                        releaseClaim(crawlJob, message.crawlJobId());
                        return false;
                    } catch (RuntimeException exception) {
                        crawlObservabilityService.recordWorkerOutcome(queueName, message, "retry_scheduled");
                        requeueForRetry(message, crawlJob);
                        return false;
                    }
                })
                .orElseGet(() -> {
                    crawlObservabilityService.recordWorkerOutcome(queueName, null, "empty");
                    return false;
                });
    }

    @Scheduled(fixedDelayString = "${webscraper.worker.poll-interval-ms:1000}", initialDelayString = "${webscraper.worker.initial-delay-ms:1000}")
    public void drainQueues() {
        consumeNext(CrawlJobQueueName.API_JOBS);
        consumeNext(CrawlJobQueueName.STATIC_SCRAPE_JOBS);
        consumeNext(CrawlJobQueueName.DYNAMIC_BROWSER_JOBS);
    }

    private CrawlJobEntity resolveCrawlJob(EnqueuedCrawlJob message) {
        if (message.crawlJobId() != null) {
            CrawlJobEntity persistedJob = crawlJobRepository.findById(message.crawlJobId())
                    .orElseThrow(() -> new CrawlJobNotFoundException(message.crawlJobId()));
            return rebuildQueuedJob(persistedJob, persistedJob.getTargetSite(), message);
        }

        Long targetSiteId = message.targetSiteId();
        if (targetSiteId == null) {
            throw new QueuedCrawlJobResolutionException("targetSiteId must not be null for transient queued jobs");
        }
        TargetSiteEntity persistedTargetSite = targetSiteRepository.findById(targetSiteId)
                .orElseThrow(() -> new QueuedCrawlJobResolutionException("Target site not found for queued job: " + targetSiteId));
        TargetSiteEntity queuedTargetSite = queuedTargetSite(persistedTargetSite, message);
        JobCategory effectiveJobCategory = effectiveJobCategory(message, queuedTargetSite);

        CrawlJobEntity transientJob = CrawlJobEntity.builder()
                .targetSite(persistedTargetSite)
                .scheduledAt(message.scheduledAt() == null ? message.enqueuedAt() : message.scheduledAt())
                .jobCategory(effectiveJobCategory)
                .schedulerManaged(false)
                .createdAt(message.enqueuedAt())
                .build();

        CrawlJobEntity persistedJob = crawlJobRepository.save(transientJob);
        return rebuildQueuedJob(persistedJob, persistedTargetSite, message);
    }

    private static EnqueuedCrawlJob retryMessage(EnqueuedCrawlJob message, CrawlJobEntity crawlJob) {
        if (crawlJob == null || crawlJob.getId() == null || message.crawlJobId() != null) {
            return message;
        }
        return message.withCrawlJobId(crawlJob.getId());
    }

    private void requeueForRetry(EnqueuedCrawlJob message, CrawlJobEntity crawlJob) {
        EnqueuedCrawlJob retryMessage = retryMessage(message, crawlJob);
        if (retryMessage.retryCount() >= MAX_RETRIES) {
            crawlJobQueue.moveToDeadLetter(retryMessage, "Retry limit exhausted");
            releaseClaim(crawlJob, retryMessage.crawlJobId());
            return;
        }
        crawlJobQueue.scheduleRetry(
                retryMessage,
                Instant.now().plus(RETRY_BACKOFF),
                "Transient execution failure"
        );
    }

    private static CrawlJobEntity rebuildQueuedJob(
            CrawlJobEntity persistedJob,
            TargetSiteEntity persistedTargetSite,
            EnqueuedCrawlJob message
    ) {
        TargetSiteEntity queuedTargetSite = queuedTargetSite(persistedTargetSite, message);
        return CrawlJobEntity.builder()
                .id(persistedJob.getId())
                .targetSite(queuedTargetSite)
                .scheduledAt(persistedJob.getScheduledAt())
                .jobCategory(persistedJob.getJobCategory())
                .createdAt(persistedJob.getCreatedAt())
                .build();
    }

    private static TargetSiteEntity queuedTargetSite(TargetSiteEntity persistedTargetSite, EnqueuedCrawlJob message) {
        return TargetSiteEntity.builder()
                .id(persistedTargetSite.getId())
                .siteCode(message.targetSiteCode() == null ? persistedTargetSite.getSiteCode() : message.targetSiteCode())
                .displayName(persistedTargetSite.getDisplayName())
                .baseUrl(message.targetUrl() == null ? persistedTargetSite.getBaseUrl() : message.targetUrl())
                .siteType(persistedTargetSite.getSiteType())
                .extractionMode(message.extractionMode() == null ? persistedTargetSite.getExtractionMode() : message.extractionMode())
                .jobCategory(effectiveJobCategory(message, persistedTargetSite))
                .legalStatus(persistedTargetSite.getLegalStatus())
                .selectorBundleVersion(persistedTargetSite.getSelectorBundleVersion())
                .enabled(persistedTargetSite.isEnabled())
                .createdAt(persistedTargetSite.getCreatedAt())
                .updatedAt(persistedTargetSite.getUpdatedAt())
                .build();
    }

    private static JobCategory effectiveJobCategory(EnqueuedCrawlJob message, TargetSiteEntity targetSite) {
        if (message.jobCategory() != null) {
            return message.jobCategory();
        }
        return targetSite.getJobCategory();
    }

    private void releaseClaim(CrawlJobEntity crawlJob, Long crawlJobId) {
        Long effectiveJobId = crawlJob != null && crawlJob.getId() != null ? crawlJob.getId() : crawlJobId;
        inFlightCrawlJobRegistry.release(effectiveJobId);
    }
}
