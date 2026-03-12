package com.campos.webscraper.application.usecase;

import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.repository.PublicContestPostingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for querying public contests.
 *
 * TDD RED: written before the use case exists.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("ListPublicContestsUseCase")
class ListPublicContestsUseCaseTest {

    @Mock
    private PublicContestPostingRepository publicContestPostingRepository;

    @Test
    @DisplayName("should query contests by status ordered by registration end date asc")
    void shouldQueryContestsByStatusOrderedByRegistrationEndDateAsc() {
        PublicContestPostingEntity first = PublicContestPostingEntity.builder()
                .contestName("Concurso A")
                .canonicalUrl("https://example.com/contest/1")
                .organizer("Org A")
                .positionTitle("Analista")
                .publishedAt(LocalDate.of(2026, 3, 1))
                .registrationEndDate(LocalDate.of(2026, 3, 20))
                .build();
        PublicContestPostingEntity second = PublicContestPostingEntity.builder()
                .contestName("Concurso B")
                .canonicalUrl("https://example.com/contest/2")
                .organizer("Org B")
                .positionTitle("Desenvolvedor")
                .publishedAt(LocalDate.of(2026, 3, 2))
                .registrationEndDate(LocalDate.of(2026, 3, 25))
                .build();

        when(publicContestPostingRepository.findByContestStatusOrderByRegistrationEndDateAsc(ContestStatus.OPEN))
                .thenReturn(List.of(first, second));

        ListPublicContestsUseCase useCase = new ListPublicContestsUseCase(publicContestPostingRepository);

        List<PublicContestPostingEntity> result = useCase.execute(ContestStatus.OPEN, "registrationEndDate");

        assertThat(result).containsExactly(first, second);
        verify(publicContestPostingRepository).findByContestStatusOrderByRegistrationEndDateAsc(ContestStatus.OPEN);
    }

    @Test
    @DisplayName("should fail for unsupported contest ordering")
    void shouldFailForUnsupportedContestOrdering() {
        ListPublicContestsUseCase useCase = new ListPublicContestsUseCase(publicContestPostingRepository);

        assertThatThrownBy(() -> useCase.execute(ContestStatus.OPEN, "publishedAt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported orderBy: publishedAt");
    }
}
