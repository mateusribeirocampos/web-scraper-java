package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.onboarding.BootstrapStatus;
import com.campos.webscraper.application.onboarding.BootstrappedOnboardingWorkflowResult;
import com.campos.webscraper.application.onboarding.BootstrappedTargetSite;
import com.campos.webscraper.application.onboarding.OnboardingOperationalCheckExecutionSummary;
import com.campos.webscraper.application.onboarding.OnboardingOperationalCheckResult;
import com.campos.webscraper.application.onboarding.RunOnboardingOperationalCheckUseCase;
import com.campos.webscraper.application.onboarding.TargetSiteOnboardingProfileNotFoundException;
import com.campos.webscraper.application.usecase.BootstrappedCrawlJob;
import com.campos.webscraper.application.usecase.TargetSiteSmokeRunResult;
import com.campos.webscraper.domain.enums.CrawlExecutionStatus;
import com.campos.webscraper.domain.enums.DedupStatus;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.JobContractType;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.JobPostingEntity;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("OnboardingOperationalCheckController")
class OnboardingOperationalCheckControllerTest {

    @Mock
    private RunOnboardingOperationalCheckUseCase runOnboardingOperationalCheckUseCase;

    @InjectMocks
    private OnboardingOperationalCheckController controller;

    @Test
    @DisplayName("should return created operational summary when workflow creates resources")
    void shouldReturnCreatedOperationalSummaryWhenWorkflowCreatesResources() throws Exception {
        when(runOnboardingOperationalCheckUseCase.execute("greenhouse_bitso", true, 60))
                .thenReturn(result(BootstrapStatus.CREATED, BootstrapStatus.CREATED));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(post("/api/v1/onboarding-profiles/greenhouse_bitso/operational-check"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.profileKey").value("greenhouse_bitso"))
                .andExpect(jsonPath("$.targetSiteBootstrapStatus").value("CREATED"))
                .andExpect(jsonPath("$.crawlJobBootstrapStatus").value("CREATED"))
                .andExpect(jsonPath("$.smokeRunRequested").value(true))
                .andExpect(jsonPath("$.executionSummary.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.recentPostingsCount").value(1))
                .andExpect(jsonPath("$.recentPostingsSample[0].title").value("Java Backend Developer"));

        verify(runOnboardingOperationalCheckUseCase).execute("greenhouse_bitso", true, 60);
    }

    @Test
    @DisplayName("should return not found when profile key is unknown")
    void shouldReturnNotFoundWhenProfileKeyIsUnknown() throws Exception {
        when(runOnboardingOperationalCheckUseCase.execute("unknown", true, 60))
                .thenThrow(new TargetSiteOnboardingProfileNotFoundException("unknown"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(post("/api/v1/onboarding-profiles/unknown/operational-check"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Target site onboarding profile not found: unknown"));
    }

    @Test
    @DisplayName("should return bad request when days back is not positive")
    void shouldReturnBadRequestWhenDaysBackIsNotPositive() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(post("/api/v1/onboarding-profiles/greenhouse_bitso/operational-check?daysBack=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("daysBack must be greater than zero"));
    }

    private static OnboardingOperationalCheckResult result(BootstrapStatus siteStatus, BootstrapStatus crawlStatus) {
        TargetSiteEntity site = TargetSiteEntity.builder()
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
                .createdAt(Instant.parse("2026-03-25T11:00:00Z"))
                .updatedAt(Instant.parse("2026-03-25T11:05:00Z"))
                .build();
        CrawlJobEntity crawlJob = CrawlJobEntity.builder()
                .id(11L)
                .targetSite(site)
                .scheduledAt(Instant.parse("2026-03-25T12:01:00Z"))
                .schedulerManaged(true)
                .createdAt(Instant.parse("2026-03-25T11:00:00Z"))
                .build();
        return new OnboardingOperationalCheckResult(
                "greenhouse_bitso",
                new BootstrappedOnboardingWorkflowResult(
                        "greenhouse_bitso",
                        new BootstrappedTargetSite("greenhouse_bitso", siteStatus, site),
                        new BootstrappedCrawlJob(crawlStatus, crawlJob),
                        true,
                        new TargetSiteSmokeRunResult(
                                7L,
                                "greenhouse_bitso",
                                101L,
                                BootstrapStatus.UPDATED,
                                "DISPATCHED",
                                CrawlExecutionStatus.SUCCEEDED
                        )
                ),
                new OnboardingOperationalCheckExecutionSummary(
                        101L,
                        501L,
                        "SUCCEEDED",
                        12,
                        Instant.parse("2026-03-25T12:00:00Z"),
                        Instant.parse("2026-03-25T12:00:10Z")
                ),
                1,
                List.of(JobPostingEntity.builder()
                        .id(1L)
                        .targetSite(site)
                        .crawlExecution(CrawlExecutionEntity.builder()
                                .id(501L)
                                .crawlJob(crawlJob)
                                .status(CrawlExecutionStatus.SUCCEEDED)
                                .createdAt(Instant.parse("2026-03-25T12:00:10Z"))
                                .build())
                        .externalId("job-1")
                        .canonicalUrl("https://example.org/jobs/1")
                        .title("Java Backend Developer")
                        .company("Example Co")
                        .location("Remote")
                        .remote(true)
                        .contractType(JobContractType.CLT)
                        .description("desc")
                        .techStackTags("Java,Spring")
                        .publishedAt(LocalDate.now().minusDays(1))
                        .fingerprintHash("fingerprint-1")
                        .dedupStatus(DedupStatus.NEW)
                        .createdAt(Instant.parse("2026-03-25T12:00:10Z"))
                        .build())
        );
    }
}
