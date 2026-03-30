package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.strategy.InconfidentesContestScraperStrategy;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.PublicContestPostingRepository;
import com.campos.webscraper.shared.ContestPostingFingerprintCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * End-to-end use case for importing municipal public contests from Inconfidentes.
 */
@Component
public class InconfidentesContestImportUseCase extends AbstractMunicipalContestImportUseCase {

    private final InconfidentesContestScraperStrategy strategy;

    @Autowired
    public InconfidentesContestImportUseCase(
            PublicContestPostingRepository publicContestPostingRepository,
            InconfidentesContestScraperStrategy strategy
    ) {
        this(
                publicContestPostingRepository,
                strategy,
                new ContestPostingFingerprintCalculator(),
                new IdempotentPublicContestPersistenceService(publicContestPostingRepository)
        );
    }

    public InconfidentesContestImportUseCase(
            PublicContestPostingRepository publicContestPostingRepository,
            InconfidentesContestScraperStrategy strategy,
            ContestPostingFingerprintCalculator fingerprintCalculator,
            IdempotentPublicContestPersistenceService idempotentPersistenceService
    ) {
        super(
                publicContestPostingRepository,
                fingerprintCalculator,
                idempotentPersistenceService
        );
        this.strategy = Objects.requireNonNull(strategy, "strategy must not be null");
    }

    public List<PublicContestPostingEntity> execute(
            TargetSiteEntity targetSite,
            CrawlExecutionEntity crawlExecution,
            ScrapeCommand command
    ) {
        return executeImport(targetSite, crawlExecution, command);
    }

    @Override
    protected ScrapeResult<PublicContestPostingEntity> scrape(ScrapeCommand command) {
        return strategy.scrape(command);
    }

    @Override
    protected String failurePrefix() {
        return "Inconfidentes scrape failed: ";
    }
}
