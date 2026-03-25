package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.onboarding.GetTargetSiteActivationAssistanceUseCase;
import com.campos.webscraper.application.onboarding.TargetSiteActivationAssistance;
import com.campos.webscraper.application.onboarding.SiteOnboardingChecklist;
import com.campos.webscraper.interfaces.dto.TargetSiteActivationAssistanceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/target-sites")
public class TargetSiteActivationAssistanceController {

    private final GetTargetSiteActivationAssistanceUseCase getTargetSiteActivationAssistanceUseCase;

    public TargetSiteActivationAssistanceController(
            GetTargetSiteActivationAssistanceUseCase getTargetSiteActivationAssistanceUseCase
    ) {
        this.getTargetSiteActivationAssistanceUseCase = Objects.requireNonNull(
                getTargetSiteActivationAssistanceUseCase,
                "getTargetSiteActivationAssistanceUseCase must not be null"
        );
    }

    @GetMapping("/{siteId}/activation-assistance")
    public ResponseEntity<TargetSiteActivationAssistanceResponse> getAssistance(@PathVariable Long siteId) {
        TargetSiteActivationAssistance assistance = getTargetSiteActivationAssistanceUseCase.execute(siteId);
        return ResponseEntity.ok(toResponse(assistance));
    }

    private static TargetSiteActivationAssistanceResponse toResponse(TargetSiteActivationAssistance assistance) {
        SiteOnboardingChecklist checklist = assistance.suggestedChecklist();
        return new TargetSiteActivationAssistanceResponse(
                assistance.siteId(),
                assistance.siteCode(),
                assistance.profileKey(),
                assistance.assistanceSource().name(),
                assistance.productionReadyIfActivatedNow(),
                assistance.blockingReasonsIfActivatedNow(),
                assistance.notes(),
                checklist.robotsTxtUrl(),
                checklist.robotsTxtReviewed(),
                checklist.robotsTxtAllowsScraping(),
                checklist.termsOfServiceUrl(),
                checklist.termsReviewed(),
                checklist.termsAllowScraping(),
                checklist.officialApiChecked(),
                checklist.officialApiEndpointUrl(),
                checklist.strategySupportVerified(),
                checklist.businessJustification(),
                checklist.rateLimitProfile(),
                checklist.legalCategory().name(),
                checklist.owner(),
                checklist.authenticationStatus(),
                checklist.discoveryEvidence()
        );
    }
}
