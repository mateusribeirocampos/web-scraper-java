package com.campos.webscraper.domain.repository;

import com.campos.webscraper.TestcontainersConfiguration;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.DedupStatus;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.JobContractType;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SeniorityLevel;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for JobPostingRepository.
 *
 * TDD RED: written before repository, entity and migration V004 exist.
 */
@RepositoryPersistenceTest
@DisplayName("JobPostingRepository integration")
class JobPostingRepositoryTest extends AbstractRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = TestcontainersConfiguration.newPostgresContainer();

    @Autowired
    private JobPostingRepository jobPostingRepository;

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
        resetJobPostingPersistence();

        Instant now = Instant.parse("2026-03-12T10:15:30Z");

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
                .finishedAt(now.plusSeconds(60))
                .pagesVisited(2)
                .itemsFound(3)
                .retryCount(0)
                .createdAt(now)
                .build());
    }

    @Test
    @DisplayName("should save and find JobPostingEntity by id")
    void shouldSaveAndFindById() {
        JobPostingEntity saved = jobPostingRepository.save(buildPosting(
                "indeed-1",
                "https://br.indeed.com/viewjob?jk=1",
                LocalDate.of(2026, 3, 5),
                "sha256:job-1"
        ));

        Optional<JobPostingEntity> found = jobPostingRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getExternalId()).isEqualTo("indeed-1");
        assertThat(found.get().getCanonicalUrl()).isEqualTo("https://br.indeed.com/viewjob?jk=1");
    }

    @Test
    @DisplayName("should find JobPostings by targetSite")
    void shouldFindByTargetSite() {
        jobPostingRepository.save(buildPosting(
                "indeed-2",
                "https://br.indeed.com/viewjob?jk=2",
                LocalDate.of(2026, 3, 5),
                "sha256:job-2"
        ));

        List<JobPostingEntity> postings = jobPostingRepository.findByTargetSite(savedSite);

        assertThat(postings).hasSize(1);
        assertThat(postings.get(0).getTargetSite().getId()).isEqualTo(savedSite.getId());
    }

    @Test
    @DisplayName("should find JobPostings published on or after a given date")
    void shouldFindByPublishedAtGreaterThanEqual() {
        jobPostingRepository.save(buildPosting(
                "indeed-3",
                "https://br.indeed.com/viewjob?jk=3",
                LocalDate.of(2026, 3, 4),
                "sha256:job-3"
        ));
        jobPostingRepository.save(buildPosting(
                "indeed-4",
                "https://br.indeed.com/viewjob?jk=4",
                LocalDate.of(2026, 3, 8),
                "sha256:job-4"
        ));

        List<JobPostingEntity> postings = jobPostingRepository.findByPublishedAtGreaterThanEqual(
                LocalDate.of(2026, 3, 5)
        );

        assertThat(postings).hasSize(1);
        assertThat(postings.get(0).getExternalId()).isEqualTo("indeed-4");
    }

    @Test
    @DisplayName("should find JobPosting by fingerprintHash")
    void shouldFindByFingerprintHash() {
        jobPostingRepository.save(buildPosting(
                "indeed-5",
                "https://br.indeed.com/viewjob?jk=5",
                LocalDate.of(2026, 3, 6),
                "sha256:fingerprint-5"
        ));

        Optional<JobPostingEntity> found = jobPostingRepository.findByFingerprintHash("sha256:fingerprint-5");

        assertThat(found).isPresent();
        assertThat(found.get().getExternalId()).isEqualTo("indeed-5");
    }

    @Test
    @DisplayName("should persist canonicalUrl and required fields correctly")
    void shouldPersistCanonicalUrlAndRequiredFieldsCorrectly() {
        JobPostingEntity saved = jobPostingRepository.save(buildPosting(
                "indeed-6",
                "https://br.indeed.com/viewjob?jk=6",
                LocalDate.of(2026, 3, 7),
                "sha256:job-6"
        ));

        Optional<JobPostingEntity> found = jobPostingRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Java Backend Developer Jr");
        assertThat(found.get().getCompany()).isEqualTo("Invillia");
        assertThat(found.get().getContractType()).isEqualTo(JobContractType.CLT);
        assertThat(found.get().getPublishedAt()).isEqualTo(LocalDate.of(2026, 3, 7));
    }

    @Test
    @DisplayName("should persist relationship with CrawlExecutionEntity")
    void shouldPersistRelationshipWithCrawlExecutionEntity() {
        JobPostingEntity saved = jobPostingRepository.save(buildPosting(
                "indeed-7",
                "https://br.indeed.com/viewjob?jk=7",
                LocalDate.of(2026, 3, 9),
                "sha256:job-7"
        ));

        Optional<JobPostingEntity> found = jobPostingRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCrawlExecution().getId()).isEqualTo(savedExecution.getId());
        assertThat(found.get().getTargetSite().getId()).isEqualTo(savedSite.getId());
    }

    private JobPostingEntity buildPosting(
            String externalId,
            String canonicalUrl,
            LocalDate publishedAt,
            String fingerprintHash
    ) {
        return JobPostingEntity.builder()
                .crawlExecution(savedExecution)
                .targetSite(savedSite)
                .externalId(externalId)
                .canonicalUrl(canonicalUrl)
                .title("Java Backend Developer Jr")
                .company("Invillia")
                .location("Remoto")
                .remote(true)
                .contractType(JobContractType.CLT)
                .seniority(SeniorityLevel.JUNIOR)
                .salaryRange("R$ 3.000 - R$ 5.000")
                .techStackTags("Java,Spring Boot,PostgreSQL")
                .description("Desenvolvimento de APIs REST")
                .publishedAt(publishedAt)
                .applicationDeadline(publishedAt.plusDays(10))
                .fingerprintHash(fingerprintHash)
                .dedupStatus(DedupStatus.NEW)
                .payloadJson("{\"source\":\"indeed\"}")
                .createdAt(Instant.parse("2026-03-12T10:15:30Z"))
                .updatedAt(Instant.parse("2026-03-12T10:16:30Z"))
                .build();
    }
}
