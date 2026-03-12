package com.campos.webscraper.application.usecase;

import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.repository.PublicContestPostingRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Query use case for public contests.
 */
@Component
public class ListPublicContestsUseCase {

    private final PublicContestPostingRepository publicContestPostingRepository;

    public ListPublicContestsUseCase(PublicContestPostingRepository publicContestPostingRepository) {
        this.publicContestPostingRepository = Objects.requireNonNull(
                publicContestPostingRepository,
                "publicContestPostingRepository must not be null"
        );
    }

    /**
     * Returns contests filtered by status using the supported ordering options.
     */
    public List<PublicContestPostingEntity> execute(ContestStatus status, String orderBy) {
        if (!"registrationEndDate".equals(orderBy)) {
            throw new IllegalArgumentException("Unsupported orderBy: " + orderBy);
        }
        return publicContestPostingRepository.findByContestStatusOrderByRegistrationEndDateAsc(status);
    }
}
