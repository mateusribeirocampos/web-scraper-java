package com.campos.webscraper.shared;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JobPostingFingerprintCalculator.
 *
 * TDD RED: written before the calculator exists.
 */
@Tag("unit")
@DisplayName("JobPostingFingerprintCalculator")
class JobPostingFingerprintCalculatorTest {

    @Test
    @DisplayName("should generate same hash for same canonical input")
    void shouldGenerateSameHashForSameCanonicalInput() {
        JobPostingFingerprintCalculator calculator = new JobPostingFingerprintCalculator();

        JobPostingEntity first = buildPosting("Java Backend Developer Jr", "Invillia");
        JobPostingEntity second = buildPosting("Java Backend Developer Jr", "Invillia");

        assertThat(calculator.calculate(first)).isEqualTo(calculator.calculate(second));
    }

    @Test
    @DisplayName("should generate different hashes when title changes")
    void shouldGenerateDifferentHashesWhenTitleChanges() {
        JobPostingFingerprintCalculator calculator = new JobPostingFingerprintCalculator();

        JobPostingEntity first = buildPosting("Java Backend Developer Jr", "Invillia");
        JobPostingEntity second = buildPosting("Java Software Engineer Jr", "Invillia");

        assertThat(calculator.calculate(first)).isNotEqualTo(calculator.calculate(second));
    }

    @Test
    @DisplayName("should generate different hashes when company changes")
    void shouldGenerateDifferentHashesWhenCompanyChanges() {
        JobPostingFingerprintCalculator calculator = new JobPostingFingerprintCalculator();

        JobPostingEntity first = buildPosting("Java Backend Developer Jr", "Invillia");
        JobPostingEntity second = buildPosting("Java Backend Developer Jr", "Nubank");

        assertThat(calculator.calculate(first)).isNotEqualTo(calculator.calculate(second));
    }

    @Test
    @DisplayName("should normalize trim and case before generating hash")
    void shouldNormalizeTrimAndCaseBeforeGeneratingHash() {
        JobPostingFingerprintCalculator calculator = new JobPostingFingerprintCalculator();

        JobPostingEntity first = buildPosting("Java Backend Developer Jr", "Invillia");
        JobPostingEntity second = buildPosting("  java backend developer jr  ", "  INVILLIA ");

        assertThat(calculator.calculate(first)).isEqualTo(calculator.calculate(second));
    }

    @Test
    @DisplayName("should consider target site, external id, canonical url, title, company and publishedAt")
    void shouldConsiderCanonicalFieldsExplicitly() {
        JobPostingFingerprintCalculator calculator = new JobPostingFingerprintCalculator();

        JobPostingEntity first = buildPosting("Java Backend Developer Jr", "Invillia");
        JobPostingEntity second = JobPostingEntity.builder()
                .crawlExecution(first.getCrawlExecution())
                .targetSite(first.getTargetSite())
                .externalId("indeed-job-999")
                .canonicalUrl(first.getCanonicalUrl())
                .title(first.getTitle())
                .company(first.getCompany())
                .remote(first.isRemote())
                .contractType(first.getContractType())
                .seniority(first.getSeniority())
                .publishedAt(first.getPublishedAt())
                .fingerprintHash(first.getFingerprintHash())
                .dedupStatus(first.getDedupStatus())
                .createdAt(first.getCreatedAt())
                .build();

        assertThat(calculator.calculate(first)).isNotEqualTo(calculator.calculate(second));
    }

    private static JobPostingEntity buildPosting(String title, String company) {
        Instant now = Instant.parse("2026-03-12T10:15:30Z");
        TargetSiteEntity targetSite = TargetSiteEntity.builder()
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

        CrawlJobEntity job = CrawlJobEntity.builder()
                .targetSite(targetSite)
                .scheduledAt(now)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .createdAt(now)
                .build();

        CrawlExecutionEntity execution = CrawlExecutionEntity.builder()
                .crawlJob(job)
                .status(CrawlExecutionStatus.SUCCEEDED)
                .startedAt(now)
                .finishedAt(now.plusSeconds(60))
                .pagesVisited(1)
                .itemsFound(1)
                .retryCount(0)
                .createdAt(now)
                .build();

        return JobPostingEntity.builder()
                .crawlExecution(execution)
                .targetSite(targetSite)
                .externalId("indeed-job-123")
                .canonicalUrl("https://br.indeed.com/viewjob?jk=123")
                .title(title)
                .company(company)
                .location("Remoto")
                .remote(true)
                .contractType(JobContractType.CLT)
                .seniority(SeniorityLevel.JUNIOR)
                .publishedAt(LocalDate.of(2026, 3, 5))
                .fingerprintHash("placeholder")
                .dedupStatus(DedupStatus.NEW)
                .createdAt(now)
                .build();
    }
}
