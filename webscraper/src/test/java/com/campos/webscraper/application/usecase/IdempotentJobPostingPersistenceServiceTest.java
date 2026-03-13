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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
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

    private static JobPostingEntity posting(String fingerprintHash, String externalId) {
        return JobPostingEntity.builder()
                .fingerprintHash(fingerprintHash)
                .externalId(externalId)
                .build();
    }
}
