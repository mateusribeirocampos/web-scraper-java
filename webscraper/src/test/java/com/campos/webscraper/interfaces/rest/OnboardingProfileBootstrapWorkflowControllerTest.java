package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.onboarding.BootstrapOnboardingProfileWorkflowUseCase;
import com.campos.webscraper.application.onboarding.BootstrapStatus;
import com.campos.webscraper.application.onboarding.BootstrappedOnboardingWorkflowResult;
import com.campos.webscraper.application.onboarding.BootstrappedTargetSite;
import com.campos.webscraper.application.onboarding.TargetSiteOnboardingProfileNotFoundException;
import com.campos.webscraper.application.usecase.BootstrappedCrawlJob;
import com.campos.webscraper.application.usecase.TargetSiteSmokeRunResult;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
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
@DisplayName("OnboardingProfileBootstrapWorkflowController")
class OnboardingProfileBootstrapWorkflowControllerTest {

    @Mock
    private BootstrapOnboardingProfileWorkflowUseCase bootstrapOnboardingProfileWorkflowUseCase;

    @InjectMocks
    private OnboardingProfileBootstrapWorkflowController controller;

    @Test
    @DisplayName("should return created when workflow creates resources without smoke run")
    void shouldReturnCreatedWhenWorkflowCreatesResourcesWithoutSmokeRun() throws Exception {
        when(bootstrapOnboardingProfileWorkflowUseCase.execute("greenhouse_bitso", false))
                .thenReturn(new BootstrappedOnboardingWorkflowResult(
                        "greenhouse_bitso",
                        new BootstrappedTargetSite("greenhouse_bitso", BootstrapStatus.CREATED, persistedSite()),
                        new BootstrappedCrawlJob(BootstrapStatus.CREATED, persistedCrawlJob()),
                        false,
                        null
                ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(post("/api/v1/onboarding-profiles/greenhouse_bitso/bootstrap"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.profileKey").value("greenhouse_bitso"))
                .andExpect(jsonPath("$.targetSiteBootstrapStatus").value("CREATED"))
                .andExpect(jsonPath("$.crawlJobBootstrapStatus").value("CREATED"))
                .andExpect(jsonPath("$.siteId").value(7))
                .andExpect(jsonPath("$.crawlJobId").value(11))
                .andExpect(jsonPath("$.smokeRunRequested").value(false))
                .andExpect(jsonPath("$.smokeRunStatus").doesNotExist());

        verify(bootstrapOnboardingProfileWorkflowUseCase).execute("greenhouse_bitso", false);
    }

    @Test
    @DisplayName("should return smoke run details when requested")
    void shouldReturnSmokeRunDetailsWhenRequested() throws Exception {
        when(bootstrapOnboardingProfileWorkflowUseCase.execute("greenhouse_bitso", true))
                .thenReturn(new BootstrappedOnboardingWorkflowResult(
                        "greenhouse_bitso",
                        new BootstrappedTargetSite("greenhouse_bitso", BootstrapStatus.UPDATED, persistedSite()),
                        new BootstrappedCrawlJob(BootstrapStatus.UPDATED, persistedCrawlJob()),
                        true,
                        new TargetSiteSmokeRunResult(
                                7L,
                                "greenhouse_bitso",
                                101L,
                                BootstrapStatus.UPDATED,
                                "DISPATCHED",
                                CrawlExecutionStatus.SUCCEEDED
                        )
                ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(post("/api/v1/onboarding-profiles/greenhouse_bitso/bootstrap?smokeRun=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.smokeRunRequested").value(true))
                .andExpect(jsonPath("$.smokeRunStatus").value("DISPATCHED"))
                .andExpect(jsonPath("$.smokeRunDispatchStatus").value("SUCCEEDED"))
                .andExpect(jsonPath("$.smokeRunJobId").value(101));
    }

    @Test
    @DisplayName("should return not found when profile key is unknown")
    void shouldReturnNotFoundWhenProfileKeyIsUnknown() throws Exception {
        when(bootstrapOnboardingProfileWorkflowUseCase.execute("unknown", false))
                .thenThrow(new TargetSiteOnboardingProfileNotFoundException("unknown"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(post("/api/v1/onboarding-profiles/unknown/bootstrap"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Target site onboarding profile not found: unknown"));
    }

    private static TargetSiteEntity persistedSite() {
        return TargetSiteEntity.builder()
                .id(7L)
                .siteCode("greenhouse_bitso")
                .displayName("Bitso Careers via Greenhouse")
                .baseUrl("https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.PENDING_REVIEW)
                .selectorBundleVersion("n/a")
                .enabled(false)
                .createdAt(Instant.parse("2026-03-24T18:00:00Z"))
                .updatedAt(Instant.parse("2026-03-24T18:30:00Z"))
                .build();
    }

    private static CrawlJobEntity persistedCrawlJob() {
        return CrawlJobEntity.builder()
                .id(11L)
                .targetSite(TargetSiteEntity.builder()
                        .id(7L)
                        .siteCode("greenhouse_bitso")
                        .jobCategory(JobCategory.PRIVATE_SECTOR)
                        .build())
                .scheduledAt(Instant.parse("2026-03-24T20:00:00Z"))
                .schedulerManaged(true)
                .createdAt(Instant.parse("2026-03-24T20:00:00Z"))
                .build();
    }
}
