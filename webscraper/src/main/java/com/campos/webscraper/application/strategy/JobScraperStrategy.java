package com.campos.webscraper.application.strategy;

import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;

/**
 * Contract for site-specific extraction strategies.
 *
 * <p>Implementations decide support using explicit target-site metadata and execute the
 * extraction flow for a single source family, returning a {@link ScrapeResult} instead of
 * throwing expected extraction failures.
 *
 * @param <T> item type produced by the strategy during extraction
 */
public interface JobScraperStrategy<T> {

    /**
     * Returns whether this strategy supports the provided site metadata.
     */
    boolean supports(TargetSiteEntity targetSite);

    /**
     * Executes the extraction flow for the given command.
     */
    ScrapeResult<T> scrape(ScrapeCommand command);
}
