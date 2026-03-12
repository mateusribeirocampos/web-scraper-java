package com.campos.webscraper.shared;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ContestPostingFingerprintCalculator.
 *
 * TDD RED: written before the calculator exists.
 */
@Tag("unit")
@DisplayName("ContestPostingFingerprintCalculator")
class ContestPostingFingerprintCalculatorTest {

    @Test
    @DisplayName("should generate same hash for same canonical input")
    void shouldGenerateSameHashForSameCanonicalInput() {
        ContestPostingFingerprintCalculator calculator = new ContestPostingFingerprintCalculator();

        PublicContestPostingEntity first = buildPosting("Concurso Publico SERPRO 2026", "SERPRO");
        PublicContestPostingEntity second = buildPosting("Concurso Publico SERPRO 2026", "SERPRO");

        assertThat(calculator.calculate(first)).isEqualTo(calculator.calculate(second));
    }

    @Test
    @DisplayName("should generate different hashes when contestName changes")
    void shouldGenerateDifferentHashesWhenContestNameChanges() {
        ContestPostingFingerprintCalculator calculator = new ContestPostingFingerprintCalculator();

        PublicContestPostingEntity first = buildPosting("Concurso Publico SERPRO 2026", "SERPRO");
        PublicContestPostingEntity second = buildPosting("Concurso Publico DATAPREV 2026", "SERPRO");

        assertThat(calculator.calculate(first)).isNotEqualTo(calculator.calculate(second));
    }

    @Test
    @DisplayName("should generate different hashes when organizer changes")
    void shouldGenerateDifferentHashesWhenOrganizerChanges() {
        ContestPostingFingerprintCalculator calculator = new ContestPostingFingerprintCalculator();

        PublicContestPostingEntity first = buildPosting("Concurso Publico SERPRO 2026", "SERPRO");
        PublicContestPostingEntity second = buildPosting("Concurso Publico SERPRO 2026", "DATAPREV");

        assertThat(calculator.calculate(first)).isNotEqualTo(calculator.calculate(second));
    }

    @Test
    @DisplayName("should normalize trim and case before generating hash")
    void shouldNormalizeTrimAndCaseBeforeGeneratingHash() {
        ContestPostingFingerprintCalculator calculator = new ContestPostingFingerprintCalculator();

        PublicContestPostingEntity first = buildPosting("Concurso Publico SERPRO 2026", "SERPRO");
        PublicContestPostingEntity second = buildPosting("  concurso publico serpro 2026  ", "  serpro ");

        assertThat(calculator.calculate(first)).isEqualTo(calculator.calculate(second));
    }

    @Test
    @DisplayName("should consider target site, external id, contestName, organizer and registrationEndDate")
    void shouldConsiderCanonicalFieldsExplicitly() {
        ContestPostingFingerprintCalculator calculator = new ContestPostingFingerprintCalculator();

        PublicContestPostingEntity first = buildPosting("Concurso Publico SERPRO 2026", "SERPRO");
        PublicContestPostingEntity second = PublicContestPostingEntity.builder()
                .crawlExecution(first.getCrawlExecution())
                .targetSite(first.getTargetSite())
                .externalId("dou-999")
                .canonicalUrl(first.getCanonicalUrl())
                .contestName(first.getContestName())
                .organizer(first.getOrganizer())
                .positionTitle(first.getPositionTitle())
                .governmentLevel(first.getGovernmentLevel())
                .educationLevel(first.getEducationLevel())
                .publishedAt(first.getPublishedAt())
                .registrationEndDate(first.getRegistrationEndDate())
                .contestStatus(first.getContestStatus())
                .fingerprintHash(first.getFingerprintHash())
                .dedupStatus(first.getDedupStatus())
                .createdAt(first.getCreatedAt())
                .build();

        assertThat(calculator.calculate(first)).isNotEqualTo(calculator.calculate(second));
    }

    private static PublicContestPostingEntity buildPosting(String contestName, String organizer) {
        Instant now = Instant.parse("2026-03-12T14:05:30Z");
        TargetSiteEntity targetSite = TargetSiteEntity.builder()
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

        CrawlJobEntity job = CrawlJobEntity.builder()
                .targetSite(targetSite)
                .scheduledAt(now)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
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

        return PublicContestPostingEntity.builder()
                .crawlExecution(execution)
                .targetSite(targetSite)
                .externalId("dou-123")
                .canonicalUrl("https://www.in.gov.br/web/dou/-/edital-123")
                .contestName(contestName)
                .organizer(organizer)
                .positionTitle("Analista de TI")
                .governmentLevel(GovernmentLevel.FEDERAL)
                .educationLevel(EducationLevel.SUPERIOR)
                .publishedAt(LocalDate.of(2026, 3, 5))
                .registrationEndDate(LocalDate.of(2026, 3, 20))
                .contestStatus(ContestStatus.OPEN)
                .fingerprintHash("placeholder")
                .dedupStatus(DedupStatus.NEW)
                .createdAt(now)
                .build();
    }
}
