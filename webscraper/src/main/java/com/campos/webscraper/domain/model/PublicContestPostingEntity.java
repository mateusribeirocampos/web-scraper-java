package com.campos.webscraper.domain.model;

import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.domain.enums.DedupStatus;
import com.campos.webscraper.domain.enums.EducationLevel;
import com.campos.webscraper.domain.enums.GovernmentLevel;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Public contest posting extracted from official APIs or approved scraping sources.
 */
@Entity
@Table(
        name = "public_contest_postings",
        indexes = {
                @Index(name = "idx_contest_published_at", columnList = "published_at DESC"),
                @Index(name = "idx_contest_registration_end", columnList = "registration_end_date"),
                @Index(name = "idx_contest_site_external", columnList = "target_site_id, external_id"),
                @Index(name = "idx_contest_fingerprint", columnList = "fingerprint_hash"),
                @Index(name = "idx_contest_gov_level_state", columnList = "government_level, state")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicContestPostingEntity {

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

    @Column(name = "contest_name", nullable = false, length = 500)
    private String contestName;

    @Column(nullable = false, length = 300)
    private String organizer;

    @Column(name = "position_title", nullable = false, length = 300)
    private String positionTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "government_level", nullable = false, length = 20)
    private GovernmentLevel governmentLevel;

    @Column(length = 2)
    private String state;

    @Enumerated(EnumType.STRING)
    @Column(name = "education_level", nullable = false, length = 20)
    private EducationLevel educationLevel;

    @Column(name = "number_of_vacancies")
    private Integer numberOfVacancies;

    @Column(name = "base_salary", precision = 10, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "salary_description", length = 500)
    private String salaryDescription;

    @Column(name = "edital_url", length = 1000)
    private String editalUrl;

    @Column(name = "published_at", nullable = false)
    private LocalDate publishedAt;

    @Column(name = "registration_start_date")
    private LocalDate registrationStartDate;

    @Column(name = "registration_end_date")
    private LocalDate registrationEndDate;

    @Column(name = "exam_date")
    private LocalDate examDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "contest_status", nullable = false, length = 30)
    private ContestStatus contestStatus;

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
