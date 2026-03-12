package com.campos.webscraper.application.factory;

import com.campos.webscraper.application.strategy.JobScraperStrategy;
import com.campos.webscraper.domain.model.TargetSiteEntity;

/**
 * Resolves the appropriate scraping strategy for a target site.
 */
public interface JobScraperFactory {

    /**
     * Resolves the first strategy that supports the provided target-site metadata.
     */
    JobScraperStrategy<?> resolve(TargetSiteEntity targetSite);
}
