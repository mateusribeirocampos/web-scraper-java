package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.onboarding.GetTargetSiteOnboardingProfileUseCase;
import com.campos.webscraper.application.onboarding.ListTargetSiteOnboardingProfilesUseCase;
import com.campos.webscraper.application.onboarding.TargetSiteOnboardingProfileNotFoundException;
import com.campos.webscraper.interfaces.dto.TargetSiteOnboardingProfileResponse;
import com.campos.webscraper.interfaces.dto.TargetSiteOnboardingProfileSummaryResponse;
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
@DisplayName("TargetSiteOnboardingProfileController")
class TargetSiteOnboardingProfileControllerTest {

    @Mock
    private ListTargetSiteOnboardingProfilesUseCase listTargetSiteOnboardingProfilesUseCase;

    @Mock
    private GetTargetSiteOnboardingProfileUseCase getTargetSiteOnboardingProfileUseCase;

    @InjectMocks
    private TargetSiteOnboardingProfileController controller;

    @Test
    @DisplayName("should list onboarding profile summaries")
    void shouldListOnboardingProfileSummaries() throws Exception {
        when(listTargetSiteOnboardingProfilesUseCase.execute()).thenReturn(List.of(
                new TargetSiteOnboardingProfileSummaryResponse(
                        "greenhouse_bitso",
                        "GREENHOUSE",
                        "greenhouse_bitso",
                        "Bitso Careers via Greenhouse",
                        "API_OFICIAL"
                )
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(get("/api/v1/onboarding-profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].profileKey").value("greenhouse_bitso"))
                .andExpect(jsonPath("$[0].sourceFamily").value("GREENHOUSE"))
                .andExpect(jsonPath("$[0].siteCode").value("greenhouse_bitso"))
                .andExpect(jsonPath("$[0].legalCategory").value("API_OFICIAL"));

        verify(listTargetSiteOnboardingProfilesUseCase).execute();
    }

    @Test
    @DisplayName("should return detailed onboarding profile")
    void shouldReturnDetailedOnboardingProfile() throws Exception {
        when(getTargetSiteOnboardingProfileUseCase.execute("greenhouse_bitso")).thenReturn(
                new TargetSiteOnboardingProfileResponse(
                        "greenhouse_bitso",
                        "GREENHOUSE",
                        "bitso",
                        "bitso",
                        "https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true",
                        "greenhouse_bitso",
                        "Bitso Careers via Greenhouse",
                        "https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true",
                        "TYPE_E",
                        "API",
                        "PRIVATE_SECTOR",
                        "PENDING_REVIEW",
                        false,
                        "n/a",
                        "https://boards.greenhouse.io/robots.txt",
                        true,
                        true,
                        "",
                        true,
                        true,
                        true,
                        "https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true",
                        true,
                        "Selecionado para expandir vagas PME com foco atual em Java/backend via ATS publico.",
                        "Greenhouse public board API: 60 rpm conservative profile",
                        "API_OFICIAL",
                        "platform-team@local",
                        "PUBLIC_ANONYMOUS",
                        "Greenhouse Job Board API publica revisada; board token bitso validado em 2026-03-13."
                )
        );

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(get("/api/v1/onboarding-profiles/greenhouse_bitso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileKey").value("greenhouse_bitso"))
                .andExpect(jsonPath("$.sourceFamily").value("GREENHOUSE"))
                .andExpect(jsonPath("$.boardToken").value("bitso"))
                .andExpect(jsonPath("$.sourceIdentifier").value("bitso"))
                .andExpect(jsonPath("$.siteCode").value("greenhouse_bitso"))
                .andExpect(jsonPath("$.legalCategory").value("API_OFICIAL"));

        verify(getTargetSiteOnboardingProfileUseCase).execute("greenhouse_bitso");
    }

    @Test
    @DisplayName("should return board token for Lever onboarding profile too")
    void shouldReturnBoardTokenForLeverOnboardingProfileToo() throws Exception {
        when(getTargetSiteOnboardingProfileUseCase.execute("lever_ciandt")).thenReturn(
                new TargetSiteOnboardingProfileResponse(
                        "lever_ciandt",
                        "LEVER",
                        "ciandt",
                        "ciandt",
                        "https://api.lever.co/v0/postings/ciandt?mode=json",
                        "lever_ciandt",
                        "CI&T Careers via Lever",
                        "https://api.lever.co/v0/postings/ciandt?mode=json",
                        "TYPE_E",
                        "API",
                        "PRIVATE_SECTOR",
                        "PENDING_REVIEW",
                        false,
                        "n/a",
                        "https://jobs.lever.co/robots.txt",
                        true,
                        true,
                        "",
                        true,
                        true,
                        true,
                        "https://api.lever.co/v0/postings/ciandt?mode=json",
                        true,
                        "Primeira trilha privada de Campinas; board publico Lever da CI&T validado para expansao hybrid tech hubs.",
                        "Lever public postings API: 60 rpm conservative profile",
                        "API_OFICIAL",
                        "platform-team@local",
                        "PUBLIC_ANONYMOUS",
                        "Lever public postings endpoint da CI&T revisado em 2026-03-31."
                )
        );

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(get("/api/v1/onboarding-profiles/lever_ciandt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileKey").value("lever_ciandt"))
                .andExpect(jsonPath("$.sourceFamily").value("LEVER"))
                .andExpect(jsonPath("$.boardToken").value("ciandt"))
                .andExpect(jsonPath("$.sourceIdentifier").value("ciandt"));

        verify(getTargetSiteOnboardingProfileUseCase).execute("lever_ciandt");
    }

    @Test
    @DisplayName("should return board token for Workday onboarding profile too")
    void shouldReturnBoardTokenForWorkdayOnboardingProfileToo() throws Exception {
        when(getTargetSiteOnboardingProfileUseCase.execute("airbus_helibras_workday")).thenReturn(
                new TargetSiteOnboardingProfileResponse(
                        "airbus_helibras_workday",
                        "WORKDAY",
                        "Airbus",
                        "Airbus",
                        "https://ag.wd3.myworkdayjobs.com/wday/cxs/ag/Airbus/jobs",
                        "airbus_helibras_workday",
                        "Airbus / Helibras Careers via Workday",
                        "https://ag.wd3.myworkdayjobs.com/wday/cxs/ag/Airbus/jobs",
                        "TYPE_E",
                        "API",
                        "PRIVATE_SECTOR",
                        "APPROVED",
                        true,
                        "n/a",
                        "https://ag.wd3.myworkdayjobs.com/robots.txt",
                        true,
                        true,
                        "",
                        true,
                        true,
                        true,
                        "https://ag.wd3.myworkdayjobs.com/wday/cxs/ag/Airbus/jobs",
                        true,
                        "Trilha privada de Itajubá mapeada no board oficial da Airbus com vagas da Helibras na cidade.",
                        "Workday public jobs API: filtered POST with conservative pagination",
                        "API_OFICIAL",
                        "platform-team@local",
                        "PUBLIC_ANONYMOUS",
                        "Board Workday Airbus revisado em 2026-04-08; robots do domínio wd3 permitem a trilha /Airbus/, a página oficial de careers da Airbus aponta para o board e a revisão legal foi fechada sem restrição explícita específica para a API pública de vagas."
                )
        );

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(get("/api/v1/onboarding-profiles/airbus_helibras_workday"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileKey").value("airbus_helibras_workday"))
                .andExpect(jsonPath("$.sourceFamily").value("WORKDAY"))
                .andExpect(jsonPath("$.boardToken").value("Airbus"))
                .andExpect(jsonPath("$.sourceIdentifier").value("Airbus"));

        verify(getTargetSiteOnboardingProfileUseCase).execute("airbus_helibras_workday");
    }

    @Test
    @DisplayName("should return not found for unknown onboarding profile")
    void shouldReturnNotFoundForUnknownOnboardingProfile() throws Exception {
        when(getTargetSiteOnboardingProfileUseCase.execute("unknown"))
                .thenThrow(new TargetSiteOnboardingProfileNotFoundException("unknown"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        mockMvc.perform(get("/api/v1/onboarding-profiles/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Target site onboarding profile not found: unknown"));
    }
}
