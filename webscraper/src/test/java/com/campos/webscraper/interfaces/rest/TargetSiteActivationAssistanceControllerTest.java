package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.onboarding.ActivationAssistanceSource;
import com.campos.webscraper.application.onboarding.GetTargetSiteActivationAssistanceUseCase;
import com.campos.webscraper.application.onboarding.OnboardingLegalCategory;
import com.campos.webscraper.application.onboarding.SiteOnboardingChecklist;
import com.campos.webscraper.application.onboarding.TargetSiteActivationAssistance;
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

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("TargetSiteActivationAssistanceController")
class TargetSiteActivationAssistanceControllerTest {

    @Mock
    private GetTargetSiteActivationAssistanceUseCase getTargetSiteActivationAssistanceUseCase;

    @InjectMocks
    private TargetSiteActivationAssistanceController controller;

    @Test
    @DisplayName("should return activation assistance draft for target site")
    void shouldReturnActivationAssistanceDraftForTargetSite() throws Exception {
        when(getTargetSiteActivationAssistanceUseCase.execute(7L))
                .thenReturn(new TargetSiteActivationAssistance(
                        7L,
                        "greenhouse_bitso",
                        "greenhouse_bitso",
                        ActivationAssistanceSource.CURATED_PROFILE,
                        new SiteOnboardingChecklist(
                                "https://boards.greenhouse.io/robots.txt",
                                true,
                                true,
                                "",
                                true,
                                true,
                                true,
                                "https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true",
                                true,
                                "Private-sector Java/backend source.",
                                "Greenhouse public board API: 60 rpm conservative profile",
                                OnboardingLegalCategory.API_OFICIAL,
                                "platform-team@local",
                                "PUBLIC_ANONYMOUS",
                                "Curated greenhouse evidence."
                        ),
                        true,
                        List.of(),
                        List.of("Checklist prefilled from curated onboarding profile: greenhouse_bitso.")
                ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(get("/api/v1/target-sites/7/activation-assistance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siteId").value(7))
                .andExpect(jsonPath("$.siteCode").value("greenhouse_bitso"))
                .andExpect(jsonPath("$.profileKey").value("greenhouse_bitso"))
                .andExpect(jsonPath("$.assistanceSource").value("CURATED_PROFILE"))
                .andExpect(jsonPath("$.productionReadyIfActivatedNow").value(true))
                .andExpect(jsonPath("$.robotsTxtUrl").value("https://boards.greenhouse.io/robots.txt"))
                .andExpect(jsonPath("$.officialApiEndpointUrl")
                        .value("https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true"));

        verify(getTargetSiteActivationAssistanceUseCase).execute(7L);
    }

    @Test
    @DisplayName("should return not found when target site does not exist")
    void shouldReturnNotFoundWhenTargetSiteDoesNotExist() throws Exception {
        when(getTargetSiteActivationAssistanceUseCase.execute(999L))
                .thenThrow(new TargetSiteNotFoundException(999L));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(get("/api/v1/target-sites/999/activation-assistance"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Target site not found: 999"));
    }
}
