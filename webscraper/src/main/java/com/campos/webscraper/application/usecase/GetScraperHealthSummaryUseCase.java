package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.queue.CrawlJobQueueName;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.QueueMessageStatus;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.repository.CrawlExecutionRepository;
import com.campos.webscraper.domain.repository.PersistentQueueMessageRepository;
import com.campos.webscraper.interfaces.dto.RecentCrawlExecutionResponse;
import com.campos.webscraper.interfaces.dto.ScraperExecutionStatusCountResponse;
import com.campos.webscraper.interfaces.dto.ScraperHealthSummaryResponse;
import com.campos.webscraper.interfaces.dto.ScraperQueueStatusCountResponse;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Builds an operational summary of queue and execution state for runtime health checks.
 */
@Component
public class GetScraperHealthSummaryUseCase {

    private final CrawlExecutionRepository crawlExecutionRepository;
    private final PersistentQueueMessageRepository persistentQueueMessageRepository;
    private final Clock clock;

    public GetScraperHealthSummaryUseCase(
            CrawlExecutionRepository crawlExecutionRepository,
            PersistentQueueMessageRepository persistentQueueMessageRepository,
            Clock clock
    ) {
        this.crawlExecutionRepository = Objects.requireNonNull(
                crawlExecutionRepository,
                "crawlExecutionRepository must not be null"
        );
        this.persistentQueueMessageRepository = Objects.requireNonNull(
                persistentQueueMessageRepository,
                "persistentQueueMessageRepository must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ScraperHealthSummaryResponse execute() {
        return new ScraperHealthSummaryResponse(
                Instant.now(clock),
                executionCounts(),
                queueCounts(),
                recentExecutions()
        );
    }

    private List<ScraperExecutionStatusCountResponse> executionCounts() {
        return Arrays.stream(CrawlExecutionStatus.values())
                .map(status -> new ScraperExecutionStatusCountResponse(
                        status.name(),
                        crawlExecutionRepository.countByStatus(status)
                ))
                .toList();
    }

    private List<ScraperQueueStatusCountResponse> queueCounts() {
        return Arrays.stream(CrawlJobQueueName.values())
                .flatMap(queueName -> Arrays.stream(QueueMessageStatus.values())
                        .map(status -> new ScraperQueueStatusCountResponse(
                                queueName.name(),
                                status.name(),
                                persistentQueueMessageRepository.countByQueueNameAndStatus(queueName, status)
                        )))
                .toList();
    }

    private List<RecentCrawlExecutionResponse> recentExecutions() {
        return crawlExecutionRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private RecentCrawlExecutionResponse toResponse(CrawlExecutionEntity execution) {
        String siteCode = execution.getCrawlJob() != null
                && execution.getCrawlJob().getTargetSite() != null
                && execution.getCrawlJob().getTargetSite().getSiteCode() != null
                ? execution.getCrawlJob().getTargetSite().getSiteCode()
                : "unknown";
        return new RecentCrawlExecutionResponse(
                execution.getId(),
                execution.getCrawlJob() == null ? null : execution.getCrawlJob().getId(),
                siteCode,
                execution.getStatus().name(),
                execution.getItemsFound(),
                execution.getStartedAt(),
                execution.getFinishedAt(),
                execution.getErrorMessage()
        );
    }
}
