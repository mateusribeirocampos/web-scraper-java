package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.enrichment.MunhozContestPdfEnricher;
import com.campos.webscraper.application.normalizer.MunhozContestNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.JobFetcher;
import com.campos.webscraper.infrastructure.parser.MunhozConcursosParser;
import com.campos.webscraper.infrastructure.parser.MunhozContestPreviewItem;
import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.FetchedPage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Static HTML strategy for the Munhoz municipal contests portal.
 */
@Component
public class MunhozContestScraperStrategy implements JobScraperStrategy<PublicContestPostingEntity> {

    private final JobFetcher jobFetcher;
    private final MunhozConcursosParser parser;
    private final MunhozContestPdfEnricher pdfEnricher;
    private final MunhozContestNormalizer normalizer;

    public MunhozContestScraperStrategy(
            JobFetcher jobFetcher,
            MunhozConcursosParser parser,
            MunhozContestPdfEnricher pdfEnricher,
            MunhozContestNormalizer normalizer
    ) {
        this.jobFetcher = Objects.requireNonNull(jobFetcher, "jobFetcher must not be null");
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
        this.pdfEnricher = Objects.requireNonNull(pdfEnricher, "pdfEnricher must not be null");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
    }

    @Override
    public boolean supports(TargetSiteEntity targetSite) {
        return "municipal_munhoz".equals(targetSite.getSiteCode())
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
                    "Munhoz listing request failed with status " + listingPage.statusCode());
        }

        List<String> detailUrls = parser.parseListingUrls(listingPage.htmlContent(), listingPage.url());
        List<MunhozContestPreviewItem> previewItems = new ArrayList<>();
        for (String detailUrl : detailUrls) {
            FetchedPage detailPage = jobFetcher.fetch(FetchRequest.of(detailUrl, command.siteCode()));
            if (!detailPage.isSuccess()) {
                return ScrapeResult.failure(command.siteCode(),
                        "Munhoz detail request failed with status "
                                + detailPage.statusCode()
                                + " for " + detailUrl);
            }
            MunhozContestPreviewItem previewItem = parser.parseDetail(detailPage.htmlContent(), detailPage.url());
            if (previewItem != null) {
                previewItems.add(previewItem);
            }
        }

        List<MunhozContestPreviewItem> enrichedPreviewItems = pdfEnricher.enrichAll(previewItems);
        List<PublicContestPostingEntity> postings = new ArrayList<>();
        for (MunhozContestPreviewItem item : enrichedPreviewItems) {
            try {
                postings.add(normalizer.normalize(item, listingPage.fetchedAt()));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed blocks instead of failing the whole municipal import.
            }
        }
        return ScrapeResult.success(postings, command.siteCode());
    }
}
