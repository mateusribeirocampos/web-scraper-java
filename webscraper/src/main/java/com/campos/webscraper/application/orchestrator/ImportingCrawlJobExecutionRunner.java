package com.campos.webscraper.application.orchestrator;

import com.campos.webscraper.application.usecase.DouContestImportUseCase;
import com.campos.webscraper.application.usecase.GreenhouseJobImportUseCase;
import com.campos.webscraper.application.usecase.IndeedJobImportUseCase;
import com.campos.webscraper.application.usecase.PciConcursosImportUseCase;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.shared.UnsupportedSiteException;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Resolves the import path for a crawl job and persists the extracted items through the existing use cases.
 */
@Component
public class ImportingCrawlJobExecutionRunner implements CrawlJobExecutionRunner {

    private final IndeedJobImportUseCase indeedJobImportUseCase;
    private final GreenhouseJobImportUseCase greenhouseJobImportUseCase;
    private final DouContestImportUseCase douContestImportUseCase;
    private final PciConcursosImportUseCase pciConcursosImportUseCase;

    public ImportingCrawlJobExecutionRunner(
            IndeedJobImportUseCase indeedJobImportUseCase,
            GreenhouseJobImportUseCase greenhouseJobImportUseCase,
            DouContestImportUseCase douContestImportUseCase,
            PciConcursosImportUseCase pciConcursosImportUseCase
    ) {
        this.indeedJobImportUseCase = Objects.requireNonNull(
                indeedJobImportUseCase,
                "indeedJobImportUseCase must not be null"
        );
        this.greenhouseJobImportUseCase = Objects.requireNonNull(
                greenhouseJobImportUseCase,
                "greenhouseJobImportUseCase must not be null"
        );
        this.douContestImportUseCase = Objects.requireNonNull(
                douContestImportUseCase,
                "douContestImportUseCase must not be null"
        );
        this.pciConcursosImportUseCase = Objects.requireNonNull(
                pciConcursosImportUseCase,
                "pciConcursosImportUseCase must not be null"
        );
    }

    @Override
    public CrawlExecutionOutcome run(CrawlJobEntity crawlJob, CrawlExecutionEntity crawlExecution) {
        Objects.requireNonNull(crawlJob, "crawlJob must not be null");
        Objects.requireNonNull(crawlExecution, "crawlExecution must not be null");

        TargetSiteEntity targetSite = Objects.requireNonNull(crawlJob.getTargetSite(), "crawlJob.targetSite must not be null");
        ScrapeCommand command = new ScrapeCommand(
                Objects.requireNonNull(targetSite.getSiteCode(), "targetSite.siteCode must not be null"),
                Objects.requireNonNull(targetSite.getBaseUrl(), "targetSite.baseUrl must not be null"),
                Objects.requireNonNull(targetSite.getExtractionMode(), "targetSite.extractionMode must not be null"),
                effectiveJobCategory(crawlJob, targetSite)
        );

        int itemsFound = switch (targetSite.getSiteCode()) {
            case "indeed-br" -> indeedJobImportUseCase.execute(targetSite, crawlExecution, command).size();
            case "greenhouse_bitso" -> greenhouseJobImportUseCase.execute(targetSite, crawlExecution, command).size();
            case "dou-api" -> douContestImportUseCase.execute(targetSite, crawlExecution, command).size();
            case "pci_concursos" -> pciConcursosImportUseCase.execute(targetSite, crawlExecution, command).size();
            default -> throw new UnsupportedSiteException("No import runner registered for site: " + targetSite.getSiteCode());
        };

        return new CrawlExecutionOutcome(1, itemsFound);
    }

    private static JobCategory effectiveJobCategory(CrawlJobEntity crawlJob, TargetSiteEntity targetSite) {
        if (crawlJob.getJobCategory() != null) {
            return crawlJob.getJobCategory();
        }
        return Objects.requireNonNull(targetSite.getJobCategory(), "targetSite.jobCategory must not be null");
    }
}
