package com.campos.webscraper.application.usecase;

import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.repository.JobPostingRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Persists private-sector postings idempotently using the fingerprint hash as the stable identity.
 */
@Component
public class IdempotentJobPostingPersistenceService {

    private final JobPostingRepository jobPostingRepository;

    public IdempotentJobPostingPersistenceService(JobPostingRepository jobPostingRepository) {
        this.jobPostingRepository = Objects.requireNonNull(jobPostingRepository, "jobPostingRepository must not be null");
    }

    public List<JobPostingEntity> persist(List<JobPostingEntity> candidates) {
        Objects.requireNonNull(candidates, "candidates must not be null");

        Map<String, JobPostingEntity> existingByFingerprint = new LinkedHashMap<>();
        Map<String, JobPostingEntity> firstNewByFingerprint = new LinkedHashMap<>();
        for (JobPostingEntity candidate : candidates) {
            String fingerprintHash = fingerprintOf(candidate);
            if (existingByFingerprint.containsKey(fingerprintHash) || firstNewByFingerprint.containsKey(fingerprintHash)) {
                continue;
            }
            if (resolveExisting(candidate, existingByFingerprint) == null) {
                firstNewByFingerprint.put(fingerprintHash, candidate);
            }
        }
        List<JobPostingEntity> newItems = List.copyOf(firstNewByFingerprint.values());

        Map<String, JobPostingEntity> savedByFingerprint = jobPostingRepository.saveAll(newItems).stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getFingerprintHash(), item), Map::putAll);

        return candidates.stream()
                .map(candidate -> {
                    JobPostingEntity existing = existingByFingerprint.get(candidate.getFingerprintHash());
                    if (existing != null) {
                        return existing;
                    }
                    return savedByFingerprint.get(candidate.getFingerprintHash());
                })
                .toList();
    }

    private JobPostingEntity resolveExisting(
            JobPostingEntity candidate,
            Map<String, JobPostingEntity> existingByFingerprint
    ) {
        String fingerprintHash = fingerprintOf(candidate);
        return jobPostingRepository.findByFingerprintHash(fingerprintHash)
                .map(existing -> {
                    existingByFingerprint.put(fingerprintHash, existing);
                    return existing;
                })
                .orElse(null);
    }

    private static String fingerprintOf(JobPostingEntity candidate) {
        return Objects.requireNonNull(
                candidate.getFingerprintHash(),
                "candidate.fingerprintHash must not be null"
        );
    }
}
