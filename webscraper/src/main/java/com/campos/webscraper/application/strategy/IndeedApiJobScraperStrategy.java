package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.normalizer.IndeedJobNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.IndeedApiClient;
import com.campos.webscraper.interfaces.dto.IndeedApiResponse;

import java.util.List;
import java.util.Objects;

/**
 * API-first strategy for Indeed MCP integration.
 */
public class IndeedApiJobScraperStrategy implements JobScraperStrategy<JobPostingEntity> {

    private final IndeedApiClient indeedApiClient;
    private final IndeedJobNormalizer normalizer;

    public IndeedApiJobScraperStrategy(IndeedApiClient indeedApiClient, IndeedJobNormalizer normalizer) {
        this.indeedApiClient = Objects.requireNonNull(indeedApiClient, "indeedApiClient must not be null");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
    }

    @Override
    public boolean supports(TargetSiteEntity targetSite) {
        return "indeed-br".equals(targetSite.getSiteCode())
                && targetSite.getSiteType() == SiteType.TYPE_E
                && targetSite.getExtractionMode() == ExtractionMode.API
                && targetSite.getJobCategory() == JobCategory.PRIVATE_SECTOR
                && targetSite.getLegalStatus() == LegalStatus.APPROVED;
    }

    @Override
    public ScrapeResult<JobPostingEntity> scrape(ScrapeCommand command) {
        IndeedApiResponse response = indeedApiClient.fetchJob(command.targetUrl());
        JobPostingEntity posting = normalizer.normalize(response);
        return ScrapeResult.success(List.of(posting), command.siteCode());
    }
}
