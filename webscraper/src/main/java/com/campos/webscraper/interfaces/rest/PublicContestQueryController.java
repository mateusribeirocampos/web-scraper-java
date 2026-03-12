package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.application.usecase.ListPublicContestsUseCase;
import com.campos.webscraper.domain.enums.ContestStatus;
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

    public PublicContestQueryController(ListPublicContestsUseCase listPublicContestsUseCase) {
        this.listPublicContestsUseCase = Objects.requireNonNull(
                listPublicContestsUseCase,
                "listPublicContestsUseCase must not be null"
        );
    }

    /**
     * Lists public contests by status and supported ordering.
     */
    @GetMapping
    public List<PublicContestSummaryResponse> list(
            @RequestParam ContestStatus status,
            @RequestParam String orderBy
    ) {
        return listPublicContestsUseCase.execute(status, orderBy).stream()
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
