package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.orchestrator.CrawlJobDispatcher;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.CrawlJobRepository;
import com.campos.webscraper.shared.CrawlJobNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the manual crawl job execution use case.
 *
 * TDD RED: written before the use case exists.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("ExecuteCrawlJobManuallyUseCase")
class ExecuteCrawlJobManuallyUseCaseTest {

    @Mock
    private CrawlJobRepository crawlJobRepository;

    @Mock
    private CrawlJobDispatcher crawlJobDispatcher;

    @Test
    @DisplayName("should dispatch an existing crawl job by id")
    void shouldDispatchAnExistingCrawlJobById() {
        CrawlJobEntity crawlJob = buildJob(42L);

        when(crawlJobRepository.findById(42L)).thenReturn(Optional.of(crawlJob));

        ExecuteCrawlJobManuallyUseCase useCase =
                new ExecuteCrawlJobManuallyUseCase(crawlJobRepository, crawlJobDispatcher);

        useCase.execute(42L);

        verify(crawlJobRepository).findById(42L);
        verify(crawlJobDispatcher).dispatch(crawlJob);
    }

    @Test
    @DisplayName("should fail when crawl job does not exist")
    void shouldFailWhenCrawlJobDoesNotExist() {
        when(crawlJobRepository.findById(99L)).thenReturn(Optional.empty());

        ExecuteCrawlJobManuallyUseCase useCase =
                new ExecuteCrawlJobManuallyUseCase(crawlJobRepository, crawlJobDispatcher);

        assertThatThrownBy(() -> useCase.execute(99L))
                .isInstanceOf(CrawlJobNotFoundException.class)
                .hasMessage("Crawl job not found: 99");
    }

    private static CrawlJobEntity buildJob(Long id) {
        Instant now = Instant.parse("2026-03-12T18:10:00Z");
        return CrawlJobEntity.builder()
                .id(id)
                .targetSite(TargetSiteEntity.builder()
                        .siteCode("indeed-br")
                        .displayName("Indeed Brasil")
                        .baseUrl("https://br.indeed.com")
                        .siteType(SiteType.TYPE_E)
                        .extractionMode(ExtractionMode.API)
                        .jobCategory(JobCategory.PRIVATE_SECTOR)
                        .legalStatus(LegalStatus.APPROVED)
                        .selectorBundleVersion("n/a")
                        .enabled(true)
                        .createdAt(now.minusSeconds(600))
                        .build())
                .scheduledAt(now)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .createdAt(now.minusSeconds(60))
                .build();
    }
}
