package com.campos.webscraper.domain.model;

import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.DedupStatus;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.JobContractType;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SeniorityLevel;
import com.campos.webscraper.domain.enums.SiteType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JobPostingEntity — validates builder behaviour and required domain fields.
 *
 * TDD RED: written before the entity exists.
 */
@DisplayName("JobPostingEntity")
class JobPostingEntityTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build a valid JobPostingEntity with all required fields")
        void shouldBuildValidEntityWithRequiredFields() {
            Instant now = Instant.parse("2026-03-12T10:15:30Z");
            LocalDate publishedAt = LocalDate.of(2026, 3, 5);

            TargetSiteEntity targetSite = buildTargetSite(now);
            CrawlExecutionEntity crawlExecution = buildExecution(now, targetSite);

            JobPostingEntity posting = JobPostingEntity.builder()
                    .crawlExecution(crawlExecution)
                    .targetSite(targetSite)
                    .externalId("indeed-job-123")
                    .canonicalUrl("https://br.indeed.com/viewjob?jk=123")
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
                    .applicationDeadline(LocalDate.of(2026, 3, 20))
                    .fingerprintHash("sha256:abc123")
                    .dedupStatus(DedupStatus.NEW)
                    .payloadJson("{\"source\":\"indeed\"}")
                    .createdAt(now)
                    .updatedAt(now.plusSeconds(60))
                    .build();

            assertThat(posting.getCrawlExecution()).isEqualTo(crawlExecution);
            assertThat(posting.getTargetSite()).isEqualTo(targetSite);
            assertThat(posting.getCanonicalUrl()).isEqualTo("https://br.indeed.com/viewjob?jk=123");
            assertThat(posting.getPublishedAt()).isEqualTo(publishedAt);
            assertThat(posting.getDedupStatus()).isEqualTo(DedupStatus.NEW);
            assertThat(posting.getId()).isNull();
        }

        @Test
        @DisplayName("should keep publishedAt as a first-class field for chronology")
        void shouldKeepPublishedAtAsFirstClassField() {
            Instant now = Instant.parse("2026-03-12T10:15:30Z");
            LocalDate publishedAt = LocalDate.of(2026, 3, 5);

            JobPostingEntity posting = JobPostingEntity.builder()
                    .crawlExecution(buildExecution(now, buildTargetSite(now)))
                    .targetSite(buildTargetSite(now))
                    .canonicalUrl("https://jobs.example.com/1")
                    .title("Java Developer")
                    .company("ACME")
                    .remote(false)
                    .contractType(JobContractType.PJ)
                    .seniority(SeniorityLevel.JUNIOR)
                    .publishedAt(publishedAt)
                    .fingerprintHash("sha256:published-at")
                    .dedupStatus(DedupStatus.NEW)
                    .createdAt(now)
                    .build();

            assertThat(posting.getPublishedAt()).isEqualTo(publishedAt);
        }

        @Test
        @DisplayName("should allow all relevant enums to be assigned explicitly")
        void shouldAllowRelevantEnumsToBeAssigned() {
            Instant now = Instant.parse("2026-03-12T10:15:30Z");

            JobPostingEntity posting = JobPostingEntity.builder()
                    .crawlExecution(buildExecution(now, buildTargetSite(now)))
                    .targetSite(buildTargetSite(now))
                    .canonicalUrl("https://jobs.example.com/2")
                    .title("Backend Java")
                    .company("ACME")
                    .remote(true)
                    .contractType(JobContractType.TEMPORARY)
                    .seniority(SeniorityLevel.MID)
                    .publishedAt(LocalDate.of(2026, 3, 6))
                    .fingerprintHash("sha256:enums")
                    .dedupStatus(DedupStatus.DUPLICATE)
                    .createdAt(now)
                    .build();

            assertThat(posting.getContractType()).isEqualTo(JobContractType.TEMPORARY);
            assertThat(posting.getSeniority()).isEqualTo(SeniorityLevel.MID);
            assertThat(posting.getDedupStatus()).isEqualTo(DedupStatus.DUPLICATE);
        }

        @Test
        @DisplayName("should require crawlExecution relationship")
        void shouldRequireCrawlExecutionRelationship() {
            Instant now = Instant.parse("2026-03-12T10:15:30Z");

            JobPostingEntity posting = JobPostingEntity.builder()
                    .targetSite(buildTargetSite(now))
                    .canonicalUrl("https://jobs.example.com/3")
                    .title("Java Developer")
                    .company("ACME")
                    .remote(false)
                    .contractType(JobContractType.CLT)
                    .publishedAt(LocalDate.of(2026, 3, 7))
                    .fingerprintHash("sha256:no-execution")
                    .dedupStatus(DedupStatus.NEW)
                    .createdAt(now)
                    .build();

            assertThat(posting.getCrawlExecution()).isNull();
        }

        @Test
        @DisplayName("should require targetSite relationship")
        void shouldRequireTargetSiteRelationship() {
            Instant now = Instant.parse("2026-03-12T10:15:30Z");

            JobPostingEntity posting = JobPostingEntity.builder()
                    .crawlExecution(buildExecution(now, buildTargetSite(now)))
                    .canonicalUrl("https://jobs.example.com/4")
                    .title("Java Developer")
                    .company("ACME")
                    .remote(false)
                    .contractType(JobContractType.CLT)
                    .publishedAt(LocalDate.of(2026, 3, 7))
                    .fingerprintHash("sha256:no-site")
                    .dedupStatus(DedupStatus.NEW)
                    .createdAt(now)
                    .build();

            assertThat(posting.getTargetSite()).isNull();
        }
    }

    private static TargetSiteEntity buildTargetSite(Instant now) {
        return TargetSiteEntity.builder()
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
                .build();
    }

    private static CrawlExecutionEntity buildExecution(Instant now, TargetSiteEntity site) {
        CrawlJobEntity job = CrawlJobEntity.builder()
                .targetSite(site)
                .scheduledAt(now)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .createdAt(now)
                .build();

        return CrawlExecutionEntity.builder()
                .crawlJob(job)
                .status(CrawlExecutionStatus.SUCCEEDED)
                .startedAt(now)
                .finishedAt(now.plusSeconds(30))
                .pagesVisited(1)
                .itemsFound(1)
                .retryCount(0)
                .createdAt(now)
                .build();
    }
}
