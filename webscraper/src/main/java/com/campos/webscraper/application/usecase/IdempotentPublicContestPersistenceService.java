package com.campos.webscraper.application.usecase;

import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.repository.PublicContestPostingRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Persists public-contest postings idempotently using the fingerprint hash as the stable identity.
 */
@Component
public class IdempotentPublicContestPersistenceService {

    private final PublicContestPostingRepository publicContestPostingRepository;

    public IdempotentPublicContestPersistenceService(PublicContestPostingRepository publicContestPostingRepository) {
        this.publicContestPostingRepository = Objects.requireNonNull(
                publicContestPostingRepository,
                "publicContestPostingRepository must not be null"
        );
    }

    public List<PublicContestPostingEntity> persist(List<PublicContestPostingEntity> candidates) {
        Objects.requireNonNull(candidates, "candidates must not be null");

        Map<String, PublicContestPostingEntity> existingByFingerprint = new LinkedHashMap<>();
        Map<String, PublicContestPostingEntity> firstNewByFingerprint = new LinkedHashMap<>();
        for (PublicContestPostingEntity candidate : candidates) {
            String fingerprintHash = fingerprintOf(candidate);
            if (existingByFingerprint.containsKey(fingerprintHash) || firstNewByFingerprint.containsKey(fingerprintHash)) {
                continue;
            }
            if (resolveExisting(candidate, existingByFingerprint) == null) {
                firstNewByFingerprint.put(fingerprintHash, candidate);
            }
        }
        List<PublicContestPostingEntity> newItems = List.copyOf(firstNewByFingerprint.values());

        Map<String, PublicContestPostingEntity> savedByFingerprint = publicContestPostingRepository.saveAll(newItems).stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getFingerprintHash(), item), Map::putAll);

        return candidates.stream()
                .map(candidate -> {
                    PublicContestPostingEntity existing = existingByFingerprint.get(candidate.getFingerprintHash());
                    if (existing != null) {
                        return existing;
                    }
                    return savedByFingerprint.get(candidate.getFingerprintHash());
                })
                .toList();
    }

    private PublicContestPostingEntity resolveExisting(
            PublicContestPostingEntity candidate,
            Map<String, PublicContestPostingEntity> existingByFingerprint
    ) {
        String fingerprintHash = fingerprintOf(candidate);
        return publicContestPostingRepository.findByFingerprintHash(fingerprintHash)
                .map(existing -> {
                    existingByFingerprint.put(fingerprintHash, existing);
                    return existing;
                })
                .orElse(null);
    }

    private static String fingerprintOf(PublicContestPostingEntity candidate) {
        return Objects.requireNonNull(
                candidate.getFingerprintHash(),
                "candidate.fingerprintHash must not be null"
        );
    }
}
