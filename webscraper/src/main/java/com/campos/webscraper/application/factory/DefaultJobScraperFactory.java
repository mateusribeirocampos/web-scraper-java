package com.campos.webscraper.application.factory;

import com.campos.webscraper.application.strategy.JobScraperStrategy;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.shared.UnsupportedSiteException;

import java.util.List;
import java.util.Objects;

/**
 * Default in-memory strategy resolver based on explicit target-site metadata.
 */
public class DefaultJobScraperFactory implements JobScraperFactory {

    private final List<JobScraperStrategy<?>> strategies;

    public DefaultJobScraperFactory(List<JobScraperStrategy<?>> strategies) {
        this.strategies = List.copyOf(Objects.requireNonNull(strategies, "strategies must not be null"));
    }

    @Override
    public JobScraperStrategy<?> resolve(TargetSiteEntity targetSite) {
        Objects.requireNonNull(targetSite, "targetSite must not be null");

        return strategies.stream()
                .filter(strategy -> strategy.supports(targetSite))
                .findFirst()
                .orElseThrow(() -> new UnsupportedSiteException(
                        "No JobScraperStrategy registered for siteCode=%s, siteType=%s, extractionMode=%s, jobCategory=%s"
                                .formatted(
                                        targetSite.getSiteCode(),
                                        targetSite.getSiteType(),
                                        targetSite.getExtractionMode(),
                                        targetSite.getJobCategory()
                                )
                ));
    }
}
