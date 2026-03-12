package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.usecase.ExecuteCrawlJobManuallyUseCase;
import com.campos.webscraper.shared.CrawlJobNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP tests for manual crawl job execution endpoint.
 *
 * TDD RED: written before the controller exists.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("CrawlJobController")
class CrawlJobControllerTest {

    @Mock
    private ExecuteCrawlJobManuallyUseCase executeCrawlJobManuallyUseCase;

    @InjectMocks
    private CrawlJobController controller;

    @Test
    @DisplayName("should return accepted when dispatching an existing crawl job")
    void shouldReturnAcceptedWhenDispatchingAnExistingCrawlJob() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(post("/api/v1/crawl-jobs/42/execute")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(42))
                .andExpect(jsonPath("$.status").value("DISPATCHED"));

        verify(executeCrawlJobManuallyUseCase).execute(42L);
    }

    @Test
    @DisplayName("should return not found when crawl job does not exist")
    void shouldReturnNotFoundWhenCrawlJobDoesNotExist() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        doThrow(new CrawlJobNotFoundException(99L))
                .when(executeCrawlJobManuallyUseCase)
                .execute(99L);

        mockMvc.perform(post("/api/v1/crawl-jobs/99/execute")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Crawl job not found: 99"));
    }
}
