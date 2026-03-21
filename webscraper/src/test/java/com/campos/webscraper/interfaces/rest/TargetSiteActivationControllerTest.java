package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.usecase.ActivateTargetSiteUseCase;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.shared.TargetSiteActivationBlockedException;
import com.campos.webscraper.shared.TargetSiteNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("TargetSiteActivationController")
class TargetSiteActivationControllerTest {

    @Mock
    private ActivateTargetSiteUseCase activateTargetSiteUseCase;

    @InjectMocks
    private TargetSiteActivationController controller;

    @Test
    @DisplayName("should activate target site when checklist is compliant")
    void shouldActivateTargetSiteWhenChecklistIsCompliant() throws Exception {
        when(activateTargetSiteUseCase.execute(org.mockito.Mockito.eq(7L), org.mockito.Mockito.any()))
                .thenReturn(TargetSiteEntity.builder()
                        .id(7L)
                        .siteCode("greenhouse_bitso")
                        .displayName("Bitso Careers via Greenhouse")
                        .baseUrl("https://boards.greenhouse.io/bitso")
                        .siteType(SiteType.TYPE_E)
                        .extractionMode(ExtractionMode.API)
                        .jobCategory(JobCategory.PRIVATE_SECTOR)
                        .legalStatus(LegalStatus.APPROVED)
                        .selectorBundleVersion("n/a")
                        .enabled(true)
                        .createdAt(Instant.parse("2026-03-21T17:00:00Z"))
                        .updatedAt(Instant.parse("2026-03-21T18:00:00Z"))
                        .build());

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new RestExceptionHandler()).build();

        mockMvc.perform(post("/api/v1/target-sites/7/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "robotsTxtUrl": "https://boards.greenhouse.io/robots.txt",
                                  "robotsTxtReviewed": true,
                                  "robotsTxtAllowsScraping": true,
                                  "termsOfServiceUrl": "",
                                  "termsReviewed": true,
                                  "termsAllowScraping": true,
                                  "officialApiChecked": true,
                                  "officialApiEndpointUrl": "https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true",
                                  "strategySupportVerified": true,
                                  "businessJustification": "Private-sector Java/backend source.",
                                  "rateLimitProfile": "60 rpm conservative",
                                  "legalCategory": "API_OFICIAL",
                                  "owner": "platform-team@local",
                                  "authenticationStatus": "PUBLIC_ANONYMOUS",
                                  "discoveryEvidence": "Greenhouse public API reviewed."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siteId").value(7))
                .andExpect(jsonPath("$.siteCode").value("greenhouse_bitso"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.legalStatus").value("APPROVED"));

        ArgumentCaptor<com.campos.webscraper.interfaces.dto.TargetSiteActivationRequest> captor =
                ArgumentCaptor.forClass(com.campos.webscraper.interfaces.dto.TargetSiteActivationRequest.class);
        verify(activateTargetSiteUseCase).execute(org.mockito.Mockito.eq(7L), captor.capture());
        assertThat(captor.getValue().legalCategory()).isEqualTo("API_OFICIAL");
    }

    @Test
    @DisplayName("should return conflict when activation is blocked by onboarding compliance")
    void shouldReturnConflictWhenActivationIsBlockedByOnboardingCompliance() throws Exception {
        when(activateTargetSiteUseCase.execute(org.mockito.Mockito.eq(7L), org.mockito.Mockito.any()))
                .thenThrow(new TargetSiteActivationBlockedException(7L, List.of("terms of service not reviewed")));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new RestExceptionHandler()).build();

        mockMvc.perform(post("/api/v1/target-sites/7/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "robotsTxtUrl": "https://boards.greenhouse.io/robots.txt",
                                  "robotsTxtReviewed": true,
                                  "robotsTxtAllowsScraping": true,
                                  "termsOfServiceUrl": "",
                                  "termsReviewed": false,
                                  "termsAllowScraping": false,
                                  "officialApiChecked": true,
                                  "officialApiEndpointUrl": "",
                                  "strategySupportVerified": true,
                                  "businessJustification": "Private-sector Java/backend source.",
                                  "rateLimitProfile": "60 rpm conservative",
                                  "legalCategory": "API_OFICIAL",
                                  "owner": "platform-team@local",
                                  "authenticationStatus": "PUBLIC_ANONYMOUS",
                                  "discoveryEvidence": "Missing terms review."
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Target site activation blocked: 7"))
                .andExpect(jsonPath("$.blockingReasons[0]").value("terms of service not reviewed"));
    }

    @Test
    @DisplayName("should return not found when target site id does not exist")
    void shouldReturnNotFoundWhenTargetSiteIdDoesNotExist() throws Exception {
        when(activateTargetSiteUseCase.execute(org.mockito.Mockito.eq(999L), org.mockito.Mockito.any()))
                .thenThrow(new TargetSiteNotFoundException(999L));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new RestExceptionHandler()).build();

        mockMvc.perform(post("/api/v1/target-sites/999/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "robotsTxtUrl": "https://boards.greenhouse.io/robots.txt",
                                  "robotsTxtReviewed": true,
                                  "robotsTxtAllowsScraping": true,
                                  "termsOfServiceUrl": "",
                                  "termsReviewed": true,
                                  "termsAllowScraping": true,
                                  "officialApiChecked": true,
                                  "officialApiEndpointUrl": "https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true",
                                  "strategySupportVerified": true,
                                  "businessJustification": "Private-sector Java/backend source.",
                                  "rateLimitProfile": "60 rpm conservative",
                                  "legalCategory": "API_OFICIAL",
                                  "owner": "platform-team@local",
                                  "authenticationStatus": "PUBLIC_ANONYMOUS",
                                  "discoveryEvidence": "Greenhouse public API reviewed."
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Target site not found: 999"));
    }

    @Test
    @DisplayName("should return bad request when legalCategory is missing")
    void shouldReturnBadRequestWhenLegalCategoryIsMissing() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new RestExceptionHandler()).build();

        mockMvc.perform(post("/api/v1/target-sites/7/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "robotsTxtUrl": "https://boards.greenhouse.io/robots.txt",
                                  "robotsTxtReviewed": true,
                                  "robotsTxtAllowsScraping": true,
                                  "termsOfServiceUrl": "",
                                  "termsReviewed": true,
                                  "termsAllowScraping": true,
                                  "officialApiChecked": true,
                                  "officialApiEndpointUrl": "https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true",
                                  "strategySupportVerified": true,
                                  "businessJustification": "Private-sector Java/backend source.",
                                  "rateLimitProfile": "60 rpm conservative",
                                  "owner": "platform-team@local",
                                  "authenticationStatus": "PUBLIC_ANONYMOUS",
                                  "discoveryEvidence": "Greenhouse public API reviewed."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("legalCategory must not be null or blank"));
    }
}
