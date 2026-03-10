package com.campos.webscraper.domain.repository;

import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for CrawlExecutionEntity.
 *
 * Derived query methods follow Spring Data JPA naming conventions.
 */
public interface CrawlExecutionRepository extends JpaRepository<CrawlExecutionEntity, Long> {

    /** Returns all executions for a given crawl job. */
    List<CrawlExecutionEntity> findByCrawlJob(CrawlJobEntity crawlJob);

    /** Returns all executions with a given lifecycle status. */
    List<CrawlExecutionEntity> findByStatus(CrawlExecutionStatus status);
}
