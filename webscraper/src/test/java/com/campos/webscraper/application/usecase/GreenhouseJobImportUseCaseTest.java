package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.normalizer.GreenhouseJobNormalizer;
import com.campos.webscraper.application.strategy.GreenhouseJobScraperStrategy;
import com.campos.webscraper.shared.JobPostingFingerprintCalculator;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.JobContractType;
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
import com.campos.webscraper.infrastructure.http.GreenhouseJobBoardClient;
import com.campos.webscraper.interfaces.dto.GreenhouseJobBoardItemResponse;
import com.campos.webscraper.interfaces.dto.GreenhouseLocationResponse;
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
 * Integration test for the complete Greenhouse import slice.
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
@DisplayName("GreenhouseJobImportUseCase integration")
class GreenhouseJobImportUseCaseTest {

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

        Instant now = Instant.parse("2026-03-13T16:00:00Z");

        savedSite = targetSiteRepository.save(TargetSiteEntity.builder()
                .siteCode("greenhouse_bitso")
                .displayName("Bitso Careers via Greenhouse")
                .baseUrl("https://boards.greenhouse.io/bitso")
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
                .itemsFound(2)
                .retryCount(0)
                .createdAt(now)
                .build());
    }

    @Test
    @DisplayName("should execute command through strategy normalize and persist Greenhouse jobs")
    void shouldExecuteCommandThroughStrategyNormalizeAndPersistGreenhouseJobs() {
        GreenhouseJobScraperStrategy strategy = new GreenhouseJobScraperStrategy(
                new FakeGreenhouseJobBoardClient(),
                new GreenhouseJobNormalizer()
        );
        GreenhouseJobImportUseCase useCase = new GreenhouseJobImportUseCase(
                jobPostingRepository,
                strategy,
                new JobPostingFingerprintCalculator(),
                new IdempotentJobPostingPersistenceService(jobPostingRepository)
        );

        ScrapeCommand command = new ScrapeCommand(
                "greenhouse_bitso",
                "https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR
        );

        List<JobPostingEntity> persisted = useCase.execute(savedSite, savedExecution, command);

        assertThat(persisted).hasSize(2);
        assertThat(persisted.get(0).getTargetSite().getId()).isEqualTo(savedSite.getId());
        assertThat(persisted.get(0).getCrawlExecution().getId()).isEqualTo(savedExecution.getId());
        assertThat(persisted.get(0).getExternalId()).isEqualTo("6120911003");
        assertThat(persisted.get(0).getPublishedAt()).isEqualTo(LocalDate.of(2024, 9, 13));
        assertThat(persisted.get(0).getFingerprintHash()).isNotBlank();
        assertThat(persisted.get(0).getCompany()).isEqualTo("Bitso");
        assertThat(persisted.get(0).getContractType()).isEqualTo(JobContractType.UNKNOWN);
        assertThat(persisted.get(1).getExternalId()).isEqualTo("7655700003");

        assertThat(jobPostingRepository.findAll()).hasSize(2);
    }

    private static final class FakeGreenhouseJobBoardClient extends GreenhouseJobBoardClient {

        @Override
        public List<GreenhouseJobBoardItemResponse> fetchPublishedJobs(String url) {
            return List.of(
                    new GreenhouseJobBoardItemResponse(
                            6120911003L,
                            "Senior Java Engineer",
                            "https://bitso.com/jobs/6120911003?gh_jid=6120911003",
                            "Bitso",
                            new GreenhouseLocationResponse("Latin America", "Remote"),
                            "2024-09-13T11:35:49-04:00",
                            "<p>Join Bitso to build reliable Java services for crypto and payments.</p>"
                    ),
                    new GreenhouseJobBoardItemResponse(
                            7655700003L,
                            "Senior Security Operations (SecOps) Engineer",
                            "https://bitso.com/jobs/7655700003?gh_jid=7655700003",
                            "Bitso",
                            new GreenhouseLocationResponse("Latin America", null),
                            "2026-03-06T11:09:26-05:00",
                            "<p>Security engineering role for cloud and incident response operations.</p>"
                    )
            );
        }
    }
}
