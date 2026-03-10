package com.campos.webscraper.domain.model;

import com.campos.webscraper.domain.enums.JobCategory;
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
 * Represents a scheduled scraping job for a specific TargetSite.
 *
 * One CrawlJob can produce multiple CrawlExecutions (retry policy).
 * Relationship: TargetSite (1) → CrawlJob (N) → CrawlExecution (N)
 */
@Entity
@Table(
        name = "crawl_jobs",
        indexes = {
                @Index(name = "idx_crawl_jobs_target_site",  columnList = "target_site_id"),
                @Index(name = "idx_crawl_jobs_scheduled_at", columnList = "scheduled_at DESC")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The site this job targets. Nullable to allow orphan-safe builders in tests. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_site_id")
    private TargetSiteEntity targetSite;

    /** When this job was scheduled to run. */
    @Column(nullable = false)
    private Instant scheduledAt;

    /**
     * Optional category override — when null, category is inferred from the TargetSite.
     * Stored as VARCHAR(30) to match the JobCategory enum length convention.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private JobCategory jobCategory;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
