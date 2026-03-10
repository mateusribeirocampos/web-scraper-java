package com.campos.webscraper.domain.repository;

import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import com.campos.webscraper.domain.model.TargetSiteEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de integração do repositório {@link TargetSiteRepository}.
 *
 * <p>Usa Testcontainers com PostgreSQL real para garantir que o mapeamento JPA
 * e a migration V001 estão corretos.
 *
 * <p>Nota: Spring Boot 4.x removeu @DataJpaTest e @AutoConfigureTestDatabase.
 * Usamos @SpringBootTest(webEnvironment = NONE) com @ServiceConnection para
 * obter um contexto JPA completo com PostgreSQL real via Testcontainers.
 *
 * <p>Ciclo TDD: estes testes foram escritos ANTES das classes de produção (fase RED).
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=none"
})
@DisplayName("TargetSiteRepository — integração com PostgreSQL via Testcontainers")
class TargetSiteRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private TargetSiteRepository repository;

    // =========================================================================
    // Helpers
    // =========================================================================

    private TargetSiteEntity buildSite(String siteCode,
                                       LegalStatus legalStatus,
                                       JobCategory jobCategory,
                                       boolean enabled) {
        return TargetSiteEntity.builder()
                .siteCode(siteCode)
                .displayName("Test — " + siteCode)
                .baseUrl("https://test.example.com/" + siteCode)
                .siteType(SiteType.TYPE_E)
                .extractionMode(ExtractionMode.API)
                .jobCategory(jobCategory)
                .legalStatus(legalStatus)
                .selectorBundleVersion("n/a")
                .enabled(enabled)
                .createdAt(Instant.now())
                .build();
    }

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    // =========================================================================
    // Persistência básica
    // =========================================================================

    @Test
    @DisplayName("deve salvar e recuperar TargetSiteEntity pelo ID gerado")
    void shouldSaveAndFindById() {
        TargetSiteEntity site = buildSite("indeed_br", LegalStatus.APPROVED,
                JobCategory.PRIVATE_SECTOR, true);

        TargetSiteEntity saved = repository.save(site);

        assertThat(saved.getId()).isNotNull();
        Optional<TargetSiteEntity> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getSiteCode()).isEqualTo("indeed_br");
        assertThat(found.get().getLegalStatus()).isEqualTo(LegalStatus.APPROVED);
        assertThat(found.get().isEnabled()).isTrue();
    }

    // =========================================================================
    // findBySiteCode
    // =========================================================================

    @Test
    @DisplayName("findBySiteCode deve retornar o site quando siteCode existe")
    void shouldFindBySiteCode() {
        repository.save(buildSite("pci_concursos", LegalStatus.APPROVED,
                JobCategory.PUBLIC_CONTEST, true));

        Optional<TargetSiteEntity> found = repository.findBySiteCode("pci_concursos");

        assertThat(found).isPresent();
        assertThat(found.get().getDisplayName()).isEqualTo("Test — pci_concursos");
    }

    @Test
    @DisplayName("findBySiteCode deve retornar Optional.empty para siteCode inexistente")
    void shouldReturnEmptyForUnknownSiteCode() {
        Optional<TargetSiteEntity> result = repository.findBySiteCode("nao_existe");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("deve rejeitar siteCode duplicado — UNIQUE constraint na coluna site_code")
    void shouldRejectDuplicateSiteCode() {
        repository.save(buildSite("indeed_br", LegalStatus.APPROVED,
                JobCategory.PRIVATE_SECTOR, true));

        TargetSiteEntity duplicate = buildSite("indeed_br", LegalStatus.PENDING_REVIEW,
                JobCategory.PRIVATE_SECTOR, false);

        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(Exception.class);
    }

    // =========================================================================
    // findByEnabledTrueAndJobCategory
    // =========================================================================

    @Test
    @DisplayName("findByEnabledTrueAndJobCategory deve retornar apenas sites ativos da categoria")
    void shouldFindEnabledSitesByCategory() {
        repository.save(buildSite("indeed_br",     LegalStatus.APPROVED, JobCategory.PRIVATE_SECTOR, true));
        repository.save(buildSite("vagas_com",     LegalStatus.APPROVED, JobCategory.PRIVATE_SECTOR, true));
        repository.save(buildSite("pci_concursos", LegalStatus.APPROVED, JobCategory.PUBLIC_CONTEST, true));
        repository.save(buildSite("gupy_disabled", LegalStatus.APPROVED, JobCategory.PRIVATE_SECTOR, false));

        List<TargetSiteEntity> result =
                repository.findByEnabledTrueAndJobCategory(JobCategory.PRIVATE_SECTOR);

        assertThat(result).hasSize(2)
                .extracting(TargetSiteEntity::getSiteCode)
                .containsExactlyInAnyOrder("indeed_br", "vagas_com");
    }

    @Test
    @DisplayName("findByEnabledTrueAndJobCategory deve retornar lista vazia quando nao ha sites ativos")
    void shouldReturnEmptyWhenNoEnabledSitesForCategory() {
        repository.save(buildSite("pci_concursos", LegalStatus.APPROVED,
                JobCategory.PUBLIC_CONTEST, true));

        List<TargetSiteEntity> result =
                repository.findByEnabledTrueAndJobCategory(JobCategory.PRIVATE_SECTOR);

        assertThat(result).isEmpty();
    }

    // =========================================================================
    // findByEnabledTrueAndLegalStatus
    // =========================================================================

    @Test
    @DisplayName("findByEnabledTrueAndLegalStatus deve retornar apenas sites ativos e aprovados")
    void shouldFindEnabledApprovedSites() {
        repository.save(buildSite("indeed_br",  LegalStatus.APPROVED,          JobCategory.PRIVATE_SECTOR, true));
        repository.save(buildSite("dou_api",    LegalStatus.APPROVED,          JobCategory.PUBLIC_CONTEST, true));
        repository.save(buildSite("catho",      LegalStatus.SCRAPING_PROIBIDO, JobCategory.PRIVATE_SECTOR, false));
        repository.save(buildSite("vagas_com",  LegalStatus.PENDING_REVIEW,    JobCategory.PRIVATE_SECTOR, false));

        List<TargetSiteEntity> result =
                repository.findByEnabledTrueAndLegalStatus(LegalStatus.APPROVED);

        assertThat(result).hasSize(2)
                .extracting(TargetSiteEntity::getSiteCode)
                .containsExactlyInAnyOrder("indeed_br", "dou_api");
    }

    // =========================================================================
    // Enums persistidos como STRING
    // =========================================================================

    @Test
    @DisplayName("deve persistir e recuperar todos os enums como STRING (nao ordinal)")
    void shouldPersistAndRetrieveEnumsAsString() {
        TargetSiteEntity site = TargetSiteEntity.builder()
                .siteCode("enum_test_site")
                .displayName("Enum Test")
                .baseUrl("https://test.example.com/enum")
                .siteType(SiteType.TYPE_A)
                .extractionMode(ExtractionMode.STATIC_HTML)
                .jobCategory(JobCategory.PUBLIC_CONTEST)
                .legalStatus(LegalStatus.PENDING_REVIEW)
                .selectorBundleVersion("pci_concursos_v1")
                .enabled(false)
                .createdAt(Instant.now())
                .build();

        TargetSiteEntity saved = repository.saveAndFlush(site);

        TargetSiteEntity reloaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getSiteType()).isEqualTo(SiteType.TYPE_A);
        assertThat(reloaded.getExtractionMode()).isEqualTo(ExtractionMode.STATIC_HTML);
        assertThat(reloaded.getJobCategory()).isEqualTo(JobCategory.PUBLIC_CONTEST);
        assertThat(reloaded.getLegalStatus()).isEqualTo(LegalStatus.PENDING_REVIEW);
    }
}
