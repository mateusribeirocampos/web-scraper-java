package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.normalizer.CamaraSantaRitaContestNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.JobFetcher;
import com.campos.webscraper.infrastructure.parser.CamaraSantaRitaContestPreviewItem;
import com.campos.webscraper.infrastructure.parser.CamaraSantaRitaProcessosSeletivosParser;
import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.FetchedPage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Static HTML strategy for the Câmara de Santa Rita do Sapucaí process page.
 */
@Component
public class CamaraSantaRitaContestScraperStrategy implements JobScraperStrategy<PublicContestPostingEntity> {

    private final JobFetcher jobFetcher;
    private final CamaraSantaRitaProcessosSeletivosParser parser;
    private final CamaraSantaRitaContestNormalizer normalizer;

    public CamaraSantaRitaContestScraperStrategy(
            JobFetcher jobFetcher,
            CamaraSantaRitaProcessosSeletivosParser parser,
            CamaraSantaRitaContestNormalizer normalizer
    ) {
        this.jobFetcher = Objects.requireNonNull(jobFetcher, "jobFetcher must not be null");
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
    }

    @Override
    public boolean supports(TargetSiteEntity targetSite) {
        return "camara_santa_rita_sapucai".equals(targetSite.getSiteCode())
                && targetSite.getSiteType() == SiteType.TYPE_A
                && targetSite.getExtractionMode() == ExtractionMode.STATIC_HTML
                && targetSite.getJobCategory() == JobCategory.PUBLIC_CONTEST
                && targetSite.getLegalStatus() == LegalStatus.APPROVED;
    }

    @Override
    public ScrapeResult<PublicContestPostingEntity> scrape(ScrapeCommand command) {
        FetchedPage page = jobFetcher.fetch(FetchRequest.of(command.targetUrl(), command.siteCode()));
        if (!page.isSuccess()) {
            return ScrapeResult.failure(
                    command.siteCode(),
                    "Santa Rita Câmara request failed with status " + page.statusCode()
            );
        }

        List<CamaraSantaRitaContestPreviewItem> previewItems = parser.parse(page.htmlContent(), page.url());
        List<PublicContestPostingEntity> postings = previewItems.stream()
                .map(item -> normalizer.normalize(item, page.fetchedAt()))
                .toList();
        return ScrapeResult.success(postings, command.siteCode());
    }
}
