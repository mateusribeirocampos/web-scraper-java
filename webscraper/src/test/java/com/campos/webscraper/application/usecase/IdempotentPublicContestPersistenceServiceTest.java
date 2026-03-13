package com.campos.webscraper.application.usecase;

import com.campos.webscraper.domain.model.PublicContestPostingEntity;
import com.campos.webscraper.domain.repository.PublicContestPostingRepository;
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
@DisplayName("IdempotentPublicContestPersistenceService")
class IdempotentPublicContestPersistenceServiceTest {

    @Mock
    private PublicContestPostingRepository publicContestPostingRepository;

    @Test
    @DisplayName("should skip duplicate contest fingerprints and preserve result order")
    void shouldSkipDuplicateContestFingerprintsAndPreserveResultOrder() {
        PublicContestPostingEntity existing = contest("fp-existing", "contest-existing");
        PublicContestPostingEntity duplicateCandidate = contest("fp-existing", "contest-duplicate");
        PublicContestPostingEntity newCandidate = contest("fp-new", "contest-new");
        PublicContestPostingEntity savedNew = contest("fp-new", "contest-new");

        when(publicContestPostingRepository.findByFingerprintHash("fp-existing")).thenReturn(Optional.of(existing));
        when(publicContestPostingRepository.findByFingerprintHash("fp-new")).thenReturn(Optional.empty());
        when(publicContestPostingRepository.saveAll(anyList())).thenReturn(List.of(savedNew));

        IdempotentPublicContestPersistenceService service =
                new IdempotentPublicContestPersistenceService(publicContestPostingRepository);

        List<PublicContestPostingEntity> persisted = service.persist(List.of(duplicateCandidate, newCandidate));

        assertThat(persisted).containsExactly(existing, savedNew);
        verify(publicContestPostingRepository).saveAll(List.of(newCandidate));
    }

    @Test
    @DisplayName("should deduplicate repeated new contest fingerprints inside the same batch")
    void shouldDeduplicateRepeatedNewContestFingerprintsInsideTheSameBatch() {
        PublicContestPostingEntity firstCandidate = contest("fp-new", "contest-a");
        PublicContestPostingEntity duplicateCandidate = contest("fp-new", "contest-b");
        PublicContestPostingEntity savedNew = contest("fp-new", "contest-a");

        when(publicContestPostingRepository.findByFingerprintHash("fp-new")).thenReturn(Optional.empty());
        when(publicContestPostingRepository.saveAll(anyList())).thenReturn(List.of(savedNew));

        IdempotentPublicContestPersistenceService service =
                new IdempotentPublicContestPersistenceService(publicContestPostingRepository);

        List<PublicContestPostingEntity> persisted = service.persist(List.of(firstCandidate, duplicateCandidate));

        assertThat(persisted).containsExactly(savedNew, savedNew);
        verify(publicContestPostingRepository).saveAll(List.of(firstCandidate));
    }

    private static PublicContestPostingEntity contest(String fingerprintHash, String externalId) {
        return PublicContestPostingEntity.builder()
                .fingerprintHash(fingerprintHash)
                .externalId(externalId)
                .build();
    }
}
