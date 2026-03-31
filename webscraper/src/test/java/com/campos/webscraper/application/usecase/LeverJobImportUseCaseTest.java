package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.normalizer.LeverJobNormalizer;
import com.campos.webscraper.application.strategy.LeverJobScraperStrategy;
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
import com.campos.webscraper.infrastructure.http.LeverPostingsClient;
import com.campos.webscraper.interfaces.dto.LeverCategoriesResponse;
import com.campos.webscraper.interfaces.dto.LeverPostingResponse;
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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=none"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("LeverJobImportUseCase integration")
class LeverJobImportUseCaseTest {

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

        Instant now = Instant.parse("2026-03-31T16:00:00Z");

        savedSite = targetSiteRepository.save(TargetSiteEntity.builder()
                .siteCode("lever_ciandt")
                .displayName("CI&T Careers via Lever")
                .baseUrl("https://api.lever.co/v0/postings/ciandt?mode=json")
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
    @DisplayName("should execute command through strategy normalize and persist Lever jobs")
    void shouldExecuteCommandThroughStrategyNormalizeAndPersistLeverJobs() {
        LeverJobScraperStrategy strategy = new LeverJobScraperStrategy(
                new FakeLeverPostingsClient(),
                new LeverJobNormalizer(
                        new com.fasterxml.jackson.databind.ObjectMapper(),
                        Clock.fixed(Instant.parse("2026-03-31T12:00:00Z"), ZoneOffset.UTC)
                )
        );
        LeverJobImportUseCase useCase = new LeverJobImportUseCase(
                jobPostingRepository,
                strategy,
                new JobPostingFingerprintCalculator(),
                new IdempotentJobPostingPersistenceService(jobPostingRepository)
        );

        ScrapeCommand command = new ScrapeCommand(
                "lever_ciandt",
                "https://api.lever.co/v0/postings/ciandt?mode=json",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR
        );

        List<JobPostingEntity> persisted = useCase.execute(savedSite, savedExecution, command);

        assertThat(persisted).hasSize(2);
        assertThat(persisted.get(0).getTargetSite().getId()).isEqualTo(savedSite.getId());
        assertThat(persisted.get(0).getCrawlExecution().getId()).isEqualTo(savedExecution.getId());
        assertThat(persisted.get(0).getExternalId()).isEqualTo("job-123");
        assertThat(persisted.get(0).getCompany()).isEqualTo("CI&T");
        assertThat(persisted.get(0).getContractType()).isEqualTo(JobContractType.UNKNOWN);
        assertThat(persisted.get(0).getPublishedAt()).isEqualTo(LocalDate.of(2026, 3, 31));
        assertThat(persisted.get(0).getFingerprintHash()).isNotBlank();
        assertThat(persisted.get(1).getExternalId()).isEqualTo("job-124");

        assertThat(jobPostingRepository.findAll()).hasSize(2);
    }

    private static final class FakeLeverPostingsClient extends LeverPostingsClient {
        @Override
        public List<LeverPostingResponse> fetchPublishedJobs(String url) {
            return List.of(
                    new LeverPostingResponse(
                            "job-123",
                            "Senior Java Engineer",
                            "https://jobs.lever.co/ciandt/job-123",
                            "https://jobs.lever.co/ciandt/job-123/apply",
                            "onsite",
                            new LeverCategoriesResponse("Engineering", "Campinas, Brazil", "Full-time"),
                            "<p>Build Java services on AWS.</p>"
                    ),
                    new LeverPostingResponse(
                            "job-124",
                            "Frontend Engineer Remote",
                            "https://jobs.lever.co/ciandt/job-124",
                            "https://jobs.lever.co/ciandt/job-124/apply",
                            "remote",
                            new LeverCategoriesResponse("Engineering", "Remote - Brazil", "Full-time"),
                            "<p>Build frontends with React and TypeScript.</p>"
                    )
            );
        }
    }
}
