package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.normalizer.InconfidentesContestNormalizer;
import com.campos.webscraper.application.enrichment.InconfidentesContestPdfEnricher;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.JobFetcher;
import com.campos.webscraper.infrastructure.parser.InconfidentesContestPreviewItem;
import com.campos.webscraper.infrastructure.parser.InconfidentesEditaisFixtureParser;
import com.campos.webscraper.infrastructure.parser.InconfidentesParsePreview;
import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.FetchedPage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Static HTML strategy for the Inconfidentes municipal editais page.
 */
@Component
public class InconfidentesContestScraperStrategy implements JobScraperStrategy<PublicContestPostingEntity> {

    private final JobFetcher jobFetcher;
    private final InconfidentesEditaisFixtureParser parser;
    private final InconfidentesContestPdfEnricher pdfEnricher;
    private final InconfidentesContestNormalizer normalizer;

    public InconfidentesContestScraperStrategy(
            JobFetcher jobFetcher,
            InconfidentesEditaisFixtureParser parser,
            InconfidentesContestPdfEnricher pdfEnricher,
            InconfidentesContestNormalizer normalizer
    ) {
        this.jobFetcher = Objects.requireNonNull(jobFetcher, "jobFetcher must not be null");
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
        this.pdfEnricher = Objects.requireNonNull(pdfEnricher, "pdfEnricher must not be null");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
    }

    @Override
    public boolean supports(TargetSiteEntity targetSite) {
        return "municipal_inconfidentes".equals(targetSite.getSiteCode())
                && targetSite.getSiteType() == SiteType.TYPE_A
                && targetSite.getExtractionMode() == ExtractionMode.STATIC_HTML
                && targetSite.getJobCategory() == JobCategory.PUBLIC_CONTEST
                && targetSite.getLegalStatus() == LegalStatus.APPROVED;
    }

    @Override
    public ScrapeResult<PublicContestPostingEntity> scrape(ScrapeCommand command) {
        FetchedPage page = jobFetcher.fetch(FetchRequest.of(command.targetUrl(), command.siteCode()));
        if (!page.isSuccess()) {
            return ScrapeResult.failure(command.siteCode(),
                    "Inconfidentes request failed with status " + page.statusCode());
        }

        InconfidentesParsePreview preview = parser.parse(page.htmlContent(), page.url());
        List<InconfidentesContestPreviewItem> enrichedPreviewItems = pdfEnricher.enrichAll(preview.items());
        List<PublicContestPostingEntity> postings = new ArrayList<>();
        for (InconfidentesContestPreviewItem item : enrichedPreviewItems) {
            try {
                postings.add(normalizer.normalize(item, page.fetchedAt()));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed contest blocks instead of failing the whole municipal import.
            }
        }
        return ScrapeResult.success(postings, command.siteCode());
    }
}
