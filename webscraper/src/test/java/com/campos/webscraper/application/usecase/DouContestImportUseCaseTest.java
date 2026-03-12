package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.normalizer.DouContestNormalizer;
import com.campos.webscraper.application.strategy.DouApiContestScraperStrategy;
import com.campos.webscraper.domain.enums.DedupStatus;
import com.campos.webscraper.domain.enums.EducationLevel;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.CrawlExecutionRepository;
import com.campos.webscraper.domain.repository.CrawlJobRepository;
import com.campos.webscraper.domain.repository.PublicContestPostingRepository;
import com.campos.webscraper.domain.repository.TargetSiteRepository;
import com.campos.webscraper.infrastructure.http.DouApiClient;
import com.campos.webscraper.interfaces.dto.DouApiItemResponse;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the complete DOU import slice.
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
@DisplayName("DouContestImportUseCase integration")
class DouContestImportUseCaseTest {

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
    private PublicContestPostingRepository publicContestPostingRepository;

    private TargetSiteEntity savedSite;
    private CrawlExecutionEntity savedExecution;

    @BeforeEach
    void setUp() {
        publicContestPostingRepository.deleteAll();
        crawlExecutionRepository.deleteAll();
        crawlJobRepository.deleteAll();
        targetSiteRepository.deleteAll();

        Instant now = Instant.parse("2026-03-12T15:20:00Z");

        savedSite = targetSiteRepository.save(TargetSiteEntity.builder()
                .siteCode("dou-api")
                .displayName("DOU API")
                .baseUrl("https://www.in.gov.br")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("n/a")
                .enabled(true)
                .createdAt(now)
                .build());

        CrawlJobEntity savedJob = crawlJobRepository.save(CrawlJobEntity.builder()
                .targetSite(savedSite)
                .scheduledAt(now)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
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
    @DisplayName("should execute command through strategy normalize and persist DOU contests")
    void shouldExecuteCommandThroughStrategyNormalizeAndPersistDouContests() {
        DouApiContestScraperStrategy strategy = new DouApiContestScraperStrategy(
                new FakeDouApiClient(),
                new DouContestNormalizer()
        );
        DouContestImportUseCase useCase = new DouContestImportUseCase(publicContestPostingRepository, strategy);

        ScrapeCommand command = new ScrapeCommand(
                "dou-api",
                "https://www.in.gov.br/api/dou",
                ExtractionMode.API,
                JobCategory.PUBLIC_CONTEST
        );

        List<PublicContestPostingEntity> persisted = useCase.execute(savedSite, savedExecution, command);

        assertThat(persisted).hasSize(2);
        assertThat(persisted.get(0).getTargetSite().getId()).isEqualTo(savedSite.getId());
        assertThat(persisted.get(0).getCrawlExecution().getId()).isEqualTo(savedExecution.getId());
        assertThat(persisted.get(0).getExternalId()).isEqualTo("dou-1");
        assertThat(persisted.get(0).getEducationLevel()).isEqualTo(EducationLevel.SUPERIOR);
        assertThat(persisted.get(0).getDedupStatus()).isEqualTo(DedupStatus.NEW);
        assertThat(persisted.get(0).getFingerprintHash()).isNotBlank();
        assertThat(persisted.get(1).getExternalId()).isEqualTo("dou-2");

        assertThat(publicContestPostingRepository.findAll()).hasSize(2);
    }

    private static final class FakeDouApiClient extends DouApiClient {

        @Override
        public List<DouApiItemResponse> searchRelevantNotices(String url) {
            return List.of(
                    new DouApiItemResponse(
                            "dou-1",
                            "Analista de TI - Desenvolvimento de Sistemas",
                            "Concurso federal para analista de tecnologia da informacao",
                            "2026-03-10",
                            "https://www.in.gov.br/web/dou/-/edital-1"
                    ),
                    new DouApiItemResponse(
                            "dou-2",
                            "Desenvolvedor Backend Java",
                            "Processo seletivo com foco em tecnologia da informacao",
                            "2026-03-11",
                            "https://www.in.gov.br/web/dou/-/edital-2"
                    )
            );
        }
    }
}
