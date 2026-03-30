package com.campos.webscraper.application.usecase;

import com.campos.webscraper.application.strategy.PousoAlegreContestScraperStrategy;
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
 * End-to-end use case for importing municipal public contests from Pouso Alegre.
 */
@Component
public class PousoAlegreContestImportUseCase extends AbstractMunicipalContestImportUseCase {

    private final PousoAlegreContestScraperStrategy strategy;

    @Autowired
    public PousoAlegreContestImportUseCase(
            PublicContestPostingRepository publicContestPostingRepository,
            PousoAlegreContestScraperStrategy strategy
    ) {
        this(
                publicContestPostingRepository,
                strategy,
                new ContestPostingFingerprintCalculator(),
                new IdempotentPublicContestPersistenceService(publicContestPostingRepository)
        );
    }

    public PousoAlegreContestImportUseCase(
            PublicContestPostingRepository publicContestPostingRepository,
            PousoAlegreContestScraperStrategy strategy,
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
        return "Pouso Alegre scrape failed: ";
    }
}
