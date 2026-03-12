package com.campos.webscraper.domain.repository;

import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.DedupStatus;
import com.campos.webscraper.domain.enums.EducationLevel;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.GovernmentLevel;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PublicContestPostingRepository.
 *
 * TDD RED: written before repository, entity and migration V005 exist.
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
@DisplayName("PublicContestPostingRepository integration")
class PublicContestPostingRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private PublicContestPostingRepository publicContestPostingRepository;

    @Autowired
    private CrawlExecutionRepository crawlExecutionRepository;

    @Autowired
    private CrawlJobRepository crawlJobRepository;

    @Autowired
    private TargetSiteRepository targetSiteRepository;

    private TargetSiteEntity savedSite;
    private CrawlExecutionEntity savedExecution;

    @BeforeEach
    void setUp() {
        publicContestPostingRepository.deleteAll();
        crawlExecutionRepository.deleteAll();
        crawlJobRepository.deleteAll();
        targetSiteRepository.deleteAll();

        Instant now = Instant.parse("2026-03-12T14:05:30Z");

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
                .finishedAt(now.plusSeconds(60))
                .pagesVisited(2)
                .itemsFound(3)
                .retryCount(0)
                .createdAt(now)
                .build());
    }

    @Test
    @DisplayName("should save and find PublicContestPostingEntity by id")
    void shouldSaveAndFindById() {
        PublicContestPostingEntity saved = publicContestPostingRepository.save(buildPosting(
                "dou-1",
                "https://www.in.gov.br/web/dou/-/edital-1",
                LocalDate.of(2026, 3, 5),
                LocalDate.of(2026, 3, 20),
                "sha256:contest-1",
                ContestStatus.OPEN
        ));

        Optional<PublicContestPostingEntity> found = publicContestPostingRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getExternalId()).isEqualTo("dou-1");
        assertThat(found.get().getCanonicalUrl()).isEqualTo("https://www.in.gov.br/web/dou/-/edital-1");
    }

    @Test
    @DisplayName("should find PublicContestPosting by fingerprintHash")
    void shouldFindByFingerprintHash() {
        publicContestPostingRepository.save(buildPosting(
                "dou-2",
                "https://www.in.gov.br/web/dou/-/edital-2",
                LocalDate.of(2026, 3, 6),
                LocalDate.of(2026, 3, 21),
                "sha256:contest-2",
                ContestStatus.OPEN
        ));

        Optional<PublicContestPostingEntity> found =
                publicContestPostingRepository.findByFingerprintHash("sha256:contest-2");

        assertThat(found).isPresent();
        assertThat(found.get().getExternalId()).isEqualTo("dou-2");
    }

    @Test
    @DisplayName("should find contests by registrationEndDate greater than or equal and order ascending")
    void shouldFindByContestStatusAndRegistrationEndDateGreaterThanEqualOrderByRegistrationEndDateAsc() {
        publicContestPostingRepository.save(buildPosting(
                "dou-3",
                "https://www.in.gov.br/web/dou/-/edital-3",
                LocalDate.of(2026, 3, 4),
                LocalDate.of(2026, 3, 28),
                "sha256:contest-3",
                ContestStatus.OPEN
        ));
        publicContestPostingRepository.save(buildPosting(
                "dou-4",
                "https://www.in.gov.br/web/dou/-/edital-4",
                LocalDate.of(2026, 3, 4),
                LocalDate.of(2026, 3, 18),
                "sha256:contest-4",
                ContestStatus.OPEN
        ));
        publicContestPostingRepository.save(buildPosting(
                "dou-5",
                "https://www.in.gov.br/web/dou/-/edital-5",
                LocalDate.of(2026, 3, 4),
                LocalDate.of(2026, 3, 15),
                "sha256:contest-5",
                ContestStatus.REGISTRATION_CLOSED
        ));

        List<PublicContestPostingEntity> contests =
                publicContestPostingRepository
                        .findByContestStatusAndRegistrationEndDateGreaterThanEqualOrderByRegistrationEndDateAsc(
                                ContestStatus.OPEN,
                                LocalDate.of(2026, 3, 17)
                        );

        assertThat(contests).hasSize(2);
        assertThat(contests.get(0).getExternalId()).isEqualTo("dou-4");
        assertThat(contests.get(1).getExternalId()).isEqualTo("dou-3");
    }

    @Test
    @DisplayName("should persist relationship with CrawlExecutionEntity")
    void shouldPersistRelationshipWithCrawlExecutionEntity() {
        PublicContestPostingEntity saved = publicContestPostingRepository.save(buildPosting(
                "dou-6",
                "https://www.in.gov.br/web/dou/-/edital-6",
                LocalDate.of(2026, 3, 7),
                LocalDate.of(2026, 3, 24),
                "sha256:contest-6",
                ContestStatus.OPEN
        ));

        Optional<PublicContestPostingEntity> found = publicContestPostingRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCrawlExecution().getId()).isEqualTo(savedExecution.getId());
        assertThat(found.get().getTargetSite().getId()).isEqualTo(savedSite.getId());
    }

    private PublicContestPostingEntity buildPosting(
            String externalId,
            String canonicalUrl,
            LocalDate publishedAt,
            LocalDate registrationEndDate,
            String fingerprintHash,
            ContestStatus contestStatus
    ) {
        return PublicContestPostingEntity.builder()
                .crawlExecution(savedExecution)
                .targetSite(savedSite)
                .externalId(externalId)
                .canonicalUrl(canonicalUrl)
                .contestName("Concurso Publico SERPRO 2026")
                .organizer("SERPRO")
                .positionTitle("Analista de TI")
                .governmentLevel(GovernmentLevel.FEDERAL)
                .state("DF")
                .educationLevel(EducationLevel.SUPERIOR)
                .numberOfVacancies(20)
                .baseSalary(new BigDecimal("12345.67"))
                .salaryDescription("Remuneracao inicial")
                .editalUrl("https://www.in.gov.br/edital.pdf")
                .publishedAt(publishedAt)
                .registrationStartDate(publishedAt.plusDays(2))
                .registrationEndDate(registrationEndDate)
                .examDate(registrationEndDate.plusDays(20))
                .contestStatus(contestStatus)
                .fingerprintHash(fingerprintHash)
                .dedupStatus(DedupStatus.NEW)
                .payloadJson("{\"source\":\"dou\"}")
                .createdAt(Instant.parse("2026-03-12T14:05:30Z"))
                .updatedAt(Instant.parse("2026-03-12T14:06:30Z"))
                .build();
    }
}
