package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.usecase.ListJobPostingsUseCase;
import com.campos.webscraper.domain.enums.SeniorityLevel;
import com.campos.webscraper.domain.model.JobPostingEntity;
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
 * HTTP tests for private-sector listing endpoint.
 *
 * TDD RED: written before the controller exists.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("JobPostingQueryController")
class JobPostingQueryControllerTest {

    @Mock
    private ListJobPostingsUseCase listJobPostingsUseCase;

    @InjectMocks
    private JobPostingQueryController controller;

    @Test
    @DisplayName("should return job postings filtered by since date and seniority")
    void shouldReturnJobPostingsFilteredBySinceDateAndSeniority() throws Exception {
        when(listJobPostingsUseCase.execute(LocalDate.of(2026, 3, 1), SeniorityLevel.JUNIOR))
                .thenReturn(List.of(
                        JobPostingEntity.builder()
                                .id(1L)
                                .title("Java Backend Developer")
                                .company("Acme")
                                .canonicalUrl("https://example.com/jobs/1")
                                .publishedAt(LocalDate.of(2026, 3, 10))
                                .build()
                ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(get("/api/v1/job-postings")
                        .param("since", "2026-03-01")
                        .param("category", "PRIVATE_SECTOR")
                        .param("seniority", "JUNIOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Java Backend Developer"))
                .andExpect(jsonPath("$[0].company").value("Acme"))
                .andExpect(jsonPath("$[0].publishedAt").value("2026-03-10"));

        verify(listJobPostingsUseCase).execute(LocalDate.of(2026, 3, 1), SeniorityLevel.JUNIOR);
    }

    @Test
    @DisplayName("should default to recent postings when since is not provided")
    void shouldDefaultToRecentPostingsWhenSinceIsNotProvided() throws Exception {
        LocalDate expectedSince = LocalDate.now().minusDays(60);

        when(listJobPostingsUseCase.execute(expectedSince, null))
                .thenReturn(List.of(
                        JobPostingEntity.builder()
                                .id(2L)
                                .title("Backend Engineer")
                                .company("Beta")
                                .canonicalUrl("https://example.com/jobs/2")
                                .publishedAt(LocalDate.of(2026, 3, 12))
                                .build()
                ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(get("/api/v1/job-postings")
                        .param("category", "PRIVATE_SECTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].title").value("Backend Engineer"));

        verify(listJobPostingsUseCase).execute(expectedSince, null);
    }

    @Test
    @DisplayName("should reject non-positive daysBack")
    void shouldRejectNonPositiveDaysBack() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(get("/api/v1/job-postings")
                        .param("category", "PRIVATE_SECTOR")
                        .param("daysBack", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("daysBack must be greater than zero"));
    }
}
