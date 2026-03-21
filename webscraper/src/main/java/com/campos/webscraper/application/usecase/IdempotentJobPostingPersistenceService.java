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
        Map<String, JobPostingEntity> mergedExistingByFingerprint = new LinkedHashMap<>();
        for (JobPostingEntity candidate : candidates) {
            String fingerprintHash = fingerprintOf(candidate);
            if (existingByFingerprint.containsKey(fingerprintHash) || firstNewByFingerprint.containsKey(fingerprintHash)) {
                if (existingByFingerprint.containsKey(fingerprintHash)) {
                    JobPostingEntity existing = mergedExistingByFingerprint.getOrDefault(
                            fingerprintHash,
                            existingByFingerprint.get(fingerprintHash)
                    );
                    mergedExistingByFingerprint.put(fingerprintHash, merge(existing, candidate));
                }
                continue;
            }
            JobPostingEntity existing = resolveExisting(candidate, existingByFingerprint);
            if (existing == null) {
                firstNewByFingerprint.put(fingerprintHash, candidate);
            } else {
                mergedExistingByFingerprint.put(fingerprintHash, merge(existing, candidate));
            }
        }
        List<JobPostingEntity> newItems = List.copyOf(firstNewByFingerprint.values());
        List<JobPostingEntity> mergedExistingItems = mergedExistingByFingerprint.entrySet().stream()
                .filter(entry -> !sameContent(existingByFingerprint.get(entry.getKey()), entry.getValue()))
                .map(Map.Entry::getValue)
                .toList();

        Map<String, JobPostingEntity> savedByFingerprint = jobPostingRepository.saveAll(newItems).stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getFingerprintHash(), item), Map::putAll);
        Map<String, JobPostingEntity> updatedByFingerprint = jobPostingRepository.saveAll(mergedExistingItems).stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getFingerprintHash(), item), Map::putAll);

        return candidates.stream()
                .map(candidate -> {
                    String fingerprintHash = candidate.getFingerprintHash();
                    JobPostingEntity updated = updatedByFingerprint.get(fingerprintHash);
                    if (updated != null) {
                        return updated;
                    }
                    JobPostingEntity existing = existingByFingerprint.get(fingerprintHash);
                    if (existing != null) {
                        return existing;
                    }
                    return savedByFingerprint.get(fingerprintHash);
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

    private static JobPostingEntity merge(JobPostingEntity existing, JobPostingEntity candidate) {
        return JobPostingEntity.builder()
                .id(existing.getId())
                .crawlExecution(firstNonNull(candidate.getCrawlExecution(), existing.getCrawlExecution()))
                .targetSite(firstNonNull(candidate.getTargetSite(), existing.getTargetSite()))
                .externalId(firstNonBlank(candidate.getExternalId(), existing.getExternalId()))
                .canonicalUrl(firstNonBlank(candidate.getCanonicalUrl(), existing.getCanonicalUrl()))
                .title(firstNonBlank(candidate.getTitle(), existing.getTitle()))
                .company(firstNonBlank(candidate.getCompany(), existing.getCompany()))
                .location(firstNonBlank(candidate.getLocation(), existing.getLocation()))
                .remote(candidate.isRemote() || existing.isRemote())
                .contractType(firstNonNull(candidate.getContractType(), existing.getContractType()))
                .seniority(firstNonNull(candidate.getSeniority(), existing.getSeniority()))
                .salaryRange(firstNonBlank(candidate.getSalaryRange(), existing.getSalaryRange()))
                .techStackTags(resolveMergedTechStackTags(existing, candidate))
                .description(firstNonBlank(candidate.getDescription(), existing.getDescription()))
                .publishedAt(firstNonNull(candidate.getPublishedAt(), existing.getPublishedAt()))
                .applicationDeadline(firstNonNull(candidate.getApplicationDeadline(), existing.getApplicationDeadline()))
                .fingerprintHash(existing.getFingerprintHash())
                .dedupStatus(firstNonNull(existing.getDedupStatus(), candidate.getDedupStatus()))
                .payloadJson(firstNonBlank(candidate.getPayloadJson(), existing.getPayloadJson()))
                .createdAt(firstNonNull(existing.getCreatedAt(), candidate.getCreatedAt()))
                .updatedAt(firstNonNull(candidate.getUpdatedAt(), existing.getUpdatedAt()))
                .build();
    }

    private static boolean sameContent(JobPostingEntity existing, JobPostingEntity merged) {
        return Objects.equals(existing.getLocation(), merged.getLocation())
                && existing.isRemote() == merged.isRemote()
                && Objects.equals(existing.getSeniority(), merged.getSeniority())
                && Objects.equals(existing.getApplicationDeadline(), merged.getApplicationDeadline())
                && Objects.equals(existing.getDescription(), merged.getDescription())
                && Objects.equals(existing.getTechStackTags(), merged.getTechStackTags())
                && Objects.equals(existing.getPayloadJson(), merged.getPayloadJson())
                && Objects.equals(existing.getUpdatedAt(), merged.getUpdatedAt());
    }

    private static String resolveMergedTechStackTags(JobPostingEntity existing, JobPostingEntity candidate) {
        if (hasRefreshedContent(candidate)) {
            return normalizeBlank(candidate.getTechStackTags());
        }
        return firstNonBlank(candidate.getTechStackTags(), existing.getTechStackTags());
    }

    private static boolean hasRefreshedContent(JobPostingEntity candidate) {
        return normalizeBlank(candidate.getDescription()) != null
                || normalizeBlank(candidate.getPayloadJson()) != null;
    }

    private static String firstNonBlank(String preferred, String fallback) {
        String normalizedPreferred = normalizeBlank(preferred);
        if (normalizedPreferred != null) {
            return normalizedPreferred;
        }
        return normalizeBlank(fallback);
    }

    private static String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static <T> T firstNonNull(T preferred, T fallback) {
        return preferred != null ? preferred : fallback;
    }

    private static String fingerprintOf(JobPostingEntity candidate) {
        return Objects.requireNonNull(
                candidate.getFingerprintHash(),
                "candidate.fingerprintHash must not be null"
        );
    }
}
