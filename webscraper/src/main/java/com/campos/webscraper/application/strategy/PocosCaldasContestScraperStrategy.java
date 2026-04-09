package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.normalizer.PocosCaldasContestNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.HttpJobFetcher;
import com.campos.webscraper.infrastructure.http.JobFetcher;
import com.campos.webscraper.infrastructure.parser.PocosCaldasContestPdfParser;
import com.campos.webscraper.infrastructure.parser.PocosCaldasContestPreviewItem;
import com.campos.webscraper.infrastructure.pdf.PdfTextExtractor;
import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.FetchedPage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class PocosCaldasContestScraperStrategy implements JobScraperStrategy<PublicContestPostingEntity> {

    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

    private final PdfTextExtractor pdfTextExtractor;
    private final PocosCaldasContestPdfParser parser;
    private final PocosCaldasContestNormalizer normalizer;
    private final JobFetcher jobFetcher;

    public PocosCaldasContestScraperStrategy(
            PdfTextExtractor pdfTextExtractor,
            PocosCaldasContestPdfParser parser,
            PocosCaldasContestNormalizer normalizer
    ) {
        this(pdfTextExtractor, parser, normalizer, new HttpJobFetcher());
    }

    PocosCaldasContestScraperStrategy(
            PdfTextExtractor pdfTextExtractor,
            PocosCaldasContestPdfParser parser,
            PocosCaldasContestNormalizer normalizer,
            JobFetcher jobFetcher
    ) {
        this.pdfTextExtractor = Objects.requireNonNull(pdfTextExtractor, "pdfTextExtractor must not be null");
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
        this.jobFetcher = Objects.requireNonNull(jobFetcher, "jobFetcher must not be null");
    }

    @Override
    public boolean supports(TargetSiteEntity targetSite) {
        return "municipal_pocos_caldas".equals(targetSite.getSiteCode())
                && targetSite.getSiteType() == SiteType.TYPE_A
                && targetSite.getExtractionMode() == ExtractionMode.STATIC_HTML
                && targetSite.getJobCategory() == JobCategory.PUBLIC_CONTEST
                && targetSite.getLegalStatus() == LegalStatus.APPROVED;
    }

    @Override
    public ScrapeResult<PublicContestPostingEntity> scrape(ScrapeCommand command) {
        try {
            String editalUrl = resolveCanonicalEditalUrl(command.targetUrl());
            String pdfText = pdfTextExtractor.extractText(editalUrl);
            PocosCaldasContestPreviewItem item = parser.parse(pdfText, editalUrl);
            PublicContestPostingEntity posting = normalizer.normalize(item, LocalDateTime.now(BRAZIL_ZONE));
            return ScrapeResult.success(List.of(posting), command.siteCode());
        } catch (RuntimeException exception) {
            return ScrapeResult.failure(command.siteCode(), exception.getMessage());
        }
    }

    private String resolveCanonicalEditalUrl(String targetUrl) {
        if (targetUrl.toLowerCase().contains(".pdf")) {
            return targetUrl;
        }

        FetchedPage page = jobFetcher.fetch(new FetchRequest(
                targetUrl,
                Map.of(
                        "User-Agent", "Mozilla/5.0 (compatible; WebScraperJava/1.0; +https://example.local)",
                        "Accept", "text/html,application/xhtml+xml"
                ),
                10_000,
                true,
                "municipal_pocos_caldas"
        ));

        if (!page.isSuccess() || page.isEmpty()) {
            throw new IllegalStateException("Failed to fetch Poços de Caldas contests listing");
        }

        String editalUrl = parser.selectCanonicalEditalUrl(page.htmlContent(), page.url());
        if (editalUrl == null || editalUrl.isBlank()) {
            throw new IllegalStateException("No canonical Poços de Caldas edital PDF found on contests listing");
        }
        return editalUrl;
    }
}
