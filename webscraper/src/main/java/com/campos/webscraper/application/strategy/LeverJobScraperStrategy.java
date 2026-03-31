package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.normalizer.LeverJobNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.LeverPostingsClient;
import com.campos.webscraper.interfaces.dto.LeverPostingResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * API-first strategy for Lever public job boards.
 */
@Component
public class LeverJobScraperStrategy implements JobScraperStrategy<JobPostingEntity> {

    private final LeverPostingsClient leverPostingsClient;
    private final LeverJobNormalizer normalizer;

    public LeverJobScraperStrategy(
            LeverPostingsClient leverPostingsClient,
            LeverJobNormalizer normalizer
    ) {
        this.leverPostingsClient = Objects.requireNonNull(leverPostingsClient, "leverPostingsClient must not be null");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
    }

    @Override
    public boolean supports(TargetSiteEntity targetSite) {
        return targetSite.getBaseUrl() != null
                && targetSite.getBaseUrl().contains("api.lever.co")
                && targetSite.getSiteType() == SiteType.TYPE_E
                && targetSite.getExtractionMode() == ExtractionMode.API
                && targetSite.getJobCategory() == JobCategory.PRIVATE_SECTOR
                && targetSite.getLegalStatus() == LegalStatus.APPROVED;
    }

    @Override
    public ScrapeResult<JobPostingEntity> scrape(ScrapeCommand command) {
        try {
            List<LeverPostingResponse> response = leverPostingsClient.fetchPublishedJobs(command.targetUrl());
            List<JobPostingEntity> postings = response.stream()
                    .map(normalizer::normalize)
                    .toList();
            return ScrapeResult.success(postings, command.siteCode());
        } catch (RuntimeException exception) {
            return ScrapeResult.failure(command.siteCode(), exception.getMessage());
        }
    }
}
