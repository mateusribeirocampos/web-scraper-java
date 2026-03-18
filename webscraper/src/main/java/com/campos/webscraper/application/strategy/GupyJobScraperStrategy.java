package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.normalizer.GupyJobNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.GupyJobBoardClient;
import com.campos.webscraper.interfaces.dto.GupyJobListingResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * API-first strategy for the Gupy Portal public jobs API.
 *
 * <p>Matches any target site whose {@code base_url} points to
 * {@code portal.api.gupy.io} and whose {@code site_code} starts with {@code gupy_}.
 */
@Component
public class GupyJobScraperStrategy implements JobScraperStrategy<JobPostingEntity> {

    private final GupyJobBoardClient gupyJobBoardClient;
    private final GupyJobNormalizer normalizer;

    public GupyJobScraperStrategy(GupyJobBoardClient gupyJobBoardClient, GupyJobNormalizer normalizer) {
        this.gupyJobBoardClient = Objects.requireNonNull(gupyJobBoardClient, "gupyJobBoardClient must not be null");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
    }

    @Override
    public boolean supports(TargetSiteEntity targetSite) {
        return targetSite.getSiteCode() != null
                && targetSite.getSiteCode().startsWith("gupy_")
                && targetSite.getBaseUrl() != null
                && targetSite.getBaseUrl().contains("portal.api.gupy.io")
                && targetSite.getSiteType() == SiteType.TYPE_E
                && targetSite.getExtractionMode() == ExtractionMode.API
                && targetSite.getJobCategory() == JobCategory.PRIVATE_SECTOR
                && targetSite.getLegalStatus() == LegalStatus.APPROVED;
    }

    @Override
    public ScrapeResult<JobPostingEntity> scrape(ScrapeCommand command) {
        List<GupyJobListingResponse> responses = gupyJobBoardClient.fetchAllJobs(command.targetUrl());
        List<JobPostingEntity> postings = responses.stream()
                .map(normalizer::normalize)
                .toList();
        return ScrapeResult.success(postings, command.siteCode());
    }
}
