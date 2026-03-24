package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.onboarding.BootstrapStatus;
import com.campos.webscraper.application.onboarding.BootstrapTargetSiteFromProfileUseCase;
import com.campos.webscraper.application.onboarding.BootstrappedTargetSite;
import com.campos.webscraper.interfaces.dto.TargetSiteBootstrapResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/onboarding-profiles")
public class TargetSiteBootstrapController {

    private final BootstrapTargetSiteFromProfileUseCase bootstrapTargetSiteFromProfileUseCase;

    public TargetSiteBootstrapController(BootstrapTargetSiteFromProfileUseCase bootstrapTargetSiteFromProfileUseCase) {
        this.bootstrapTargetSiteFromProfileUseCase = Objects.requireNonNull(
                bootstrapTargetSiteFromProfileUseCase,
                "bootstrapTargetSiteFromProfileUseCase must not be null"
        );
    }

    @PostMapping("/{profileKey}/bootstrap-target-site")
    public ResponseEntity<TargetSiteBootstrapResponse> bootstrap(@PathVariable String profileKey) {
        BootstrappedTargetSite bootstrapped = bootstrapTargetSiteFromProfileUseCase.execute(profileKey);
        HttpStatus status = bootstrapped.bootstrapStatus() == BootstrapStatus.CREATED ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(toResponse(bootstrapped));
    }

    private static TargetSiteBootstrapResponse toResponse(BootstrappedTargetSite bootstrapped) {
        return new TargetSiteBootstrapResponse(
                bootstrapped.profileKey(),
                bootstrapped.bootstrapStatus().name(),
                bootstrapped.targetSite().getId(),
                bootstrapped.targetSite().getSiteCode(),
                bootstrapped.targetSite().isEnabled(),
                bootstrapped.targetSite().getLegalStatus().name()
        );
    }
}
