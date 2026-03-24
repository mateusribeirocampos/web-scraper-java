package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.usecase.BootstrapCrawlJobFromTargetSiteUseCase;
import com.campos.webscraper.application.usecase.BootstrappedCrawlJob;
import com.campos.webscraper.application.onboarding.BootstrapStatus;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
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

import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("TargetSiteCrawlJobBootstrapController")
class TargetSiteCrawlJobBootstrapControllerTest {

    @Mock
    private BootstrapCrawlJobFromTargetSiteUseCase bootstrapCrawlJobFromTargetSiteUseCase;

    @InjectMocks
    private TargetSiteCrawlJobBootstrapController controller;

    @Test
    @DisplayName("should return created when bootstrapping a new crawl job")
    void shouldReturnCreatedWhenBootstrappingANewCrawlJob() throws Exception {
        when(bootstrapCrawlJobFromTargetSiteUseCase.execute(7L)).thenReturn(
                new BootstrappedCrawlJob(BootstrapStatus.CREATED, persistedJob(101L, 7L, "greenhouse_bitso"))
        );

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(post("/api/v1/target-sites/7/bootstrap-crawl-job"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bootstrapStatus").value("CREATED"))
                .andExpect(jsonPath("$.jobId").value(101))
                .andExpect(jsonPath("$.siteId").value(7))
                .andExpect(jsonPath("$.siteCode").value("greenhouse_bitso"))
                .andExpect(jsonPath("$.schedulerManaged").value(true));

        verify(bootstrapCrawlJobFromTargetSiteUseCase).execute(7L);
    }

    @Test
    @DisplayName("should return ok when bootstrapping updates an existing crawl job")
    void shouldReturnOkWhenBootstrappingUpdatesAnExistingCrawlJob() throws Exception {
        when(bootstrapCrawlJobFromTargetSiteUseCase.execute(7L)).thenReturn(
                new BootstrappedCrawlJob(BootstrapStatus.UPDATED, persistedJob(11L, 7L, "greenhouse_bitso"))
        );

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(post("/api/v1/target-sites/7/bootstrap-crawl-job"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bootstrapStatus").value("UPDATED"))
                .andExpect(jsonPath("$.jobId").value(11));
    }

    @Test
    @DisplayName("should return not found when target site does not exist")
    void shouldReturnNotFoundWhenTargetSiteDoesNotExist() throws Exception {
        when(bootstrapCrawlJobFromTargetSiteUseCase.execute(999L))
                .thenThrow(new TargetSiteNotFoundException(999L));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(post("/api/v1/target-sites/999/bootstrap-crawl-job"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Target site not found: 999"));
    }

    private static CrawlJobEntity persistedJob(Long jobId, Long siteId, String siteCode) {
        return CrawlJobEntity.builder()
                .id(jobId)
                .targetSite(TargetSiteEntity.builder()
                        .id(siteId)
                        .siteCode(siteCode)
                        .jobCategory(JobCategory.PRIVATE_SECTOR)
                        .build())
                .scheduledAt(Instant.parse("2026-03-24T20:00:00Z"))
                .schedulerManaged(true)
                .createdAt(Instant.parse("2026-03-24T20:00:00Z"))
                .build();
    }
}
