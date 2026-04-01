package com.campos.webscraper.domain.repository;

import com.campos.webscraper.domain.model.RawSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for RawSnapshotEntity.
 */
public interface RawSnapshotRepository extends JpaRepository<RawSnapshotEntity, Long> {

    List<RawSnapshotEntity> findBySiteCode(String siteCode);

    List<RawSnapshotEntity> findBySiteCodeAndResponseStatus(String siteCode, int responseStatus);
}
