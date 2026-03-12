package com.campos.webscraper.application.usecase;

import com.campos.webscraper.domain.enums.SeniorityLevel;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.repository.JobPostingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for querying private-sector job postings.
 *
 * TDD RED: written before the use case exists.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("ListJobPostingsUseCase")
class ListJobPostingsUseCaseTest {

    @Mock
    private JobPostingRepository jobPostingRepository;

    @Test
    @DisplayName("should query postings by since date and seniority ordered by publishedAt desc")
    void shouldQueryPostingsBySinceDateAndSeniorityOrderedByPublishedAtDesc() {
        LocalDate since = LocalDate.of(2026, 3, 1);
        JobPostingEntity first = JobPostingEntity.builder()
                .title("Java Backend Developer")
                .company("Acme")
                .canonicalUrl("https://example.com/jobs/1")
                .publishedAt(LocalDate.of(2026, 3, 10))
                .build();
        JobPostingEntity second = JobPostingEntity.builder()
                .title("Spring Boot Developer")
                .company("Beta")
                .canonicalUrl("https://example.com/jobs/2")
                .publishedAt(LocalDate.of(2026, 3, 8))
                .build();

        when(jobPostingRepository.findByPublishedAtGreaterThanEqualAndSeniorityOrderByPublishedAtDesc(
                since,
                SeniorityLevel.JUNIOR
        )).thenReturn(List.of(first, second));

        ListJobPostingsUseCase useCase = new ListJobPostingsUseCase(jobPostingRepository);

        List<JobPostingEntity> result = useCase.execute(since, SeniorityLevel.JUNIOR);

        assertThat(result).containsExactly(first, second);
        verify(jobPostingRepository).findByPublishedAtGreaterThanEqualAndSeniorityOrderByPublishedAtDesc(
                since,
                SeniorityLevel.JUNIOR
        );
    }
}
