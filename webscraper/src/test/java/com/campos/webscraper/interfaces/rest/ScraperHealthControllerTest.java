package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.usecase.GetScraperHealthSummaryUseCase;
import com.campos.webscraper.interfaces.dto.RecentCrawlExecutionResponse;
import com.campos.webscraper.interfaces.dto.ScraperExecutionStatusCountResponse;
import com.campos.webscraper.interfaces.dto.ScraperHealthSummaryResponse;
import com.campos.webscraper.interfaces.dto.ScraperQueueStatusCountResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("ScraperHealthController")
class ScraperHealthControllerTest {

    @Mock
    private GetScraperHealthSummaryUseCase getScraperHealthSummaryUseCase;

    @InjectMocks
    private ScraperHealthController controller;

    @Test
    @DisplayName("should return scraper health summary")
    void shouldReturnScraperHealthSummary() throws Exception {
        when(getScraperHealthSummaryUseCase.execute()).thenReturn(new ScraperHealthSummaryResponse(
                Instant.parse("2026-03-21T18:00:00Z"),
                List.of(
                        new ScraperExecutionStatusCountResponse("SUCCEEDED", 12),
                        new ScraperExecutionStatusCountResponse("FAILED", 2)
                ),
                List.of(
                        new ScraperQueueStatusCountResponse("API_JOBS", "READY", 3)
                ),
                List.of(
                        new RecentCrawlExecutionResponse(
                                501L,
                                42L,
                                "greenhouse_bitso",
                                "SUCCEEDED",
                                7,
                                Instant.parse("2026-03-21T17:59:30Z"),
                                Instant.parse("2026-03-21T17:59:50Z"),
                                null
                        )
                )
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/v1/scraper/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedAt").value("2026-03-21T18:00:00Z"))
                .andExpect(jsonPath("$.executionCounts[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.executionCounts[0].count").value(12))
                .andExpect(jsonPath("$.queueCounts[0].queueName").value("API_JOBS"))
                .andExpect(jsonPath("$.queueCounts[0].status").value("READY"))
                .andExpect(jsonPath("$.queueCounts[0].count").value(3))
                .andExpect(jsonPath("$.recentExecutions[0].executionId").value(501))
                .andExpect(jsonPath("$.recentExecutions[0].siteCode").value("greenhouse_bitso"))
                .andExpect(jsonPath("$.recentExecutions[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.recentExecutions[0].itemsFound").value(7));

        verify(getScraperHealthSummaryUseCase).execute();
    }
}
