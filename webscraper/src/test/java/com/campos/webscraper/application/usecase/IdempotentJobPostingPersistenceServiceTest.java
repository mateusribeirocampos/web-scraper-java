package com.campos.webscraper.application.usecase;

import com.campos.webscraper.domain.model.JobPostingEntity;
import com.campos.webscraper.domain.repository.JobPostingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotentJobPostingPersistenceService")
class IdempotentJobPostingPersistenceServiceTest {

    @Mock
    private JobPostingRepository jobPostingRepository;

    @Test
    @DisplayName("should skip duplicate fingerprints and preserve result order")
    void shouldSkipDuplicateFingerprintsAndPreserveResultOrder() {
        JobPostingEntity existing = posting("fp-existing", "existing-id");
        JobPostingEntity duplicateCandidate = posting("fp-existing", "duplicate-id");
        JobPostingEntity newCandidate = posting("fp-new", "new-id");
        JobPostingEntity savedNew = posting("fp-new", "new-id");

        when(jobPostingRepository.findByFingerprintHash("fp-existing")).thenReturn(Optional.of(existing));
        when(jobPostingRepository.findByFingerprintHash("fp-new")).thenReturn(Optional.empty());
        when(jobPostingRepository.saveAll(anyList())).thenReturn(List.of(savedNew));

        IdempotentJobPostingPersistenceService service = new IdempotentJobPostingPersistenceService(jobPostingRepository);

        List<JobPostingEntity> persisted = service.persist(List.of(duplicateCandidate, newCandidate));

        assertThat(persisted).containsExactly(existing, savedNew);
        verify(jobPostingRepository).saveAll(List.of(newCandidate));
    }

    @Test
    @DisplayName("should deduplicate repeated new fingerprints inside the same batch")
    void shouldDeduplicateRepeatedNewFingerprintsInsideTheSameBatch() {
        JobPostingEntity firstCandidate = posting("fp-new", "new-a");
        JobPostingEntity duplicateCandidate = posting("fp-new", "new-b");
        JobPostingEntity savedNew = posting("fp-new", "new-a");

        when(jobPostingRepository.findByFingerprintHash("fp-new")).thenReturn(Optional.empty());
        when(jobPostingRepository.saveAll(anyList())).thenReturn(List.of(savedNew));

        IdempotentJobPostingPersistenceService service = new IdempotentJobPostingPersistenceService(jobPostingRepository);

        List<JobPostingEntity> persisted = service.persist(List.of(firstCandidate, duplicateCandidate));

        assertThat(persisted).containsExactly(savedNew, savedNew);
        verify(jobPostingRepository).saveAll(List.of(firstCandidate));
    }

