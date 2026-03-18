package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.normalizer.IndeedJobNormalizer;
import com.campos.webscraper.application.strategy.IndeedApiJobScraperStrategy;
import com.campos.webscraper.application.usecase.IdempotentJobPostingPersistenceService;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.CrawlExecutionRepository;
import com.campos.webscraper.domain.repository.CrawlJobRepository;
import com.campos.webscraper.domain.repository.JobPostingRepository;
import com.campos.webscraper.domain.repository.TargetSiteRepository;
import com.campos.webscraper.infrastructure.http.IndeedApiClient;
import com.campos.webscraper.interfaces.dto.IndeedApiResponse;
import com.campos.webscraper.shared.JobPostingFingerprintCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the complete Indeed import slice.
 *
 * TDD RED: written before the use case exists.
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
@DisplayName("IndeedJobImportUseCase integration")
class IndeedJobImportUseCaseTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private TargetSiteRepository targetSiteRepository;

    @Autowired
    private CrawlJobRepository crawlJobRepository;

    @Autowired
    private CrawlExecutionRepository crawlExecutionRepository;

    @Autowired
    private JobPostingRepository jobPostingRepository;

    private TargetSiteEntity savedSite;
    private CrawlExecutionEntity savedExecution;

    @BeforeEach
    void setUp() {
        jobPostingRepository.deleteAll();
        crawlExecutionRepository.deleteAll();
        crawlJobRepository.deleteAll();
        targetSiteRepository.deleteAll();

        Instant now = Instant.parse("2026-03-12T15:00:00Z");

        savedSite = targetSiteRepository.save(TargetSiteEntity.builder()
                .siteCode("indeed-br")
                .displayName("Indeed Brasil")
                .baseUrl("https://br.indeed.com")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("n/a")
                .enabled(true)
                .createdAt(now)
                .build());

        CrawlJobEntity savedJob = crawlJobRepository.save(CrawlJobEntity.builder()
                .targetSite(savedSite)
                .scheduledAt(now)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .createdAt(now)
                .build());

        savedExecution = crawlExecutionRepository.save(CrawlExecutionEntity.builder()
                .crawlJob(savedJob)
                .status(CrawlExecutionStatus.SUCCEEDED)
                .startedAt(now)
                .finishedAt(now.plusSeconds(20))
                .pagesVisited(1)
                .itemsFound(1)
                .retryCount(0)
                .createdAt(now)
                .build());
    }

    @Test
    @DisplayName("should execute command through strategy normalize and persist Indeed jobs")
    void shouldExecuteCommandThroughStrategyNormalizeAndPersistIndeedJobs() {
        IndeedApiJobScraperStrategy strategy = new IndeedApiJobScraperStrategy(
                new FakeIndeedApiClient(),
                new IndeedJobNormalizer()
        );
        JobPostingFingerprintCalculator fingerprintCalculator = new JobPostingFingerprintCalculator();
        IdempotentJobPostingPersistenceService idempotentPersistenceService =
                new IdempotentJobPostingPersistenceService(jobPostingRepository);
        IndeedJobImportUseCase useCase = new IndeedJobImportUseCase(
                jobPostingRepository,
                strategy,
                fingerprintCalculator,
                idempotentPersistenceService
        );

        ScrapeCommand command = new ScrapeCommand(
                "indeed-br",
                "https://to.indeed.test/api/jobs/123",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR
        );

        List<JobPostingEntity> persisted = useCase.execute(savedSite, savedExecution, command);

        assertThat(persisted).hasSize(1);
        assertThat(persisted.get(0).getTargetSite().getId()).isEqualTo(savedSite.getId());
        assertThat(persisted.get(0).getCrawlExecution().getId()).isEqualTo(savedExecution.getId());
        assertThat(persisted.get(0).getExternalId()).isEqualTo("job-123");
        assertThat(persisted.get(0).getPublishedAt()).isEqualTo(LocalDate.of(2026, 3, 5));
        assertThat(persisted.get(0).getFingerprintHash()).isNotBlank();

        assertThat(jobPostingRepository.findAll()).hasSize(1);
    }

    private static final class FakeIndeedApiClient extends IndeedApiClient {

        @Override
        public IndeedApiResponse fetchJob(String url) {
            return new IndeedApiResponse(
                    "job-123",
                    "Java Backend Developer | Jr (Remote)",
                    "Invillia",
                    "Remoto",
                    "2026-03-05",
                    "https://to.indeed.com/job-123"
            );
        }
    }
}
