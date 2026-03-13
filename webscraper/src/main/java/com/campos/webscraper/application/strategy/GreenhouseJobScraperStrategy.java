package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.normalizer.GreenhouseJobNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.GreenhouseJobBoardClient;
import com.campos.webscraper.interfaces.dto.GreenhouseJobBoardItemResponse;

import java.util.List;
import java.util.Objects;

/**
 * API-first strategy for Greenhouse public job boards.
 */
public class GreenhouseJobScraperStrategy implements JobScraperStrategy<JobPostingEntity> {

    private final GreenhouseJobBoardClient greenhouseJobBoardClient;
    private final GreenhouseJobNormalizer normalizer;

    public GreenhouseJobScraperStrategy(
            GreenhouseJobBoardClient greenhouseJobBoardClient,
            GreenhouseJobNormalizer normalizer
    ) {
        this.greenhouseJobBoardClient = Objects.requireNonNull(
                greenhouseJobBoardClient,
                "greenhouseJobBoardClient must not be null"
        );
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
    }

    @Override
    public boolean supports(TargetSiteEntity targetSite) {
        return "greenhouse_bitso".equals(targetSite.getSiteCode())
                && targetSite.getSiteType() == SiteType.TYPE_E
                && targetSite.getExtractionMode() == ExtractionMode.API
                && targetSite.getJobCategory() == JobCategory.PRIVATE_SECTOR
                && targetSite.getLegalStatus() == LegalStatus.APPROVED;
    }

    @Override
    public ScrapeResult<JobPostingEntity> scrape(ScrapeCommand command) {
        List<GreenhouseJobBoardItemResponse> response = greenhouseJobBoardClient.fetchPublishedJobs(command.targetUrl());
        List<JobPostingEntity> postings = response.stream()
                .map(normalizer::normalize)
                .toList();
        return ScrapeResult.success(postings, command.siteCode());
    }
}
