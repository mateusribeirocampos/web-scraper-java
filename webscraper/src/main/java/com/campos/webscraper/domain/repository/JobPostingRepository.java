package com.campos.webscraper.domain.repository;

import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.enums.SeniorityLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for JobPostingEntity.
 */
public interface JobPostingRepository extends JpaRepository<JobPostingEntity, Long> {

    /**
     * Returns all postings collected from a specific target site.
     */
    List<JobPostingEntity> findByTargetSite(TargetSiteEntity targetSite);

    /**
     * Returns postings published on or after the provided date.
     */
    List<JobPostingEntity> findByPublishedAtGreaterThanEqual(LocalDate publishedAt);

    /**
     * Returns postings published on or after the provided date, newest first.
     */
    List<JobPostingEntity> findByPublishedAtGreaterThanEqualOrderByPublishedAtDesc(LocalDate publishedAt);

    List<JobPostingEntity> findByTargetSiteAndPublishedAtGreaterThanEqualOrderByPublishedAtDesc(
            TargetSiteEntity targetSite,
            LocalDate publishedAt
    );

    List<JobPostingEntity> findTop5ByTargetSiteAndPublishedAtGreaterThanEqualOrderByPublishedAtDesc(
            TargetSiteEntity targetSite,
            LocalDate publishedAt
    );

    long countByTargetSiteAndPublishedAtGreaterThanEqual(TargetSiteEntity targetSite, LocalDate publishedAt);

    List<JobPostingEntity> findTop5ByCrawlExecutionOrderByCreatedAtDesc(CrawlExecutionEntity crawlExecution);

    long countByCrawlExecution(CrawlExecutionEntity crawlExecution);

    List<JobPostingEntity> findTop5ByCrawlExecutionAndPublishedAtGreaterThanEqualOrderByPublishedAtDesc(
            CrawlExecutionEntity crawlExecution,
            LocalDate publishedAt
    );

    long countByCrawlExecutionAndPublishedAtGreaterThanEqual(CrawlExecutionEntity crawlExecution, LocalDate publishedAt);

    List<JobPostingEntity> findTop5ByCrawlExecutionAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            CrawlExecutionEntity crawlExecution,
            Instant createdAt
    );

    long countByCrawlExecutionAndCreatedAtGreaterThanEqual(CrawlExecutionEntity crawlExecution, Instant createdAt);

    /**
     * Returns postings filtered by publication date and seniority, newest first.
     */
    List<JobPostingEntity> findByPublishedAtGreaterThanEqualAndSeniorityOrderByPublishedAtDesc(
            LocalDate publishedAt,
            SeniorityLevel seniority
    );

    /**
     * Returns a posting by its deduplication fingerprint hash.
     */
    Optional<JobPostingEntity> findByFingerprintHash(String fingerprintHash);
}
