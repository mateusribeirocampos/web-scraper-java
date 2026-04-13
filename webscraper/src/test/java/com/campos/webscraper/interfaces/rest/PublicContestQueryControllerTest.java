package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.usecase.ListPublicContestsUseCase;
import com.campos.webscraper.application.usecase.PublicContestSearchProfileMatcher;
import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.domain.enums.EducationLevel;
import com.campos.webscraper.domain.enums.PublicContestSearchProfile;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("PublicContestQueryController")
class PublicContestQueryControllerTest {

    @Mock
    private ListPublicContestsUseCase listPublicContestsUseCase;

    private PublicContestQueryController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new PublicContestQueryController(
                listPublicContestsUseCase,
                new PublicContestSearchProfileMatcher()
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

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
                                .educationLevel(EducationLevel.SUPERIOR)
                                .canonicalUrl("https://example.com/contests/1")
                                .publishedAt(LocalDate.of(2026, 3, 10))
                                .registrationEndDate(LocalDate.of(2026, 3, 25))
                                .build()
                ));

        mockMvc.perform(get("/api/v1/public-contests")
                        .param("status", "OPEN")
                        .param("orderBy", "registrationEndDate")
                        .param("profile", "UNFILTERED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].contestName").value("Concurso TI 2026"))
                .andExpect(jsonPath("$[0].registrationEndDate").value("2026-03-25"));

        verify(listPublicContestsUseCase).execute(ContestStatus.OPEN, "registrationEndDate");
    }

    @Test
    @DisplayName("should filter contests by TI_DEGREE_AND_ROLE profile by default")
    void shouldFilterContestsByTiDegreeAndRoleProfileByDefault() throws Exception {
        PublicContestPostingEntity itContest = PublicContestPostingEntity.builder()
                .id(1L)
                .contestName("Concurso TI 2026")
                .organizer("SERPRO")
                .positionTitle("Analista de Tecnologia da Informação")
                .educationLevel(EducationLevel.SUPERIOR)
                .canonicalUrl("https://example.com/contests/1")
                .publishedAt(LocalDate.of(2026, 3, 10))
                .registrationEndDate(LocalDate.of(2026, 3, 25))
                .build();

        PublicContestPostingEntity adminContest = PublicContestPostingEntity.builder()
                .id(2L)
                .contestName("Concurso Admin 2026")
                .organizer("SERPRO")
                .positionTitle("Assistente Administrativo")
                .educationLevel(EducationLevel.MEDIO)
                .canonicalUrl("https://example.com/contests/2")
                .publishedAt(LocalDate.of(2026, 3, 10))
                .registrationEndDate(LocalDate.of(2026, 3, 25))
                .build();

        when(listPublicContestsUseCase.execute(ContestStatus.OPEN, "registrationEndDate"))
                .thenReturn(List.of(itContest, adminContest));

        mockMvc.perform(get("/api/v1/public-contests")
                        .param("status", "OPEN")
                        .param("orderBy", "registrationEndDate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].positionTitle").value("Analista de Tecnologia da Informação"));
    }
}
