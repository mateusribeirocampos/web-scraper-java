package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.onboarding.BootstrapStatus;
import com.campos.webscraper.application.onboarding.BootstrapTargetSiteFromProfileUseCase;
import com.campos.webscraper.application.onboarding.BootstrappedTargetSite;
import com.campos.webscraper.application.onboarding.TargetSiteOnboardingProfileNotFoundException;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
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
@DisplayName("TargetSiteBootstrapController")
class TargetSiteBootstrapControllerTest {

    @Mock
    private BootstrapTargetSiteFromProfileUseCase bootstrapTargetSiteFromProfileUseCase;

    @InjectMocks
    private TargetSiteBootstrapController controller;

    @Test
    @DisplayName("should return created when bootstrapping a new target site from profile")
    void shouldReturnCreatedWhenBootstrappingANewTargetSiteFromProfile() throws Exception {
        when(bootstrapTargetSiteFromProfileUseCase.execute("greenhouse_bitso")).thenReturn(
                new BootstrappedTargetSite(
                        "greenhouse_bitso",
                        BootstrapStatus.CREATED,
                        persistedSite(42L, false, LegalStatus.PENDING_REVIEW)
                )
        );

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(post("/api/v1/onboarding-profiles/greenhouse_bitso/bootstrap-target-site"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.profileKey").value("greenhouse_bitso"))
                .andExpect(jsonPath("$.bootstrapStatus").value("CREATED"))
                .andExpect(jsonPath("$.siteId").value(42))
                .andExpect(jsonPath("$.siteCode").value("greenhouse_bitso"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.legalStatus").value("PENDING_REVIEW"));

        verify(bootstrapTargetSiteFromProfileUseCase).execute("greenhouse_bitso");
    }

    @Test
    @DisplayName("should return ok when bootstrapping updates an existing target site")
    void shouldReturnOkWhenBootstrappingUpdatesAnExistingTargetSite() throws Exception {
        when(bootstrapTargetSiteFromProfileUseCase.execute("greenhouse_bitso")).thenReturn(
                new BootstrappedTargetSite(
                        "greenhouse_bitso",
                        BootstrapStatus.UPDATED,
                        persistedSite(7L, true, LegalStatus.APPROVED)
                )
        );

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(post("/api/v1/onboarding-profiles/greenhouse_bitso/bootstrap-target-site"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bootstrapStatus").value("UPDATED"))
                .andExpect(jsonPath("$.siteId").value(7))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.legalStatus").value("APPROVED"));
    }

    @Test
    @DisplayName("should return not found when onboarding profile key is unknown")
    void shouldReturnNotFoundWhenOnboardingProfileKeyIsUnknown() throws Exception {
        when(bootstrapTargetSiteFromProfileUseCase.execute("unknown"))
                .thenThrow(new TargetSiteOnboardingProfileNotFoundException("unknown"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(post("/api/v1/onboarding-profiles/unknown/bootstrap-target-site"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Target site onboarding profile not found: unknown"));
    }

    private static TargetSiteEntity persistedSite(Long id, boolean enabled, LegalStatus legalStatus) {
        return TargetSiteEntity.builder()
                .id(id)
                .siteCode("greenhouse_bitso")
                .displayName("Bitso Careers via Greenhouse")
                .baseUrl("https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(legalStatus)
                .selectorBundleVersion("n/a")
                .enabled(enabled)
                .createdAt(Instant.parse("2026-03-24T18:00:00Z"))
                .updatedAt(Instant.parse("2026-03-24T18:30:00Z"))
                .build();
    }
}
