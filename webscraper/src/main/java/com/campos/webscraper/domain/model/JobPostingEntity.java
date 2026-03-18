package com.campos.webscraper.domain.model;

import com.campos.webscraper.domain.enums.DedupStatus;
import com.campos.webscraper.domain.enums.JobContractType;
import com.campos.webscraper.domain.enums.SeniorityLevel;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Private-sector job posting extracted from a target site or official API.
 *
 * <p>{@code publishedAt} is mandatory because chronology is a core query dimension of the platform.
 * Deduplication is driven by {@code fingerprintHash} and {@code dedupStatus}.
 */
@Entity
@Table(
        name = "job_postings",
        indexes = {
                @Index(name = "idx_job_published_at", columnList = "published_at DESC"),
                @Index(name = "idx_job_site_external", columnList = "target_site_id, external_id"),
                @Index(name = "idx_job_fingerprint", columnList = "fingerprint_hash"),
                @Index(name = "idx_job_seniority_stack", columnList = "seniority, tech_stack_tags")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPostingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "crawl_execution_id", nullable = false)
    private CrawlExecutionEntity crawlExecution;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "target_site_id", nullable = false)
    private TargetSiteEntity targetSite;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(name = "canonical_url", nullable = false, length = 1000)
    private String canonicalUrl;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 300)
    private String company;

    @Column(length = 300)
    private String location;

    @Column(nullable = false)
    private boolean remote;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false, length = 30)
    private JobContractType contractType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SeniorityLevel seniority;

    @Column(name = "salary_range", length = 500)
    private String salaryRange;

    @Column(name = "tech_stack_tags", length = 1000)
    private String techStackTags;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "published_at", nullable = false)
    private LocalDate publishedAt;

    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;

    @Column(name = "fingerprint_hash", nullable = false, length = 128)
    private String fingerprintHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "dedup_status", nullable = false, length = 20)
    private DedupStatus dedupStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", columnDefinition = "jsonb")
    private String payloadJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
