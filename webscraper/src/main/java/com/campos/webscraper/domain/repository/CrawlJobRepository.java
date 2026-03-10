package com.campos.webscraper.domain.repository;

import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for CrawlJobEntity.
 *
 * Derived query methods follow Spring Data JPA naming conventions.
 */
public interface CrawlJobRepository extends JpaRepository<CrawlJobEntity, Long> {

    /** Returns all crawl jobs for a given target site (ordered by scheduled_at DESC by DB index). */
    List<CrawlJobEntity> findByTargetSite(TargetSiteEntity targetSite);
}
