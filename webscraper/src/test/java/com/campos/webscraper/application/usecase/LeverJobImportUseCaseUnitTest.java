package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.strategy.LeverJobScraperStrategy;
import com.campos.webscraper.domain.enums.DedupStatus;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.JobPostingRepository;
import com.campos.webscraper.shared.JobPostingFingerprintCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("LeverJobImportUseCase")
class LeverJobImportUseCaseUnitTest {

    @Mock
    private JobPostingRepository jobPostingRepository;

    @Mock
    private LeverJobScraperStrategy strategy;

    @Mock
    private JobPostingFingerprintCalculator fingerprintCalculator;

    @Mock
    private IdempotentJobPostingPersistenceService idempotentPersistenceService;

    @Test
    @DisplayName("should preserve the oldest published date already seen for the same external id")
    void shouldPreserveTheOldestPublishedDateAlreadySeenForTheSameExternalId() {
        TargetSiteEntity targetSite = TargetSiteEntity.builder().id(10L).siteCode("lever_ciandt").build();
        CrawlExecutionEntity crawlExecution = CrawlExecutionEntity.builder()
                .id(100L)
                .startedAt(Instant.parse("2026-03-31T10:00:00Z"))
                .build();
        ScrapeCommand command = new ScrapeCommand(
                "lever_ciandt",
                "https://api.lever.co/v0/postings/ciandt?mode=json",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR
        );

        JobPostingEntity scraped = JobPostingEntity.builder()
                .externalId("job-123")
                .canonicalUrl("https://jobs.lever.co/ciandt/job-123")
                .title("Senior Java Engineer")
                .company("CI&T")
                .dedupStatus(DedupStatus.NEW)
                .build();

        JobPostingEntity oldest = JobPostingEntity.builder()
                .id(1L)
                .targetSite(targetSite)
                .externalId("job-123")
                .canonicalUrl("https://jobs.lever.co/ciandt/job-123")
                .title("Senior Java Engineer")
                .publishedAt(LocalDate.of(2026, 3, 1))
                .build();

        JobPostingEntity newest = JobPostingEntity.builder()
                .id(2L)
                .targetSite(targetSite)
                .externalId("job-123")
                .canonicalUrl("https://jobs.lever.co/ciandt/job-123")
                .title("Senior Java Engineer")
                .publishedAt(LocalDate.of(2026, 3, 20))
                .build();

        when(strategy.scrape(command)).thenReturn(ScrapeResult.success(List.of(scraped), "lever_ciandt"));
        when(jobPostingRepository.findByTargetSiteAndExternalIdOrderByPublishedAtDescCreatedAtDesc(targetSite, "job-123"))
                .thenReturn(List.of(oldest, newest));
        when(fingerprintCalculator.calculate(any(JobPostingEntity.class))).thenReturn("fp-123");
        when(idempotentPersistenceService.persist(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        LeverJobImportUseCase useCase = new LeverJobImportUseCase(
                jobPostingRepository,
                strategy,
                fingerprintCalculator,
                idempotentPersistenceService
        );

        List<JobPostingEntity> persisted = useCase.execute(targetSite, crawlExecution, command);

        assertThat(persisted).hasSize(1);
        assertThat(persisted.get(0).getPublishedAt()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(persisted.get(0).getFingerprintHash()).isEqualTo("fp-123");
    }

    @Test
    @DisplayName("should preserve the oldest published date even when title and url change for the same Lever external id")
    void shouldPreserveTheOldestPublishedDateEvenWhenTitleAndUrlChangeForTheSameLeverExternalId() {
        TargetSiteEntity targetSite = TargetSiteEntity.builder().id(10L).siteCode("lever_ciandt").build();
        CrawlExecutionEntity crawlExecution = CrawlExecutionEntity.builder()
                .id(100L)
                .startedAt(Instant.parse("2026-03-31T10:00:00Z"))
                .build();
        ScrapeCommand command = new ScrapeCommand(
                "lever_ciandt",
                "https://api.lever.co/v0/postings/ciandt?mode=json",
                ExtractionMode.API,
                JobCategory.PRIVATE_SECTOR
        );

        JobPostingEntity scraped = JobPostingEntity.builder()
                .externalId("job-123")
                .canonicalUrl("https://jobs.lever.co/ciandt/job-123-reopened")
                .title("Senior Java Engineer Reopened")
                .company("CI&T")
                .dedupStatus(DedupStatus.NEW)
                .build();

        JobPostingEntity historical = JobPostingEntity.builder()
                .id(1L)
                .targetSite(targetSite)
                .externalId("job-123")
                .canonicalUrl("https://jobs.lever.co/ciandt/job-123")
                .title("Senior Java Engineer")
                .publishedAt(LocalDate.of(2026, 3, 1))
                .build();

        when(strategy.scrape(command)).thenReturn(ScrapeResult.success(List.of(scraped), "lever_ciandt"));
        when(jobPostingRepository.findByTargetSiteAndExternalIdOrderByPublishedAtDescCreatedAtDesc(targetSite, "job-123"))
                .thenReturn(List.of(historical));
        when(fingerprintCalculator.calculate(any(JobPostingEntity.class))).thenReturn("fp-reposted");
        when(idempotentPersistenceService.persist(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        LeverJobImportUseCase useCase = new LeverJobImportUseCase(
                jobPostingRepository,
                strategy,
                fingerprintCalculator,
                idempotentPersistenceService
        );

        List<JobPostingEntity> persisted = useCase.execute(targetSite, crawlExecution, command);

        assertThat(persisted).hasSize(1);
        assertThat(persisted.get(0).getPublishedAt()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(persisted.get(0).getFingerprintHash()).isEqualTo("fp-reposted");
    }
}
