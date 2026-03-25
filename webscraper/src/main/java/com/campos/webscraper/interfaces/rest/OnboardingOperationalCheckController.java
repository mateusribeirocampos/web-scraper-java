package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.onboarding.OnboardingOperationalCheckExecutionSummary;
import com.campos.webscraper.application.onboarding.OnboardingOperationalCheckResult;
import com.campos.webscraper.application.onboarding.RunOnboardingOperationalCheckUseCase;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.interfaces.dto.JobPostingSummaryResponse;
import com.campos.webscraper.interfaces.dto.OnboardingOperationalCheckExecutionResponse;
import com.campos.webscraper.interfaces.dto.OnboardingOperationalCheckResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/onboarding-profiles")
public class OnboardingOperationalCheckController {

    private final RunOnboardingOperationalCheckUseCase runOnboardingOperationalCheckUseCase;

    public OnboardingOperationalCheckController(
            RunOnboardingOperationalCheckUseCase runOnboardingOperationalCheckUseCase
    ) {
        this.runOnboardingOperationalCheckUseCase = Objects.requireNonNull(
                runOnboardingOperationalCheckUseCase,
                "runOnboardingOperationalCheckUseCase must not be null"
        );
    }

    @PostMapping("/{profileKey}/operational-check")
    public ResponseEntity<OnboardingOperationalCheckResponse> runOperationalCheck(
            @PathVariable String profileKey,
            @RequestParam(defaultValue = "true") boolean smokeRun,
            @RequestParam(defaultValue = "60") int daysBack
    ) {
        if (daysBack <= 0) {
            throw new IllegalArgumentException("daysBack must be greater than zero");
        }

        OnboardingOperationalCheckResult result =
                runOnboardingOperationalCheckUseCase.execute(profileKey, smokeRun, daysBack);
        HttpStatus status = result.workflow().targetSite().bootstrapStatus().name().equals("CREATED")
                || result.workflow().crawlJob().bootstrapStatus().name().equals("CREATED")
                ? HttpStatus.CREATED
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(toResponse(result));
    }

    private static OnboardingOperationalCheckResponse toResponse(OnboardingOperationalCheckResult result) {
        return new OnboardingOperationalCheckResponse(
                result.profileKey(),
                result.workflow().targetSite().bootstrapStatus().name(),
                result.workflow().targetSite().targetSite().getId(),
                result.workflow().targetSite().targetSite().getSiteCode(),
                result.workflow().crawlJob().bootstrapStatus().name(),
                result.workflow().crawlJob().crawlJob().getId(),
                result.workflow().crawlJob().crawlJob().getScheduledAt(),
                result.smokeRunRequested(),
                result.smokeRun() == null ? null : result.smokeRun().smokeRunStatus(),
                result.smokeRun() == null || result.smokeRun().dispatchStatus() == null
                        ? null
                        : result.smokeRun().dispatchStatus().name(),
                result.smokeRun() == null ? null : result.smokeRun().jobId(),
                toExecutionResponse(result.executionSummary()),
                result.recentPostingsCount(),
                result.recentPostingsSample().stream()
                        .map(OnboardingOperationalCheckController::toPostingResponse)
                        .toList()
        );
    }

    private static OnboardingOperationalCheckExecutionResponse toExecutionResponse(
            OnboardingOperationalCheckExecutionSummary executionSummary
    ) {
        if (executionSummary == null) {
            return null;
        }
        return new OnboardingOperationalCheckExecutionResponse(
                executionSummary.crawlJobId(),
                executionSummary.crawlExecutionId(),
                executionSummary.status(),
                executionSummary.itemsFound(),
                executionSummary.startedAt(),
                executionSummary.finishedAt()
        );
    }

    private static JobPostingSummaryResponse toPostingResponse(JobPostingEntity posting) {
        return new JobPostingSummaryResponse(
                posting.getId(),
                posting.getTitle(),
                posting.getCompany(),
                posting.getCanonicalUrl(),
                posting.getPublishedAt()
        );
    }
}
