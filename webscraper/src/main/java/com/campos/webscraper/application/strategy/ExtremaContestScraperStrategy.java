package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.enrichment.ExtremaContestPdfEnricher;
import com.campos.webscraper.application.normalizer.ExtremaContestNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.JobFetcher;
import com.campos.webscraper.infrastructure.parser.ExtremaConcursosParser;
import com.campos.webscraper.infrastructure.parser.ExtremaContestPreviewItem;
import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.FetchedPage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Static HTML strategy for the Extrema municipal education contest pages.
 */
@Component
public class ExtremaContestScraperStrategy implements JobScraperStrategy<PublicContestPostingEntity> {

    private final JobFetcher jobFetcher;
    private final ExtremaConcursosParser parser;
    private final ExtremaContestPdfEnricher pdfEnricher;
    private final ExtremaContestNormalizer normalizer;

    public ExtremaContestScraperStrategy(
            JobFetcher jobFetcher,
            ExtremaConcursosParser parser,
            ExtremaContestPdfEnricher pdfEnricher,
            ExtremaContestNormalizer normalizer
    ) {
        this.jobFetcher = Objects.requireNonNull(jobFetcher, "jobFetcher must not be null");
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
        this.pdfEnricher = Objects.requireNonNull(pdfEnricher, "pdfEnricher must not be null");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
    }

    @Override
    public boolean supports(TargetSiteEntity targetSite) {
        return "municipal_extrema".equals(targetSite.getSiteCode())
                && targetSite.getSiteType() == SiteType.TYPE_A
                && targetSite.getExtractionMode() == ExtractionMode.STATIC_HTML
                && targetSite.getJobCategory() == JobCategory.PUBLIC_CONTEST
                && targetSite.getLegalStatus() == LegalStatus.APPROVED;
    }

    @Override
    public ScrapeResult<PublicContestPostingEntity> scrape(ScrapeCommand command) {
        FetchedPage listingPage = jobFetcher.fetch(FetchRequest.of(command.targetUrl(), command.siteCode()));
        if (!listingPage.isSuccess()) {
            return ScrapeResult.failure(command.siteCode(),
                    "Extrema listing request failed with status " + listingPage.statusCode());
        }

        List<String> detailUrls = parser.parseListingUrls(listingPage.htmlContent(), listingPage.url());
        List<ExtremaContestPreviewItem> previewItems = new ArrayList<>();
        for (String detailUrl : detailUrls) {
            FetchedPage detailPage = jobFetcher.fetch(FetchRequest.of(detailUrl, command.siteCode()));
            if (!detailPage.isSuccess()) {
                return ScrapeResult.failure(command.siteCode(),
                        "Extrema detail request failed with status "
                                + detailPage.statusCode()
                                + " for " + detailUrl);
            }
            ExtremaContestPreviewItem previewItem = parser.parseDetail(detailPage.htmlContent(), detailPage.url());
            if (previewItem != null) {
                previewItems.add(previewItem);
            }
        }

        List<ExtremaContestPreviewItem> enrichedPreviewItems = pdfEnricher.enrichAll(previewItems);
        List<PublicContestPostingEntity> postings = new ArrayList<>();
        for (ExtremaContestPreviewItem item : enrichedPreviewItems) {
            try {
                postings.add(normalizer.normalize(item, listingPage.fetchedAt()));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed items instead of failing the whole municipal import.
            }
        }
        return ScrapeResult.success(postings, command.siteCode());
    }
}
