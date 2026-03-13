package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.normalizer.DouContestNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.DouApiClient;
import com.campos.webscraper.interfaces.dto.DouApiItemResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * API-first strategy for the DOU contest integration.
 */
@Component
public class DouApiContestScraperStrategy implements JobScraperStrategy<PublicContestPostingEntity> {

    private final DouApiClient douApiClient;
    private final DouContestNormalizer normalizer;

    public DouApiContestScraperStrategy(DouApiClient douApiClient, DouContestNormalizer normalizer) {
        this.douApiClient = Objects.requireNonNull(douApiClient, "douApiClient must not be null");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
    }

    @Override
    public boolean supports(TargetSiteEntity targetSite) {
        return "dou-api".equals(targetSite.getSiteCode())
                && targetSite.getSiteType() == SiteType.TYPE_E
                && targetSite.getExtractionMode() == ExtractionMode.API
                && targetSite.getJobCategory() == JobCategory.PUBLIC_CONTEST
                && targetSite.getLegalStatus() == LegalStatus.APPROVED;
    }

    @Override
    public ScrapeResult<PublicContestPostingEntity> scrape(ScrapeCommand command) {
        List<DouApiItemResponse> response = douApiClient.searchRelevantNotices(command.targetUrl());
        List<PublicContestPostingEntity> postings = response.stream()
                .map(normalizer::normalize)
                .toList();
        return ScrapeResult.success(postings, command.siteCode());
    }
}
