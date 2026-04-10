package com.campos.webscraper.domain.repository;

import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CrawlJobRepository and CrawlExecutionRepository.
 *
 * TDD RED: written before the repositories and migration V002 exist.
 * Requires Docker to run (Testcontainers with PostgreSQL).
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=none"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("CrawlJobRepository + CrawlExecutionRepository integration")
class CrawlJobRepositoryTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-09T18:06:25Z");
    private static final Instant FIXED_FINISH = Instant.parse("2026-04-09T18:07:10Z");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private CrawlJobRepository crawlJobRepository;

    @Autowired
    private CrawlExecutionRepository crawlExecutionRepository;

    @Autowired
    private TargetSiteRepository targetSiteRepository;

    private TargetSiteEntity savedSite;

    @BeforeEach
    void setUp() {
        crawlExecutionRepository.deleteAll();
        crawlJobRepository.deleteAll();
        targetSiteRepository.deleteAll();

        savedSite = targetSiteRepository.save(
                TargetSiteEntity.builder()
                        .siteCode("indeed-br")
                        .displayName("Indeed Brasil")
                        .baseUrl("https://br.indeed.com")
                        .siteType(SiteType.TYPE_E)
                        .extractionMode(ExtractionMode.API)
                        .jobCategory(JobCategory.PRIVATE_SECTOR)
                        .legalStatus(LegalStatus.APPROVED)
                        .selectorBundleVersion("n/a")
                        .enabled(true)
                        .createdAt(FIXED_NOW)
                        .build()
        );
    }

    @Nested
    @DisplayName("CrawlJob persistence")
    class CrawlJobPersistenceTests {

        @Test
        @DisplayName("should persist and retrieve a CrawlJob linked to a TargetSite")
        void shouldPersistCrawlJob() {
            Instant now = FIXED_NOW;
            CrawlJobEntity job = CrawlJobEntity.builder()
                    .targetSite(savedSite)
                    .scheduledAt(now)
                    .createdAt(now)
                    .build();

            CrawlJobEntity saved = crawlJobRepository.save(job);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getTargetSite().getSiteCode()).isEqualTo("indeed-br");
            assertThat(saved.getScheduledAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("should find CrawlJobs by TargetSite")
        void shouldFindByTargetSite() {
            Instant now = FIXED_NOW;
            crawlJobRepository.save(CrawlJobEntity.builder()
                    .targetSite(savedSite)
                    .scheduledAt(now)
                    .createdAt(now)
                    .build());

            List<CrawlJobEntity> jobs = crawlJobRepository.findByTargetSite(savedSite);

            assertThat(jobs).hasSize(1);
            assertThat(jobs.get(0).getTargetSite().getSiteCode()).isEqualTo("indeed-br");
        }
    }

    @Nested
    @DisplayName("CrawlExecution persistence")
    class CrawlExecutionPersistenceTests {

        @Test
        @DisplayName("should persist a CrawlExecution with PENDING status")
        void shouldPersistCrawlExecution() {
            Instant now = FIXED_NOW;
            CrawlJobEntity job = crawlJobRepository.save(
                    CrawlJobEntity.builder()
                            .targetSite(savedSite)
                            .scheduledAt(now)
                            .createdAt(now)
                            .build()
            );

            CrawlExecutionEntity execution = crawlExecutionRepository.save(
                    CrawlExecutionEntity.builder()
                            .crawlJob(job)
                            .status(CrawlExecutionStatus.PENDING)
                            .retryCount(0)
                            .createdAt(now)
                            .build()
            );

            assertThat(execution.getId()).isNotNull();
            assertThat(execution.getStatus()).isEqualTo(CrawlExecutionStatus.PENDING);
            assertThat(execution.getCrawlJob().getId()).isEqualTo(job.getId());
        }

        @Test
        @DisplayName("should find CrawlExecutions by status")
        void shouldFindByStatus() {
            Instant now = FIXED_NOW;
            CrawlJobEntity job = crawlJobRepository.save(
                    CrawlJobEntity.builder()
                            .targetSite(savedSite)
                            .scheduledAt(now)
                            .createdAt(now)
                            .build()
            );

            crawlExecutionRepository.save(CrawlExecutionEntity.builder()
                    .crawlJob(job).status(CrawlExecutionStatus.PENDING)
                    .retryCount(0).createdAt(now).build());
            crawlExecutionRepository.save(CrawlExecutionEntity.builder()
                    .crawlJob(job).status(CrawlExecutionStatus.RUNNING)
                    .retryCount(0).startedAt(now).createdAt(now).build());

            List<CrawlExecutionEntity> pending =
                    crawlExecutionRepository.findByStatus(CrawlExecutionStatus.PENDING);

            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).getStatus()).isEqualTo(CrawlExecutionStatus.PENDING);
        }

        @Test
        @DisplayName("should persist SUCCEEDED execution with metrics")
        void shouldPersistSucceededWithMetrics() {
            Instant start = FIXED_NOW;
            Instant finish = FIXED_FINISH;

            CrawlJobEntity job = crawlJobRepository.save(
                    CrawlJobEntity.builder()
                            .targetSite(savedSite)
                            .scheduledAt(start)
                            .createdAt(start)
                            .build()
            );

            CrawlExecutionEntity execution = crawlExecutionRepository.save(
                    CrawlExecutionEntity.builder()
                            .crawlJob(job)
                            .status(CrawlExecutionStatus.SUCCEEDED)
                            .startedAt(start)
                            .finishedAt(finish)
                            .pagesVisited(10)
                            .itemsFound(87)
                            .retryCount(0)
                            .createdAt(start)
                            .build()
            );

            Optional<CrawlExecutionEntity> found =
                    crawlExecutionRepository.findById(execution.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getPagesVisited()).isEqualTo(10);
            assertThat(found.get().getItemsFound()).isEqualTo(87);
            assertThat(found.get().getFinishedAt()).isEqualTo(finish);
        }

        @Test
        @DisplayName("should persist FAILED execution with error message")
        void shouldPersistFailedWithErrorMessage() {
            Instant now = FIXED_NOW;
            CrawlJobEntity job = crawlJobRepository.save(
                    CrawlJobEntity.builder()
                            .targetSite(savedSite)
                            .scheduledAt(now)
                            .createdAt(now)
                            .build()
            );

            CrawlExecutionEntity execution = crawlExecutionRepository.save(
                    CrawlExecutionEntity.builder()
                            .crawlJob(job)
                            .status(CrawlExecutionStatus.FAILED)
                            .retryCount(1)
                            .errorMessage("HTTP 503 Service Unavailable")
                            .createdAt(now)
                            .build()
            );

            Optional<CrawlExecutionEntity> found =
                    crawlExecutionRepository.findById(execution.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo(CrawlExecutionStatus.FAILED);
            assertThat(found.get().getRetryCount()).isEqualTo(1);
            assertThat(found.get().getErrorMessage()).isEqualTo("HTTP 503 Service Unavailable");
        }

        @Test
        @DisplayName("should find executions by CrawlJob")
        void shouldFindByCrawlJob() {
            Instant now = FIXED_NOW;
            CrawlJobEntity job = crawlJobRepository.save(
                    CrawlJobEntity.builder()
                            .targetSite(savedSite)
                            .scheduledAt(now)
                            .createdAt(now)
                            .build()
            );

            crawlExecutionRepository.save(CrawlExecutionEntity.builder()
                    .crawlJob(job).status(CrawlExecutionStatus.PENDING)
                    .retryCount(0).createdAt(now).build());
            crawlExecutionRepository.save(CrawlExecutionEntity.builder()
                    .crawlJob(job).status(CrawlExecutionStatus.RUNNING)
                    .retryCount(0).startedAt(now).createdAt(now).build());

            List<CrawlExecutionEntity> executions =
                    crawlExecutionRepository.findByCrawlJob(job);

            assertThat(executions).hasSize(2);
        }
    }
}
