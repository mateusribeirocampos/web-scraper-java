package com.campos.webscraper.domain.model;

import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.DedupStatus;
import com.campos.webscraper.domain.enums.EducationLevel;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.GovernmentLevel;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PublicContestPostingEntity — validates builder behaviour and required fields.
 *
 * TDD RED: written before the entity exists.
 */
@DisplayName("PublicContestPostingEntity")
class PublicContestPostingEntityTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build a valid PublicContestPostingEntity with required fields")
        void shouldBuildValidEntityWithRequiredFields() {
            Instant now = Instant.parse("2026-03-12T14:05:30Z");
            LocalDate publishedAt = LocalDate.of(2026, 3, 5);

            TargetSiteEntity targetSite = buildTargetSite(now);
            CrawlExecutionEntity crawlExecution = buildExecution(now, targetSite);

            PublicContestPostingEntity posting = PublicContestPostingEntity.builder()
                    .crawlExecution(crawlExecution)
                    .targetSite(targetSite)
                    .externalId("dou-123")
                    .canonicalUrl("https://www.in.gov.br/web/dou/-/edital-123")
                    .contestName("Concurso Publico SERPRO 2026")
                    .organizer("SERPRO")
                    .positionTitle("Analista de TI")
                    .governmentLevel(GovernmentLevel.FEDERAL)
                    .state("DF")
                    .educationLevel(EducationLevel.SUPERIOR)
                    .numberOfVacancies(12)
                    .baseSalary(new BigDecimal("12345.67"))
                    .salaryDescription("Remuneracao inicial")
                    .editalUrl("https://www.in.gov.br/edital.pdf")
                    .publishedAt(publishedAt)
                    .registrationStartDate(LocalDate.of(2026, 3, 7))
                    .registrationEndDate(LocalDate.of(2026, 3, 21))
                    .examDate(LocalDate.of(2026, 4, 18))
                    .contestStatus(ContestStatus.OPEN)
                    .fingerprintHash("sha256:contest-123")
                    .dedupStatus(DedupStatus.NEW)
                    .payloadJson("{\"source\":\"dou\"}")
                    .createdAt(now)
                    .updatedAt(now.plusSeconds(60))
                    .build();

            assertThat(posting.getCrawlExecution()).isEqualTo(crawlExecution);
            assertThat(posting.getTargetSite()).isEqualTo(targetSite);
            assertThat(posting.getContestName()).isEqualTo("Concurso Publico SERPRO 2026");
            assertThat(posting.getRegistrationEndDate()).isEqualTo(LocalDate.of(2026, 3, 21));
            assertThat(posting.getContestStatus()).isEqualTo(ContestStatus.OPEN);
            assertThat(posting.getId()).isNull();
        }

        @Test
        @DisplayName("should keep publishedAt and registrationEndDate as first-class date fields")
        void shouldKeepPublishedAtAndRegistrationEndDateAsFirstClassDateFields() {
            Instant now = Instant.parse("2026-03-12T14:05:30Z");

            PublicContestPostingEntity posting = PublicContestPostingEntity.builder()
                    .crawlExecution(buildExecution(now, buildTargetSite(now)))
                    .targetSite(buildTargetSite(now))
                    .canonicalUrl("https://gov.example/1")
                    .contestName("Concurso TI")
                    .organizer("MPO")
                    .positionTitle("Analista")
                    .governmentLevel(GovernmentLevel.FEDERAL)
                    .educationLevel(EducationLevel.SUPERIOR)
                    .publishedAt(LocalDate.of(2026, 3, 1))
                    .registrationEndDate(LocalDate.of(2026, 3, 15))
                    .contestStatus(ContestStatus.OPEN)
                    .fingerprintHash("sha256:dates")
                    .dedupStatus(DedupStatus.NEW)
                    .createdAt(now)
                    .build();

            assertThat(posting.getPublishedAt()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(posting.getRegistrationEndDate()).isEqualTo(LocalDate.of(2026, 3, 15));
        }

        @Test
        @DisplayName("should allow all relevant enums to be assigned explicitly")
        void shouldAllowRelevantEnumsToBeAssigned() {
            Instant now = Instant.parse("2026-03-12T14:05:30Z");

            PublicContestPostingEntity posting = PublicContestPostingEntity.builder()
                    .crawlExecution(buildExecution(now, buildTargetSite(now)))
                    .targetSite(buildTargetSite(now))
                    .canonicalUrl("https://gov.example/2")
                    .contestName("Concurso TI Estadual")
                    .organizer("Secretaria Estadual")
                    .positionTitle("Tecnico")
                    .governmentLevel(GovernmentLevel.ESTADUAL)
                    .educationLevel(EducationLevel.TECNICO)
                    .publishedAt(LocalDate.of(2026, 3, 2))
                    .registrationEndDate(LocalDate.of(2026, 3, 22))
                    .contestStatus(ContestStatus.REGISTRATION_CLOSED)
                    .fingerprintHash("sha256:enums")
                    .dedupStatus(DedupStatus.DUPLICATE)
                    .createdAt(now)
                    .build();

            assertThat(posting.getGovernmentLevel()).isEqualTo(GovernmentLevel.ESTADUAL);
            assertThat(posting.getEducationLevel()).isEqualTo(EducationLevel.TECNICO);
            assertThat(posting.getContestStatus()).isEqualTo(ContestStatus.REGISTRATION_CLOSED);
            assertThat(posting.getDedupStatus()).isEqualTo(DedupStatus.DUPLICATE);
        }

        @Test
        @DisplayName("should require crawlExecution relationship")
        void shouldRequireCrawlExecutionRelationship() {
            Instant now = Instant.parse("2026-03-12T14:05:30Z");

            PublicContestPostingEntity posting = PublicContestPostingEntity.builder()
                    .targetSite(buildTargetSite(now))
                    .canonicalUrl("https://gov.example/3")
                    .contestName("Concurso TI")
                    .organizer("SERPRO")
                    .positionTitle("Analista")
                    .governmentLevel(GovernmentLevel.FEDERAL)
                    .educationLevel(EducationLevel.SUPERIOR)
                    .publishedAt(LocalDate.of(2026, 3, 5))
                    .contestStatus(ContestStatus.OPEN)
                    .fingerprintHash("sha256:no-execution")
                    .dedupStatus(DedupStatus.NEW)
                    .createdAt(now)
                    .build();

            assertThat(posting.getCrawlExecution()).isNull();
        }

        @Test
        @DisplayName("should require targetSite relationship")
        void shouldRequireTargetSiteRelationship() {
            Instant now = Instant.parse("2026-03-12T14:05:30Z");

            PublicContestPostingEntity posting = PublicContestPostingEntity.builder()
                    .crawlExecution(buildExecution(now, buildTargetSite(now)))
                    .canonicalUrl("https://gov.example/4")
                    .contestName("Concurso TI")
                    .organizer("SERPRO")
                    .positionTitle("Analista")
                    .governmentLevel(GovernmentLevel.FEDERAL)
                    .educationLevel(EducationLevel.SUPERIOR)
                    .publishedAt(LocalDate.of(2026, 3, 5))
                    .contestStatus(ContestStatus.OPEN)
                    .fingerprintHash("sha256:no-site")
                    .dedupStatus(DedupStatus.NEW)
                    .createdAt(now)
                    .build();

            assertThat(posting.getTargetSite()).isNull();
        }
    }

    private static TargetSiteEntity buildTargetSite(Instant now) {
        return TargetSiteEntity.builder()
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
                .build();
    }

    private static CrawlExecutionEntity buildExecution(Instant now, TargetSiteEntity site) {
        CrawlJobEntity job = CrawlJobEntity.builder()
                .targetSite(site)
                .scheduledAt(now)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
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
