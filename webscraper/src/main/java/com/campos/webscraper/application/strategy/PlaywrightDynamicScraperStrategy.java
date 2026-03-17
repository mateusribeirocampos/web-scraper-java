package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.normalizer.DynamicJobNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.JobFetcher;
import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.FetchedPage;
import com.campos.webscraper.infrastructure.parser.DynamicJobListingParser;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Browser automation strategy para sites Tipo C (JS-heavy).
 */
@Component
public class PlaywrightDynamicScraperStrategy implements JobScraperStrategy<JobPostingEntity> {

    private final JobFetcher jobFetcher;
    private final DynamicJobListingParser parser;
    private final DynamicJobNormalizer normalizer;

    public PlaywrightDynamicScraperStrategy(
            JobFetcher jobFetcher,
            DynamicJobListingParser parser,
            DynamicJobNormalizer normalizer
    ) {
        this.jobFetcher = Objects.requireNonNull(jobFetcher, "jobFetcher must not be null");
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
    }

    @Override
    public boolean supports(TargetSiteEntity targetSite) {
        return "dynamic-playwright".equals(targetSite.getSiteCode())
                && targetSite.getSiteType() == SiteType.TYPE_C
                && targetSite.getExtractionMode() == ExtractionMode.BROWSER_AUTOMATION
                && targetSite.getJobCategory() == JobCategory.PRIVATE_SECTOR
                && targetSite.getLegalStatus() == LegalStatus.APPROVED;
    }

    @Override
    public ScrapeResult<JobPostingEntity> scrape(ScrapeCommand command) {
        FetchedPage page = jobFetcher.fetch(FetchRequest.of(command.targetUrl(), command.siteCode()));
        if (!page.isSuccess()) {
            return ScrapeResult.failure(command.siteCode(),
                    "Playwright fetch failed with status " + page.statusCode());
        }

        List<JobPostingEntity> postings = parser.parse(page.htmlContent(), page.url()).stream()
                .map(normalizer::normalize)
                .toList();

        if (postings.isEmpty()) {
            return ScrapeResult.failure(command.siteCode(), "No dynamic job cards were rendered");
        }

        return ScrapeResult.success(postings, command.siteCode());
    }
}
