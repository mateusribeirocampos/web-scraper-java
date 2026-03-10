-- =============================================================================
-- V001 — Tabela target_sites
--
-- Representa cada site-alvo de scraping ou API cadastrado na plataforma.
-- Cada registro define a estratégia de extração, categoria de vagas e status
-- legal que governa se o site pode ser ativado em produção (ADR002).
--
-- Regra de onboarding: legal_status deve ser 'APPROVED' antes de enabled = true.
-- =============================================================================

CREATE TABLE target_sites (
    id                     BIGSERIAL PRIMARY KEY,

    -- Identificação imutável do site
    site_code              VARCHAR(100)  NOT NULL,
    display_name           VARCHAR(200)  NOT NULL,
    base_url               VARCHAR(1000) NOT NULL,

    -- Estratégia de extração (ver ADR002 e ADR004)
    site_type              VARCHAR(20)   NOT NULL,   -- SiteType enum: TYPE_A..TYPE_E
    extraction_mode        VARCHAR(30)   NOT NULL,   -- ExtractionMode enum: API, STATIC_HTML, ...

    -- Classificação de domínio
    job_category           VARCHAR(30)   NOT NULL,   -- JobCategory enum: PRIVATE_SECTOR, PUBLIC_CONTEST
    legal_status           VARCHAR(30)   NOT NULL,   -- LegalStatus enum: APPROVED, PENDING_REVIEW, SCRAPING_PROIBIDO

    -- Configuração de parser
    selector_bundle_version VARCHAR(50)  NOT NULL DEFAULT 'n/a',  -- ex: "pci_concursos_v1"; "n/a" para fontes API

    -- Estado operacional
    enabled                BOOLEAN       NOT NULL DEFAULT false,   -- false até checklist de onboarding concluído

    -- Auditoria
    created_at             TIMESTAMPTZ   NOT NULL,
    updated_at             TIMESTAMPTZ,

    CONSTRAINT uq_target_sites_site_code UNIQUE (site_code)
);

-- Índice para a query mais comum: buscar sites ativos de uma categoria
CREATE INDEX idx_target_sites_enabled_category
    ON target_sites (enabled, job_category);

-- Índice para busca por status legal (checklist de compliance)
CREATE INDEX idx_target_sites_legal_status
    ON target_sites (legal_status);

-- Comentários de coluna para documentar o schema no banco
COMMENT ON TABLE  target_sites IS
    'Sites-alvo de scraping/API. Cada registro representa uma fonte de vagas ou concursos públicos.';

COMMENT ON COLUMN target_sites.site_code IS
    'Código único e imutável do site (ex: indeed_br, pci_concursos). Usado como chave de negócio.';

COMMENT ON COLUMN target_sites.site_type IS
    'Classificação técnica do site (TYPE_A..TYPE_E). Determina a JobScraperStrategy usada (ADR004).';

COMMENT ON COLUMN target_sites.legal_status IS
    'Status de revisão legal e robots.txt/ToS. Somente APPROVED permite enabled = true em produção (ADR002).';

COMMENT ON COLUMN target_sites.selector_bundle_version IS
    'Versão ativa do SelectorBundle para este site. Usa ''n/a'' para fontes que consomem API oficial.';

COMMENT ON COLUMN target_sites.enabled IS
    'false até o checklist de onboarding de ADR002 estar completo e legal_status = APPROVED.';
