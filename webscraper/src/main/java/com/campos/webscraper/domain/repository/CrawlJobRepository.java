package com.campos.webscraper.domain.repository;

import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.Instant;
import java.util.Optional;

/**
 * Repository for CrawlJobEntity.
 *
 * Derived query methods follow Spring Data JPA naming conventions.
 */
public interface CrawlJobRepository extends JpaRepository<CrawlJobEntity, Long> {

    /** Returns all crawl jobs for a given target site (ordered by scheduled_at DESC by DB index). */
    @EntityGraph(attributePaths = "targetSite")
    List<CrawlJobEntity> findByTargetSite(TargetSiteEntity targetSite);

    /** Returns the canonical scheduler-managed crawl job for a target site, when present. */
    Optional<CrawlJobEntity> findFirstByTargetSiteIdAndSchedulerManagedTrueOrderByCreatedAtAsc(Long targetSiteId);

    /** Returns enabled jobs due for execution ordered by the oldest scheduled time first. */
    List<CrawlJobEntity> findByTargetSiteEnabledTrueAndSchedulerManagedTrueAndScheduledAtLessThanEqualOrderByScheduledAtAsc(Instant scheduledAt);
}
