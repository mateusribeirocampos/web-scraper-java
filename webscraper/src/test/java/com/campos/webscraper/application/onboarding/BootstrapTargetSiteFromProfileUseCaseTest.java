package com.campos.webscraper.application.onboarding;

import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import com.campos.webscraper.domain.repository.TargetSiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("BootstrapTargetSiteFromProfileUseCase")
class BootstrapTargetSiteFromProfileUseCaseTest {

    @Mock
    private TargetSiteRepository targetSiteRepository;

    private TargetSiteOnboardingProfileCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new TargetSiteOnboardingProfileCatalog();
    }

    @Test
    @DisplayName("should create new target site from curated profile when site code does not exist")
    void shouldCreateNewTargetSiteFromCuratedProfileWhenSiteCodeDoesNotExist() {
        when(targetSiteRepository.findBySiteCode("greenhouse_bitso")).thenReturn(Optional.empty());
        when(targetSiteRepository.save(any(TargetSiteEntity.class))).thenAnswer(invocation -> {
            TargetSiteEntity candidate = invocation.getArgument(0);
            return TargetSiteEntity.builder()
                    .id(42L)
                    .siteCode(candidate.getSiteCode())
                    .displayName(candidate.getDisplayName())
                    .baseUrl(candidate.getBaseUrl())
                    .siteType(candidate.getSiteType())
                    .extractionMode(candidate.getExtractionMode())
                    .jobCategory(candidate.getJobCategory())
                    .legalStatus(candidate.getLegalStatus())
                    .selectorBundleVersion(candidate.getSelectorBundleVersion())
                    .enabled(candidate.isEnabled())
                    .createdAt(candidate.getCreatedAt())
                    .updatedAt(candidate.getUpdatedAt())
                    .build();
        });

        BootstrapTargetSiteFromProfileUseCase useCase = new BootstrapTargetSiteFromProfileUseCase(
                catalog,
                targetSiteRepository,
                Clock.fixed(Instant.parse("2026-03-24T18:00:00Z"), ZoneOffset.UTC)
        );

        BootstrappedTargetSite result = useCase.execute("greenhouse_bitso");

        assertThat(result.bootstrapStatus()).isEqualTo(BootstrapStatus.CREATED);
        assertThat(result.targetSite().getId()).isEqualTo(42L);
        assertThat(result.targetSite().getSiteCode()).isEqualTo("greenhouse_bitso");
        assertThat(result.targetSite().isEnabled()).isFalse();
        assertThat(result.targetSite().getLegalStatus()).isEqualTo(LegalStatus.PENDING_REVIEW);
        assertThat(result.targetSite().getCreatedAt()).isEqualTo(Instant.parse("2026-03-24T18:00:00Z"));
        assertThat(result.targetSite().getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("should update existing target site from curated profile while preserving existing approval when config is unchanged")
    void shouldUpdateExistingTargetSiteFromCuratedProfileWhilePreservingExistingApprovalWhenConfigIsUnchanged() {
        TargetSiteEntity existing = TargetSiteEntity.builder()
                .id(7L)
                .siteCode("greenhouse_bitso")
                .displayName("Legacy Bitso Name")
                .baseUrl("https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("n/a")
                .enabled(true)
                .createdAt(Instant.parse("2026-03-20T12:00:00Z"))
                .updatedAt(Instant.parse("2026-03-20T12:30:00Z"))
                .build();

        when(targetSiteRepository.findBySiteCode("greenhouse_bitso")).thenReturn(Optional.of(existing));
        when(targetSiteRepository.save(any(TargetSiteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BootstrapTargetSiteFromProfileUseCase useCase = new BootstrapTargetSiteFromProfileUseCase(
                catalog,
                targetSiteRepository,
                Clock.fixed(Instant.parse("2026-03-24T19:00:00Z"), ZoneOffset.UTC)
        );

        BootstrappedTargetSite result = useCase.execute("greenhouse_bitso");

        assertThat(result.bootstrapStatus()).isEqualTo(BootstrapStatus.UPDATED);
        assertThat(result.targetSite().getId()).isEqualTo(7L);
        assertThat(result.targetSite().getDisplayName()).isEqualTo("Bitso Careers via Greenhouse");
        assertThat(result.targetSite().getBaseUrl()).isEqualTo("https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true");
        assertThat(result.targetSite().getSiteType()).isEqualTo(SiteType.TYPE_E);
        assertThat(result.targetSite().getExtractionMode()).isEqualTo(ExtractionMode.API);
        assertThat(result.targetSite().getSelectorBundleVersion()).isEqualTo("n/a");
        assertThat(result.targetSite().getLegalStatus()).isEqualTo(LegalStatus.APPROVED);
        assertThat(result.targetSite().isEnabled()).isTrue();
        assertThat(result.targetSite().getCreatedAt()).isEqualTo(Instant.parse("2026-03-20T12:00:00Z"));
        assertThat(result.targetSite().getUpdatedAt()).isEqualTo(Instant.parse("2026-03-24T19:00:00Z"));

        ArgumentCaptor<TargetSiteEntity> captor = ArgumentCaptor.forClass(TargetSiteEntity.class);
        verify(targetSiteRepository).save(captor.capture());
        assertThat(captor.getValue().getLegalStatus()).isEqualTo(LegalStatus.APPROVED);
        assertThat(captor.getValue().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("should promote an existing site when curated activation changes without runnable config changes")
    void shouldPromoteAnExistingSiteWhenCuratedActivationChangesWithoutRunnableConfigChanges() {
        TargetSiteEntity existing = TargetSiteEntity.builder()
                .id(26L)
                .siteCode("camara_santa_rita_sapucai")
                .displayName("Câmara Municipal de Santa Rita do Sapucaí - Processos Seletivos")
                .baseUrl("https://www.santaritadosapucai.mg.leg.br/transparencia/processos-seletivos-2025")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.PENDING_REVIEW)
                .selectorBundleVersion("camara_santa_rita_html_v1")
                .enabled(false)
                .createdAt(Instant.parse("2026-04-05T12:00:00Z"))
                .updatedAt(Instant.parse("2026-04-05T12:30:00Z"))
                .build();

        when(targetSiteRepository.findBySiteCode("camara_santa_rita_sapucai")).thenReturn(Optional.of(existing));
        when(targetSiteRepository.save(any(TargetSiteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BootstrapTargetSiteFromProfileUseCase useCase = new BootstrapTargetSiteFromProfileUseCase(
                catalog,
                targetSiteRepository,
                Clock.fixed(Instant.parse("2026-04-06T12:00:00Z"), ZoneOffset.UTC)
        );

        BootstrappedTargetSite result = useCase.execute("camara_santa_rita_sapucai");

        assertThat(result.bootstrapStatus()).isEqualTo(BootstrapStatus.UPDATED);
        assertThat(result.targetSite().getLegalStatus()).isEqualTo(LegalStatus.APPROVED);
        assertThat(result.targetSite().isEnabled()).isTrue();
        assertThat(result.targetSite().getUpdatedAt()).isEqualTo(Instant.parse("2026-04-06T12:00:00Z"));
    }

    @Test
    @DisplayName("should preserve existing approval when curated template is still pending review")
    void shouldPreserveExistingApprovalWhenCuratedTemplateIsStillPendingReview() {
        TargetSiteEntity existing = TargetSiteEntity.builder()
                .id(7L)
                .siteCode("greenhouse_bitso")
                .displayName("Legacy Bitso Name")
                .baseUrl("https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("n/a")
                .enabled(true)
                .createdAt(Instant.parse("2026-03-20T12:00:00Z"))
                .updatedAt(Instant.parse("2026-03-20T12:30:00Z"))
                .build();

        when(targetSiteRepository.findBySiteCode("greenhouse_bitso")).thenReturn(Optional.of(existing));
        when(targetSiteRepository.save(any(TargetSiteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BootstrapTargetSiteFromProfileUseCase useCase = new BootstrapTargetSiteFromProfileUseCase(
                catalog,
                targetSiteRepository,
                Clock.fixed(Instant.parse("2026-03-24T19:00:00Z"), ZoneOffset.UTC)
        );

        BootstrappedTargetSite result = useCase.execute("greenhouse_bitso");

        assertThat(result.bootstrapStatus()).isEqualTo(BootstrapStatus.UPDATED);
        assertThat(result.targetSite().getLegalStatus()).isEqualTo(LegalStatus.APPROVED);
        assertThat(result.targetSite().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("should preserve manual disable for an already approved site")
    void shouldPreserveManualDisableForAnAlreadyApprovedSite() {
        TargetSiteEntity existing = TargetSiteEntity.builder()
                .id(26L)
                .siteCode("camara_santa_rita_sapucai")
                .displayName("Câmara Municipal de Santa Rita do Sapucaí - Processos Seletivos")
                .baseUrl("https://www.santaritadosapucai.mg.leg.br/transparencia/processos-seletivos-2025")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("camara_santa_rita_html_v1")
                .enabled(false)
                .createdAt(Instant.parse("2026-04-05T12:00:00Z"))
                .updatedAt(Instant.parse("2026-04-05T12:30:00Z"))
                .build();

        when(targetSiteRepository.findBySiteCode("camara_santa_rita_sapucai")).thenReturn(Optional.of(existing));
        when(targetSiteRepository.save(any(TargetSiteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BootstrapTargetSiteFromProfileUseCase useCase = new BootstrapTargetSiteFromProfileUseCase(
                catalog,
                targetSiteRepository,
                Clock.fixed(Instant.parse("2026-04-06T12:00:00Z"), ZoneOffset.UTC)
        );

        BootstrappedTargetSite result = useCase.execute("camara_santa_rita_sapucai");

        assertThat(result.bootstrapStatus()).isEqualTo(BootstrapStatus.UPDATED);
        assertThat(result.targetSite().getLegalStatus()).isEqualTo(LegalStatus.APPROVED);
        assertThat(result.targetSite().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("should revoke approval when bootstrap changes runnable configuration of an approved site")
    void shouldRevokeApprovalWhenBootstrapChangesRunnableConfigurationOfAnApprovedSite() {
        TargetSiteEntity existing = TargetSiteEntity.builder()
                .id(11L)
                .siteCode("greenhouse_bitso")
                .displayName("Bitso Careers via Greenhouse")
                .baseUrl("https://legacy.example/bitso")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("legacy_bundle")
                .enabled(true)
                .createdAt(Instant.parse("2026-03-20T12:00:00Z"))
                .updatedAt(Instant.parse("2026-03-20T12:30:00Z"))
                .build();

        when(targetSiteRepository.findBySiteCode("greenhouse_bitso")).thenReturn(Optional.of(existing));
        when(targetSiteRepository.save(any(TargetSiteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BootstrapTargetSiteFromProfileUseCase useCase = new BootstrapTargetSiteFromProfileUseCase(
                catalog,
                targetSiteRepository,
                Clock.fixed(Instant.parse("2026-03-24T19:00:00Z"), ZoneOffset.UTC)
        );

        BootstrappedTargetSite result = useCase.execute("greenhouse_bitso");

        assertThat(result.bootstrapStatus()).isEqualTo(BootstrapStatus.UPDATED);
        assertThat(result.targetSite().getLegalStatus()).isEqualTo(LegalStatus.PENDING_REVIEW);
        assertThat(result.targetSite().isEnabled()).isFalse();
        assertThat(result.targetSite().getUpdatedAt()).isEqualTo(Instant.parse("2026-03-24T19:00:00Z"));
    }

    @Test
    @DisplayName("should treat duplicate create race as update by reloading the site code")
    void shouldTreatDuplicateCreateRaceAsUpdateByReloadingTheSiteCode() {
        TargetSiteEntity persisted = TargetSiteEntity.builder()
                .id(42L)
                .siteCode("greenhouse_bitso")
                .displayName("Bitso Careers via Greenhouse")
                .baseUrl("https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.PENDING_REVIEW)
                .selectorBundleVersion("n/a")
                .enabled(false)
                .createdAt(Instant.parse("2026-03-24T19:00:00Z"))
                .updatedAt(null)
                .build();

        when(targetSiteRepository.findBySiteCode("greenhouse_bitso"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(persisted));
        when(targetSiteRepository.save(any(TargetSiteEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        BootstrapTargetSiteFromProfileUseCase useCase = new BootstrapTargetSiteFromProfileUseCase(
                catalog,
                targetSiteRepository,
                Clock.fixed(Instant.parse("2026-03-24T19:00:00Z"), ZoneOffset.UTC)
        );

        BootstrappedTargetSite result = useCase.execute("greenhouse_bitso");

        assertThat(result.bootstrapStatus()).isEqualTo(BootstrapStatus.UPDATED);
        assertThat(result.targetSite().getId()).isEqualTo(42L);
        assertThat(result.targetSite().getSiteCode()).isEqualTo("greenhouse_bitso");
    }

    @Test
    @DisplayName("should revoke approval when bootstrap changes job category")
    void shouldRevokeApprovalWhenBootstrapChangesJobCategory() {
        TargetSiteEntity existing = TargetSiteEntity.builder()
                .id(13L)
                .siteCode("greenhouse_bitso")
                .displayName("Bitso Careers via Greenhouse")
                .baseUrl("https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true")
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.APPROVED)
                .selectorBundleVersion("n/a")
                .enabled(true)
                .createdAt(Instant.parse("2026-03-20T12:00:00Z"))
                .updatedAt(Instant.parse("2026-03-20T12:30:00Z"))
                .build();

        when(targetSiteRepository.findBySiteCode("greenhouse_bitso")).thenReturn(Optional.of(existing));
        when(targetSiteRepository.save(any(TargetSiteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BootstrapTargetSiteFromProfileUseCase useCase = new BootstrapTargetSiteFromProfileUseCase(
                catalog,
                targetSiteRepository,
                Clock.fixed(Instant.parse("2026-03-24T19:00:00Z"), ZoneOffset.UTC)
        );

        BootstrappedTargetSite result = useCase.execute("greenhouse_bitso");

        assertThat(result.bootstrapStatus()).isEqualTo(BootstrapStatus.UPDATED);
        assertThat(result.targetSite().getJobCategory()).isEqualTo(JobCategory.PRIVATE_SECTOR);
        assertThat(result.targetSite().getLegalStatus()).isEqualTo(LegalStatus.PENDING_REVIEW);
        assertThat(result.targetSite().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("should preserve prohibited legal status when runnable configuration changes")
    void shouldPreserveProhibitedLegalStatusWhenRunnableConfigurationChanges() {
        TargetSiteEntity existing = TargetSiteEntity.builder()
                .id(15L)
                .siteCode("greenhouse_bitso")
                .displayName("Bitso Careers via Greenhouse")
                .baseUrl("https://legacy.example/bitso")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PRIVATE_SECTOR)
                .legalStatus(LegalStatus.SCRAPING_PROIBIDO)
                .selectorBundleVersion("legacy_bundle")
                .enabled(false)
                .createdAt(Instant.parse("2026-03-20T12:00:00Z"))
                .updatedAt(Instant.parse("2026-03-20T12:30:00Z"))
                .build();

        when(targetSiteRepository.findBySiteCode("greenhouse_bitso")).thenReturn(Optional.of(existing));
        when(targetSiteRepository.save(any(TargetSiteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BootstrapTargetSiteFromProfileUseCase useCase = new BootstrapTargetSiteFromProfileUseCase(
                catalog,
                targetSiteRepository,
                Clock.fixed(Instant.parse("2026-03-24T19:00:00Z"), ZoneOffset.UTC)
        );

        BootstrappedTargetSite result = useCase.execute("greenhouse_bitso");

        assertThat(result.bootstrapStatus()).isEqualTo(BootstrapStatus.UPDATED);
        assertThat(result.targetSite().getLegalStatus()).isEqualTo(LegalStatus.SCRAPING_PROIBIDO);
        assertThat(result.targetSite().isEnabled()).isFalse();
    }
}
