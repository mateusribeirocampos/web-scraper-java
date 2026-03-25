package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.usecase.RunTargetSiteSmokeRunUseCase;
import com.campos.webscraper.application.usecase.TargetSiteSmokeRunResult;
import com.campos.webscraper.interfaces.dto.TargetSiteSmokeRunResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/target-sites")
public class TargetSiteSmokeRunController {

    private final RunTargetSiteSmokeRunUseCase runTargetSiteSmokeRunUseCase;

    public TargetSiteSmokeRunController(RunTargetSiteSmokeRunUseCase runTargetSiteSmokeRunUseCase) {
        this.runTargetSiteSmokeRunUseCase = Objects.requireNonNull(
                runTargetSiteSmokeRunUseCase,
                "runTargetSiteSmokeRunUseCase must not be null"
        );
    }

    @PostMapping("/{siteId}/smoke-run")
    public ResponseEntity<TargetSiteSmokeRunResponse> execute(@PathVariable Long siteId) {
        TargetSiteSmokeRunResult result = runTargetSiteSmokeRunUseCase.execute(siteId);
        return ResponseEntity.ok(new TargetSiteSmokeRunResponse(
                result.siteId(),
                result.siteCode(),
                result.jobId(),
                result.bootstrapStatus().name(),
                result.smokeRunStatus(),
                result.dispatchStatus() == null ? null : result.dispatchStatus().name()
        ));
    }
}
