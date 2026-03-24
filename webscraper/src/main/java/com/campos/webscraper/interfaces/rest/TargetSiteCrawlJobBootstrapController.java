package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.onboarding.BootstrapStatus;
import com.campos.webscraper.application.usecase.BootstrapCrawlJobFromTargetSiteUseCase;
import com.campos.webscraper.application.usecase.BootstrappedCrawlJob;
import com.campos.webscraper.interfaces.dto.TargetSiteCrawlJobBootstrapResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/target-sites")
public class TargetSiteCrawlJobBootstrapController {

    private final BootstrapCrawlJobFromTargetSiteUseCase bootstrapCrawlJobFromTargetSiteUseCase;

    public TargetSiteCrawlJobBootstrapController(
            BootstrapCrawlJobFromTargetSiteUseCase bootstrapCrawlJobFromTargetSiteUseCase
    ) {
        this.bootstrapCrawlJobFromTargetSiteUseCase = Objects.requireNonNull(
                bootstrapCrawlJobFromTargetSiteUseCase,
                "bootstrapCrawlJobFromTargetSiteUseCase must not be null"
        );
    }

    @PostMapping("/{siteId}/bootstrap-crawl-job")
    public ResponseEntity<TargetSiteCrawlJobBootstrapResponse> bootstrap(@PathVariable Long siteId) {
        BootstrappedCrawlJob bootstrapped = bootstrapCrawlJobFromTargetSiteUseCase.execute(siteId);
        HttpStatus status = bootstrapped.bootstrapStatus() == BootstrapStatus.CREATED ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(new TargetSiteCrawlJobBootstrapResponse(
                bootstrapped.bootstrapStatus().name(),
                bootstrapped.crawlJob().getId(),
                bootstrapped.crawlJob().getTargetSite().getId(),
                bootstrapped.crawlJob().getTargetSite().getSiteCode(),
                bootstrapped.crawlJob().isSchedulerManaged(),
                bootstrapped.crawlJob().getScheduledAt()
        ));
    }
}
