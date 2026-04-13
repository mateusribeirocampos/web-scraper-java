package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.usecase.ListPublicContestsUseCase;
import com.campos.webscraper.application.usecase.PublicContestSearchProfileMatcher;
import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.domain.enums.PublicContestSearchProfile;
import com.campos.webscraper.interfaces.dto.PublicContestSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * REST queries for public contests.
 */
@RestController
@RequestMapping("/api/v1/public-contests")
public class PublicContestQueryController {

    private final ListPublicContestsUseCase listPublicContestsUseCase;
    private final PublicContestSearchProfileMatcher profileMatcher;

    public PublicContestQueryController(
            ListPublicContestsUseCase listPublicContestsUseCase,
            PublicContestSearchProfileMatcher profileMatcher
    ) {
        this.listPublicContestsUseCase = Objects.requireNonNull(
                listPublicContestsUseCase,
                "listPublicContestsUseCase must not be null"
        );
        this.profileMatcher = Objects.requireNonNull(
                profileMatcher,
                "profileMatcher must not be null"
        );
    }

    /**
     * Lists public contests by status and supported ordering, optionally filtered by a relevance profile.
     *
     * <p>The default profile is {@code TI_DEGREE_AND_ROLE}, which filters contests requiring
     * IT-related education and positions. Use {@code UNFILTERED} to return all contests.
     */
    @GetMapping
    public List<PublicContestSummaryResponse> list(
            @RequestParam ContestStatus status,
            @RequestParam String orderBy,
            @RequestParam(defaultValue = "TI_DEGREE_AND_ROLE") PublicContestSearchProfile profile
    ) {
        return listPublicContestsUseCase.execute(status, orderBy).stream()
                .filter(contest -> profileMatcher.matches(contest, profile))
                .map(contest -> new PublicContestSummaryResponse(
                        contest.getId(),
                        contest.getContestName(),
                        contest.getOrganizer(),
                        contest.getPositionTitle(),
                        contest.getCanonicalUrl(),
                        contest.getPublishedAt(),
                        contest.getRegistrationEndDate()
                ))
                .toList();
    }
}
