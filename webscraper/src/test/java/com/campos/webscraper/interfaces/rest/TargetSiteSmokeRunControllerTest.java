package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.onboarding.BootstrapStatus;
import com.campos.webscraper.application.usecase.RunTargetSiteSmokeRunUseCase;
import com.campos.webscraper.application.usecase.TargetSiteSmokeRunResult;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.shared.TargetSiteNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("TargetSiteSmokeRunController")
class TargetSiteSmokeRunControllerTest {

    @Mock
    private RunTargetSiteSmokeRunUseCase runTargetSiteSmokeRunUseCase;

    @InjectMocks
    private TargetSiteSmokeRunController controller;

    @Test
    @DisplayName("should return smoke run result for target site")
    void shouldReturnSmokeRunResultForTargetSite() throws Exception {
        when(runTargetSiteSmokeRunUseCase.execute(7L))
                .thenReturn(new TargetSiteSmokeRunResult(
                        7L,
                        "greenhouse_bitso",
                        101L,
                        BootstrapStatus.CREATED,
                        "DISPATCHED",
                        CrawlExecutionStatus.SUCCEEDED
                ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(post("/api/v1/target-sites/7/smoke-run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siteId").value(7))
                .andExpect(jsonPath("$.siteCode").value("greenhouse_bitso"))
                .andExpect(jsonPath("$.jobId").value(101))
                .andExpect(jsonPath("$.bootstrapStatus").value("CREATED"))
                .andExpect(jsonPath("$.smokeRunStatus").value("DISPATCHED"))
                .andExpect(jsonPath("$.dispatchStatus").value("SUCCEEDED"));

        verify(runTargetSiteSmokeRunUseCase).execute(7L);
    }

    @Test
    @DisplayName("should return not found when target site does not exist")
    void shouldReturnNotFoundWhenTargetSiteDoesNotExist() throws Exception {
        when(runTargetSiteSmokeRunUseCase.execute(999L))
                .thenThrow(new TargetSiteNotFoundException(999L));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(post("/api/v1/target-sites/999/smoke-run"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Target site not found: 999"));
    }
}
