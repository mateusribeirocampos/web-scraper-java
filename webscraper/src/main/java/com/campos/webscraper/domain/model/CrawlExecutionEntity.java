package com.campos.webscraper.domain.model;

import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents a single execution attempt of a CrawlJob.
 *
 * Lifecycle: PENDING → RUNNING → SUCCEEDED | FAILED → DEAD_LETTER
 *
 * One CrawlJob may have many CrawlExecutions (retry policy).
 * JobPosting and PublicContestPosting reference a CrawlExecution by FK.
 */
@Entity
@Table(
        name = "crawl_executions",
        indexes = {
                @Index(name = "idx_crawl_executions_crawl_job", columnList = "crawl_job_id"),
                @Index(name = "idx_crawl_executions_status",    columnList = "status"),
                @Index(name = "idx_crawl_executions_started",   columnList = "started_at DESC")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "crawl_job_id", nullable = false)
    private CrawlJobEntity crawlJob;

    /** Current lifecycle status, stored as VARCHAR to avoid silent corruption on reorder. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrawlExecutionStatus status;

    /** Timestamp when a worker picked up this execution (null if still PENDING). */
    @Column
    private Instant startedAt;

    /** Timestamp when execution completed (null while PENDING or RUNNING). */
    @Column
    private Instant finishedAt;

    /** Number of pages visited during this execution. */
    @Column(nullable = false)
    @Builder.Default
    private int pagesVisited = 0;

    /** Number of raw items found across all visited pages. */
    @Column(nullable = false)
    @Builder.Default
    private int itemsFound = 0;

    /** How many times this execution has been retried (0 = first attempt). */
    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0;

    /** Last error message when status is FAILED or DEAD_LETTER. Null on success. */
    @Column(columnDefinition = "text")
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
