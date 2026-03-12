package com.campos.webscraper.domain.repository;

import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.enums.SeniorityLevel;
import org.springframework.data.jpa.repository.JpaRepository;

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
