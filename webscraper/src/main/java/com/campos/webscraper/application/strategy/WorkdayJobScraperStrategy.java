package com.campos.webscraper.application.strategy;

import com.campos.webscraper.application.normalizer.WorkdayJobNormalizer;
import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.model.ScrapeCommand;
import com.campos.webscraper.domain.model.ScrapeResult;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.infrastructure.http.WorkdayJobBoardClient;
import com.campos.webscraper.interfaces.dto.WorkdayJobPostingResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * API-first strategy for public Workday CXS job boards.
 */
@Component
public class WorkdayJobScraperStrategy implements JobScraperStrategy<JobPostingEntity> {

    private static final String ITAJUBA_LOCATION_FACET_ID = "f5811cef9cb501d280d0106a4c0af747";
    private static final String POCOS_DE_CALDAS_LOCATION_FACET_ID = "78940cc24df4014699db7549cd0cb5c2";

    private final WorkdayJobBoardClient workdayJobBoardClient;
    private final WorkdayJobNormalizer normalizer;

    public WorkdayJobScraperStrategy(
            WorkdayJobBoardClient workdayJobBoardClient,
            WorkdayJobNormalizer normalizer
    ) {
        this.workdayJobBoardClient = Objects.requireNonNull(workdayJobBoardClient, "workdayJobBoardClient must not be null");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
    }

    @Override
    public boolean supports(TargetSiteEntity targetSite) {
        return targetSite.getBaseUrl() != null
                && targetSite.getBaseUrl().contains("myworkdayjobs.com/wday/cxs")
                && targetSite.getSiteType() == SiteType.TYPE_E
                && targetSite.getExtractionMode() == ExtractionMode.API
                && targetSite.getJobCategory() == JobCategory.PRIVATE_SECTOR
                && targetSite.getLegalStatus() == LegalStatus.APPROVED;
    }

    @Override
    public ScrapeResult<JobPostingEntity> scrape(ScrapeCommand command) {
        try {
            List<WorkdayJobPostingResponse> response = workdayJobBoardClient.fetchJobsByLocation(
                    command.targetUrl(),
                    resolveLocationFacetId(command.siteCode())
            );
            List<JobPostingEntity> postings = response.stream()
                    .map(item -> normalizer.normalize(command.siteCode(), item))
                    .toList();
            return ScrapeResult.success(postings, command.siteCode());
        } catch (RuntimeException exception) {
            return ScrapeResult.failure(command.siteCode(), exception.getMessage());
        }
    }

    private String resolveLocationFacetId(String siteCode) {
        if ("airbus_helibras_workday".equals(siteCode)) {
            return ITAJUBA_LOCATION_FACET_ID;
        }
        if ("alcoa_pocos_caldas_workday".equals(siteCode)) {
            return POCOS_DE_CALDAS_LOCATION_FACET_ID;
        }
        throw new IllegalStateException("No Workday location facet configured for site: " + siteCode);
    }
}
