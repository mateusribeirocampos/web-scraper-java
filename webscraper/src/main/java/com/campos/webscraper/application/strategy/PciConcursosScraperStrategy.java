package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.normalizer.PciConcursosContestNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.JobFetcher;
import com.campos.webscraper.infrastructure.parser.PciConcursosFixtureParser;
import com.campos.webscraper.infrastructure.parser.PciConcursosParsePreview;
import com.campos.webscraper.infrastructure.parser.PciConcursosPreviewItem;
import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.FetchedPage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Static HTML strategy for PCI Concursos listing pages.
 */
public class PciConcursosScraperStrategy implements JobScraperStrategy<PublicContestPostingEntity> {

    private final JobFetcher jobFetcher;
    private final PciConcursosFixtureParser parser;
    private final PciConcursosContestNormalizer normalizer;

    public PciConcursosScraperStrategy(
            JobFetcher jobFetcher,
            PciConcursosFixtureParser parser,
            PciConcursosContestNormalizer normalizer
    ) {
        this.jobFetcher = Objects.requireNonNull(jobFetcher, "jobFetcher must not be null");
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
    }

    @Override
    public boolean supports(TargetSiteEntity targetSite) {
        return "pci_concursos".equals(targetSite.getSiteCode())
                && targetSite.getSiteType() == SiteType.TYPE_A
                && targetSite.getExtractionMode() == ExtractionMode.STATIC_HTML
                && targetSite.getJobCategory() == JobCategory.PUBLIC_CONTEST
                && targetSite.getLegalStatus() == LegalStatus.APPROVED;
    }

    @Override
    public ScrapeResult<PublicContestPostingEntity> scrape(ScrapeCommand command) {
        List<PublicContestPostingEntity> postings = new ArrayList<>();
        Set<String> visitedUrls = new HashSet<>();
        String nextUrl = command.targetUrl();

        while (nextUrl != null && visitedUrls.add(nextUrl)) {
            FetchedPage page = jobFetcher.fetch(FetchRequest.of(nextUrl, command.siteCode()));
            if (!page.isSuccess()) {
                return ScrapeResult.failure(command.siteCode(),
                        "PCI Concursos request failed with status " + page.statusCode());
            }

            PciConcursosParsePreview preview = parser.parse(page.htmlContent(), page.url());
            for (PciConcursosPreviewItem item : preview.items()) {
                postings.add(normalizer.normalize(item, page.fetchedAt()));
            }
            String candidateNextUrl = parser.extractNextPageUrl(page.htmlContent(), page.url());
            nextUrl = Objects.equals(candidateNextUrl, page.url()) ? null : candidateNextUrl;
        }

        return ScrapeResult.success(postings, command.siteCode());
    }
}