    @Test
    @DisplayName("should merge enriched fields into an existing posting with the same fingerprint")
    void shouldMergeEnrichedFieldsIntoAnExistingPostingWithTheSameFingerprint() {
        Instant existingUpdatedAt = Instant.parse("2026-03-21T10:00:00Z");
        Instant candidateUpdatedAt = Instant.parse("2026-03-21T11:00:00Z");

        JobPostingEntity existing = JobPostingEntity.builder()
                .id(10L)
                .fingerprintHash("fp-existing")
                .externalId("existing-id")
                .canonicalUrl("https://example.com/jobs/10")
                .title("Backend Engineer")
                .company("Acme")
                .location(null)
                .remote(false)
                .description(null)
                .techStackTags(null)
                .publishedAt(LocalDate.of(2026, 3, 20))
                .payloadJson("{\"old\":true}")
                .updatedAt(existingUpdatedAt)
                .build();

        JobPostingEntity candidate = JobPostingEntity.builder()
                .fingerprintHash("fp-existing")
                .externalId("existing-id")
                .canonicalUrl("https://example.com/jobs/10")
                .title("Backend Engineer")
                .company("Acme")
                .location("Brazil")
                .remote(true)
                .description("<p>Java Spring role</p>")
                .techStackTags("Java,Spring")
                .publishedAt(LocalDate.of(2026, 3, 20))
                .applicationDeadline(LocalDate.of(2026, 4, 20))
                .payloadJson("{\"content\":true}")
                .updatedAt(candidateUpdatedAt)
                .build();

        JobPostingEntity merged = JobPostingEntity.builder()
                .id(10L)
                .fingerprintHash("fp-existing")
                .externalId("existing-id")
                .canonicalUrl("https://example.com/jobs/10")
                .title("Backend Engineer")
                .company("Acme")
                .location("Brazil")
                .remote(true)
                .description("<p>Java Spring role</p>")
                .techStackTags("Java,Spring")
                .publishedAt(LocalDate.of(2026, 3, 20))
                .applicationDeadline(LocalDate.of(2026, 4, 20))
                .payloadJson("{\"content\":true}")
                .updatedAt(candidateUpdatedAt)
                .build();

        when(jobPostingRepository.findByFingerprintHash("fp-existing")).thenReturn(Optional.of(existing));
        when(jobPostingRepository.saveAll(anyList())).thenReturn(List.of(merged));

        IdempotentJobPostingPersistenceService service = new IdempotentJobPostingPersistenceService(jobPostingRepository);

        List<JobPostingEntity> persisted = service.persist(List.of(candidate));

        assertThat(persisted).containsExactly(merged);
        verify(jobPostingRepository).saveAll(argThat(items -> {
            List<JobPostingEntity> list = StreamSupport.stream(items.spliterator(), false).toList();
            if (list.size() != 1) {
                return false;
            }
            JobPostingEntity item = list.get(0);
            return "fp-existing".equals(item.getFingerprintHash())
                    && "Brazil".equals(item.getLocation())
                    && item.isRemote()
                    && "<p>Java Spring role</p>".equals(item.getDescription())
                    && "Java,Spring".equals(item.getTechStackTags())
                    && LocalDate.of(2026, 4, 20).equals(item.getApplicationDeadline())
                    && "{\"content\":true}".equals(item.getPayloadJson())
                    && candidateUpdatedAt.equals(item.getUpdatedAt());
        }));
    }

    @Test
    @DisplayName("should allow refreshed payloads to clear stale tech stack tags")
    void shouldAllowRefreshedPayloadsToClearStaleTechStackTags() {
        JobPostingEntity existing = JobPostingEntity.builder()
                .id(11L)
                .fingerprintHash("fp-clear-tags")
                .externalId("existing-id")
                .canonicalUrl("https://example.com/jobs/11")
                .title("Sales Executive")
                .company("Acme")
                .description("<p>Updated content</p>")
                .techStackTags("Go")
                .publishedAt(LocalDate.of(2026, 3, 20))
                .payloadJson("{\"old\":true}")
                .build();

        JobPostingEntity candidate = JobPostingEntity.builder()
                .fingerprintHash("fp-clear-tags")
                .externalId("existing-id")
                .canonicalUrl("https://example.com/jobs/11")
                .title("Sales Executive")
                .company("Acme")
                .description("<p>Updated content</p>")
                .techStackTags(null)
                .publishedAt(LocalDate.of(2026, 3, 20))
                .payloadJson("{\"content\":true}")
                .build();

        JobPostingEntity merged = JobPostingEntity.builder()
                .id(11L)
                .fingerprintHash("fp-clear-tags")
                .externalId("existing-id")
                .canonicalUrl("https://example.com/jobs/11")
                .title("Sales Executive")
                .company("Acme")
                .description("<p>Updated content</p>")
                .techStackTags(null)
                .publishedAt(LocalDate.of(2026, 3, 20))
                .payloadJson("{\"content\":true}")
                .build();

        when(jobPostingRepository.findByFingerprintHash("fp-clear-tags")).thenReturn(Optional.of(existing));
        when(jobPostingRepository.saveAll(anyList())).thenReturn(List.of(merged));

        IdempotentJobPostingPersistenceService service = new IdempotentJobPostingPersistenceService(jobPostingRepository);

        List<JobPostingEntity> persisted = service.persist(List.of(candidate));

        assertThat(persisted).containsExactly(merged);
        verify(jobPostingRepository).saveAll(argThat(items -> {
            List<JobPostingEntity> list = StreamSupport.stream(items.spliterator(), false).toList();
            return list.size() == 1 && list.get(0).getTechStackTags() == null;
        }));
    }

    private static JobPostingEntity posting(String fingerprintHash, String externalId) {
        return JobPostingEntity.builder()
                .fingerprintHash(fingerprintHash)
                .externalId(externalId)
                .build();
    }
}
