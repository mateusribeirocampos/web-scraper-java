package com.campos.webscraper.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Raw HTTP snapshot captured during a crawl execution.
 *
 * Stores the verbatim response body and HTTP status so that scraping failures
 * can be diagnosed offline without repeating the fetch. crawlExecutionId is
 * nullable to support orphan snapshots produced by smoke runs or pre-execution probes.
 */
@Entity
@Table(
        name = "raw_snapshots",
        indexes = {
                @Index(name = "idx_raw_snapshots_site_code",       columnList = "site_code"),
                @Index(name = "idx_raw_snapshots_crawl_execution",  columnList = "crawl_execution_id"),
                @Index(name = "idx_raw_snapshots_fetched_at",       columnList = "fetched_at DESC")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK to the crawl_executions row that produced this snapshot. Nullable. */
    @Column(name = "crawl_execution_id")
    private Long crawlExecutionId;

    /** Site code from the owning TargetSite (e.g. "pci-concursos", "dou"). */
    @Column(name = "site_code", nullable = false, length = 100)
    private String siteCode;

    /** Timestamp when the HTTP response was received. */
    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    /** Verbatim response body (HTML, JSON, etc.). */
    @Column(name = "response_body", nullable = false, columnDefinition = "text")
    private String responseBody;

    /** HTTP status code returned by the server. */
    @Column(name = "response_status", nullable = false)
    private int responseStatus;
}
