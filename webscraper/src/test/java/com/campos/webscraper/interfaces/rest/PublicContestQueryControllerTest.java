package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.usecase.ListPublicContestsUseCase;
import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP tests for public contest listing endpoint.
 *
 * TDD RED: written before the controller exists.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("PublicContestQueryController")
class PublicContestQueryControllerTest {

    @Mock
    private ListPublicContestsUseCase listPublicContestsUseCase;

    @InjectMocks
    private PublicContestQueryController controller;

    @Test
    @DisplayName("should return contests filtered by status and ordered by registration end date")
    void shouldReturnContestsFilteredByStatusAndOrderedByRegistrationEndDate() throws Exception {
        when(listPublicContestsUseCase.execute(ContestStatus.OPEN, "registrationEndDate"))
                .thenReturn(List.of(
                        PublicContestPostingEntity.builder()
                                .id(1L)
                                .contestName("Concurso TI 2026")
                                .organizer("SERPRO")
                                .positionTitle("Analista de TI")
                                .canonicalUrl("https://example.com/contests/1")
                                .publishedAt(LocalDate.of(2026, 3, 10))
                                .registrationEndDate(LocalDate.of(2026, 3, 25))
                                .build()
                ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(get("/api/v1/public-contests")
                        .param("status", "OPEN")
                        .param("orderBy", "registrationEndDate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].contestName").value("Concurso TI 2026"))
                .andExpect(jsonPath("$[0].registrationEndDate").value("2026-03-25"));

        verify(listPublicContestsUseCase).execute(ContestStatus.OPEN, "registrationEndDate");
    }
}
