package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.onboarding.BootstrapOnboardingProfileWorkflowUseCase;
import com.campos.webscraper.application.onboarding.BootstrapStatus;
import com.campos.webscraper.application.onboarding.BootstrappedOnboardingWorkflowResult;
import com.campos.webscraper.interfaces.dto.OnboardingProfileBootstrapWorkflowResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/onboarding-profiles")
public class OnboardingProfileBootstrapWorkflowController {

    private final BootstrapOnboardingProfileWorkflowUseCase bootstrapOnboardingProfileWorkflowUseCase;

    public OnboardingProfileBootstrapWorkflowController(
            BootstrapOnboardingProfileWorkflowUseCase bootstrapOnboardingProfileWorkflowUseCase
    ) {
        this.bootstrapOnboardingProfileWorkflowUseCase = Objects.requireNonNull(
                bootstrapOnboardingProfileWorkflowUseCase,
                "bootstrapOnboardingProfileWorkflowUseCase must not be null"
        );
    }

    @PostMapping("/{profileKey}/bootstrap")
    public ResponseEntity<OnboardingProfileBootstrapWorkflowResponse> bootstrap(
            @PathVariable String profileKey,
            @RequestParam(defaultValue = "false") boolean smokeRun
    ) {
        BootstrappedOnboardingWorkflowResult result =
                bootstrapOnboardingProfileWorkflowUseCase.execute(profileKey, smokeRun);
        HttpStatus status = isCreated(result) ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(new OnboardingProfileBootstrapWorkflowResponse(
                result.profileKey(),
                result.targetSite().bootstrapStatus().name(),
                result.targetSite().targetSite().getId(),
                result.targetSite().targetSite().getSiteCode(),
                result.targetSite().targetSite().isEnabled(),
                result.targetSite().targetSite().getLegalStatus().name(),
                result.crawlJob().bootstrapStatus().name(),
                result.crawlJob().crawlJob().getId(),
                result.crawlJob().crawlJob().isSchedulerManaged(),
                result.crawlJob().crawlJob().getScheduledAt(),
                result.smokeRunRequested(),
                result.smokeRun() == null ? null : result.smokeRun().smokeRunStatus(),
                result.smokeRun() == null || result.smokeRun().dispatchStatus() == null
                        ? null
                        : result.smokeRun().dispatchStatus().name(),
                result.smokeRun() == null ? null : result.smokeRun().jobId()
        ));
    }

    private static boolean isCreated(BootstrappedOnboardingWorkflowResult result) {
        return result.targetSite().bootstrapStatus() == BootstrapStatus.CREATED
                || result.crawlJob().bootstrapStatus() == BootstrapStatus.CREATED;
    }
}
