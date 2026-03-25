package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.domain.model.CrawlExecutionEntity;
import com.campos.webscraper.domain.model.CrawlJobEntity;
import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.repository.CrawlExecutionRepository;
import com.campos.webscraper.domain.repository.CrawlJobRepository;
import com.campos.webscraper.domain.repository.JobPostingRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
public class RunOnboardingOperationalCheckUseCase {

    private final BootstrapOnboardingProfileWorkflowUseCase bootstrapOnboardingProfileWorkflowUseCase;
    private final CrawlJobRepository crawlJobRepository;
    private final CrawlExecutionRepository crawlExecutionRepository;
    private final JobPostingRepository jobPostingRepository;
    private final Clock clock;

    public RunOnboardingOperationalCheckUseCase(
            BootstrapOnboardingProfileWorkflowUseCase bootstrapOnboardingProfileWorkflowUseCase,
            CrawlJobRepository crawlJobRepository,
            CrawlExecutionRepository crawlExecutionRepository,
            JobPostingRepository jobPostingRepository,
            Clock clock
    ) {
        this.bootstrapOnboardingProfileWorkflowUseCase = Objects.requireNonNull(
                bootstrapOnboardingProfileWorkflowUseCase,
                "bootstrapOnboardingProfileWorkflowUseCase must not be null"
        );
        this.crawlJobRepository = Objects.requireNonNull(crawlJobRepository, "crawlJobRepository must not be null");
        this.crawlExecutionRepository = Objects.requireNonNull(crawlExecutionRepository, "crawlExecutionRepository must not be null");
        this.jobPostingRepository = Objects.requireNonNull(jobPostingRepository, "jobPostingRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public OnboardingOperationalCheckResult execute(String profileKey, boolean smokeRun, int daysBack) {
        Instant requestStartedAt = Instant.now(clock);
        BootstrappedOnboardingWorkflowResult workflow =
                bootstrapOnboardingProfileWorkflowUseCase.execute(profileKey, smokeRun);

        CrawlExecutionEntity observedExecution = resolveObservedExecution(workflow, requestStartedAt);
        OnboardingOperationalCheckExecutionSummary executionSummary = toExecutionSummary(observedExecution);
        LocalDate since = LocalDate.now().minusDays(daysBack);
        List<JobPostingEntity> recentPostings = observedExecution == null
                ? List.of()
                : jobPostingRepository.findTop5ByCrawlExecutionAndPublishedAtGreaterThanEqualOrderByPublishedAtDesc(
                        observedExecution,
                        since
                );
        long recentPostingsCount = observedExecution == null
                ? 0L
                : jobPostingRepository.countByCrawlExecutionAndPublishedAtGreaterThanEqual(observedExecution, since);

        return new OnboardingOperationalCheckResult(
                profileKey,
                workflow,
                executionSummary,
                Math.toIntExact(recentPostingsCount),
                recentPostings.stream().limit(5).toList()
        );
    }

    private CrawlExecutionEntity resolveObservedExecution(
            BootstrappedOnboardingWorkflowResult workflow,
            Instant requestStartedAt
    ) {
        Long observedJobId = workflow.smokeRun() != null && workflow.smokeRun().jobId() != null
                ? workflow.smokeRun().jobId()
                : workflow.crawlJob().crawlJob().getId();
        if (observedJobId == null) {
            return null;
        }

        CrawlJobEntity observedJob = crawlJobRepository.findById(observedJobId).orElse(null);
        if (observedJob == null) {
            return null;
        }

        List<CrawlExecutionEntity> executions = crawlExecutionRepository.findByCrawlJob(observedJob);
        if (workflow.smokeRun() != null && "SKIPPED_IN_FLIGHT".equals(workflow.smokeRun().smokeRunStatus())) {
            return executions.stream()
                    .max(Comparator.comparing(CrawlExecutionEntity::getCreatedAt))
                    .orElse(null);
        }

        return executions.stream()
                .filter(execution -> execution.getCreatedAt() != null && !execution.getCreatedAt().isBefore(requestStartedAt))
                .max(Comparator.comparing(CrawlExecutionEntity::getCreatedAt))
                .orElse(null);
    }

    private static OnboardingOperationalCheckExecutionSummary toExecutionSummary(CrawlExecutionEntity execution) {
        if (execution == null) {
            return null;
        }

        return new OnboardingOperationalCheckExecutionSummary(
                execution.getCrawlJob().getId(),
                execution.getId(),
                execution.getStatus().name(),
                execution.getItemsFound(),
                execution.getStartedAt(),
                execution.getFinishedAt()
        );
    }
}
