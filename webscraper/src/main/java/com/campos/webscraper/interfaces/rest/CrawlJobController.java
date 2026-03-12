package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.usecase.ExecuteCrawlJobManuallyUseCase;
import com.campos.webscraper.interfaces.dto.CrawlJobExecutionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * REST endpoints for manual crawl job operations.
 */
@RestController
@RequestMapping("/api/v1/crawl-jobs")
public class CrawlJobController {

    private final ExecuteCrawlJobManuallyUseCase executeCrawlJobManuallyUseCase;

    public CrawlJobController(ExecuteCrawlJobManuallyUseCase executeCrawlJobManuallyUseCase) {
        this.executeCrawlJobManuallyUseCase = Objects.requireNonNull(
                executeCrawlJobManuallyUseCase,
                "executeCrawlJobManuallyUseCase must not be null"
        );
    }

    /**
     * Triggers the dispatch of a crawl job on demand.
     */
    @PostMapping("/{jobId}/execute")
    public ResponseEntity<CrawlJobExecutionResponse> execute(@PathVariable Long jobId) {
        executeCrawlJobManuallyUseCase.execute(jobId);
        return ResponseEntity.accepted()
                .body(new CrawlJobExecutionResponse(jobId, "DISPATCHED"));
    }
}
