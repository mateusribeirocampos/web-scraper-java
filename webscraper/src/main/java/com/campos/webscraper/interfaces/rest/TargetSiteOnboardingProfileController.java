package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.onboarding.GetTargetSiteOnboardingProfileUseCase;
import com.campos.webscraper.application.onboarding.ListTargetSiteOnboardingProfilesUseCase;
import com.campos.webscraper.interfaces.dto.TargetSiteOnboardingProfileResponse;
import com.campos.webscraper.interfaces.dto.TargetSiteOnboardingProfileSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * REST endpoints for curated onboarding templates that operators can use before activating a source.
 */
@RestController
@RequestMapping("/api/v1/onboarding-profiles")
public class TargetSiteOnboardingProfileController {

    private final ListTargetSiteOnboardingProfilesUseCase listTargetSiteOnboardingProfilesUseCase;
    private final GetTargetSiteOnboardingProfileUseCase getTargetSiteOnboardingProfileUseCase;

    public TargetSiteOnboardingProfileController(
            ListTargetSiteOnboardingProfilesUseCase listTargetSiteOnboardingProfilesUseCase,
            GetTargetSiteOnboardingProfileUseCase getTargetSiteOnboardingProfileUseCase
    ) {
        this.listTargetSiteOnboardingProfilesUseCase = Objects.requireNonNull(
                listTargetSiteOnboardingProfilesUseCase,
                "listTargetSiteOnboardingProfilesUseCase must not be null"
        );
        this.getTargetSiteOnboardingProfileUseCase = Objects.requireNonNull(
                getTargetSiteOnboardingProfileUseCase,
                "getTargetSiteOnboardingProfileUseCase must not be null"
        );
    }

    @GetMapping
    public ResponseEntity<List<TargetSiteOnboardingProfileSummaryResponse>> list() {
        return ResponseEntity.ok(listTargetSiteOnboardingProfilesUseCase.execute());
    }

    @GetMapping("/{profileKey}")
    public ResponseEntity<TargetSiteOnboardingProfileResponse> get(@PathVariable String profileKey) {
        return ResponseEntity.ok(getTargetSiteOnboardingProfileUseCase.execute(profileKey));
    }
}
