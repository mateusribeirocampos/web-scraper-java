package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.usecase.ActivateTargetSiteUseCase;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.interfaces.dto.TargetSiteActivationRequest;
import com.campos.webscraper.interfaces.dto.TargetSiteActivationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * REST endpoint that gates target-site activation behind the ADR002 onboarding checklist.
 */
@RestController
@RequestMapping("/api/v1/target-sites")
public class TargetSiteActivationController {

    private final ActivateTargetSiteUseCase activateTargetSiteUseCase;

    public TargetSiteActivationController(ActivateTargetSiteUseCase activateTargetSiteUseCase) {
        this.activateTargetSiteUseCase = Objects.requireNonNull(
                activateTargetSiteUseCase,
                "activateTargetSiteUseCase must not be null"
        );
    }

    @PostMapping("/{siteId}/activation")
    public ResponseEntity<TargetSiteActivationResponse> activate(
            @PathVariable Long siteId,
            @RequestBody TargetSiteActivationRequest request
    ) {
        request.validate();
        TargetSiteEntity activated = activateTargetSiteUseCase.execute(siteId, request);
        return ResponseEntity.ok(new TargetSiteActivationResponse(
                activated.getId(),
                activated.getSiteCode(),
                activated.isEnabled(),
                activated.getLegalStatus().name()
        ));
    }
}
