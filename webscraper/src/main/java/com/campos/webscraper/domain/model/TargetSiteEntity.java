package com.campos.webscraper.domain.model;

import com.campos.webscraper.domain.enums.ExtractionMode;
import com.campos.webscraper.domain.enums.JobCategory;
import com.campos.webscraper.domain.enums.LegalStatus;
import com.campos.webscraper.domain.enums.SiteType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entidade JPA que representa um site-alvo de scraping ou API cadastrado na plataforma.
 *
 * <p>Cada {@code TargetSiteEntity} define:
 * <ul>
 *   <li>A estratégia técnica de extração ({@code siteType} + {@code extractionMode}).</li>
 *   <li>A categoria de vagas produzidas ({@code jobCategory}).</li>
 *   <li>O status legal que autoriza (ou bloqueia) a ativação em produção ({@code legalStatus}).</li>
 * </ul>
 *
 * <p>Regra de onboarding (ADR002): {@code legalStatus} deve ser {@code APPROVED}
 * antes de {@code enabled = true} em produção.
 *
 * <p>Persistida na tabela {@code target_sites} — criada pela migration V001.
 */
@Entity
@Table(
        name = "target_sites",
        indexes = {
                @Index(name = "idx_target_sites_enabled_category", columnList = "enabled, job_category"),
                @Index(name = "idx_target_sites_legal_status",     columnList = "legal_status")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetSiteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    // Identificação
    // =========================================================================

    /**
     * Código único e imutável do site.
     * Usado como chave de negócio — ex.: {@code "indeed_br"}, {@code "pci_concursos"}.
     */
    @Column(name = "site_code", nullable = false, unique = true, length = 100)
    private String siteCode;

    /** Nome de exibição legível — ex.: {@code "Indeed Brasil"}, {@code "PCI Concursos"}. */
    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    /** URL base do site — ex.: {@code "https://br.indeed.com"}. */
    @Column(name = "base_url", nullable = false, length = 1000)
    private String baseUrl;

    // =========================================================================
    // Estratégia de extração (ADR002 + ADR004)
    // =========================================================================

    /**
     * Classificação técnica do site (TYPE_A a TYPE_E).
     * Determina qual {@code JobScraperStrategy} a {@code JobScraperFactory} vai resolver.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "site_type", nullable = false, length = 20)
    private SiteType siteType;

    /**
     * Modo de extração ativo para este site.
     * Refina a estratégia: {@code API}, {@code STATIC_HTML}, {@code DYNAMIC_HTML} ou
     * {@code BROWSER_AUTOMATION}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_mode", nullable = false, length = 30)
    private ExtractionMode extractionMode;

    // =========================================================================
    // Classificação de domínio
    // =========================================================================

    /**
     * Categoria de vagas produzidas por este site.
     * {@code PRIVATE_SECTOR} para portais de emprego privado;
     * {@code PUBLIC_CONTEST} para fontes de concursos públicos.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "job_category", nullable = false, length = 30)
    private JobCategory jobCategory;

    /**
     * Status de revisão legal e de robots.txt/ToS (ADR002).
     * Somente {@code APPROVED} permite {@code enabled = true} em produção.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "legal_status", nullable = false, length = 30)
    private LegalStatus legalStatus;

    // =========================================================================
    // Configuração de parser
    // =========================================================================

    /**
     * Versão do {@code SelectorBundle} ativo para este site.
     * Usa {@code "n/a"} para fontes que consomem API oficial (TYPE_E).
     * Ex.: {@code "pci_concursos_v1"}.
     */
    @Column(name = "selector_bundle_version", nullable = false, length = 50)
    private String selectorBundleVersion;

    // =========================================================================
    // Estado operacional
    // =========================================================================

    /**
     * Indica se o site está ativo para execuções agendadas.
     * {@code false} até o checklist de onboarding de ADR002 estar completo.
     */
    @Column(nullable = false)
    private boolean enabled;

    // =========================================================================
    // Auditoria
    // =========================================================================

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
