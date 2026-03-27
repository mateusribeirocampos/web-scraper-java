package com.campos.webscraper.domain.repository;

import com.campos.webscraper.domain.enums.ContestStatus;
import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for PublicContestPostingEntity.
 */
public interface PublicContestPostingRepository extends JpaRepository<PublicContestPostingEntity, Long> {

    /**
     * Returns contests by fingerprint hash for deduplication.
     */
    Optional<PublicContestPostingEntity> findByFingerprintHash(String fingerprintHash);

    /**
     * Returns contests by stable external id within the same target site.
     */
    Optional<PublicContestPostingEntity> findByTargetSiteAndExternalId(
            TargetSiteEntity targetSite,
            String externalId
    );

    /**
     * Returns open contests whose registration deadline is still active, ordered by nearest deadline first.
     */
    List<PublicContestPostingEntity> findByContestStatusAndRegistrationEndDateGreaterThanEqualOrderByRegistrationEndDateAsc(
            ContestStatus contestStatus,
            LocalDate registrationEndDate
    );

    /**
     * Returns contests by status ordered by nearest registration end date first.
     */
    List<PublicContestPostingEntity> findByContestStatusOrderByRegistrationEndDateAsc(ContestStatus contestStatus);
}
