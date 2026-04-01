# ADR 005 — Modelo de Domínio JPA e Persistência para o WebScraper de Vagas

## Title

Modelo de domínio, entidades JPA e direção de repositórios para vagas de emprego (setor privado)
e concursos públicos brasileiros, com suporte a filtragem e ordenação por data de publicação.

## Status

Accepted

## Date

2026-03-09

## Context

A plataforma precisa persistir metadados de sites-alvo, estado de execução de jobs, registros
extraídos, snapshots brutos e versões de parsers. A persistência deve suportar auditabilidade,
deduplicação, recuperação e disponibilização de vagas **ordenadas e filtradas por data**.

O domínio central é agora concreto:
- `JobPosting` — vaga do setor privado (empresa + cargo + tecnologia + localização + salário + data).
- `PublicContestPosting` — concurso público (órgão + cargo + nível + vagas + prazo + edital).

Ambos compartilham campos comuns mas têm especificidades por categoria.

---

## Decision

### 1. Agregados Principais

```text
TargetSite
 ├── ScraperProfile
 ├── SelectorBundle
 └── CrawlJob
      ├── CrawlExecution
      ├── RawSnapshot
      └── JobPosting  (setor privado)
           └── JobPostingFieldAudit
      └── PublicContestPosting  (concurso público)
           └── ContestPostingFieldAudit
```

---

### 2. Entidade `JobPosting` — Vaga Setor Privado

Representa uma vaga de emprego em empresa privada extraída de portais como Indeed, Vagas.com,
portal Gupy, etc.

```java
@Entity
@Table(
    name = "job_postings",
    indexes = {
        @Index(name = "idx_job_published_at", columnList = "published_at DESC"),
        @Index(name = "idx_job_site_external", columnList = "target_site_id, external_id"),
        @Index(name = "idx_job_fingerprint", columnList = "fingerprint_hash"),
        @Index(name = "idx_job_seniority_stack", columnList = "seniority, tech_stack_tags")
    }
)
public class JobPostingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "crawl_execution_id")
    private CrawlExecutionEntity crawlExecution;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "target_site_id")
    private TargetSiteEntity targetSite;

    // === Identificação ===

    @Column(length = 255)
    private String externalId;            // ID no site de origem

    @Column(nullable = false, length = 1000)
    private String canonicalUrl;          // URL canônica da vaga

    // === Dados da Vaga ===

    @Column(nullable = false, length = 500)
    private String title;                 // ex.: "Desenvolvedor Java Júnior"

    @Column(nullable = false, length = 300)
    private String company;               // ex.: "Invillia", "Nubank"

    @Column(length = 300)
    private String location;              // ex.: "São Paulo, SP" ou "Remoto"

    @Column(nullable = false)
    private boolean remote;               // true se for vaga remota

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private JobContractType contractType; // CLT, PJ, INTERNSHIP, APPRENTICE, TEMPORARY

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SeniorityLevel seniority;     // JUNIOR, MID, SENIOR, LEAD, NOT_SPECIFIED

    @Column(length = 500)
    private String salaryRange;           // ex.: "R$ 3.000 - R$ 5.000" (texto livre)

    @Column(length = 1000)
    private String techStackTags;         // ex.: "Java,Spring Boot,PostgreSQL,Docker"

    @Column(columnDefinition = "text")
    private String description;           // Descrição completa da vaga

    // === Datas — campos críticos para filtragem e ordenação ===

    @Column(nullable = false)
    private LocalDate publishedAt;        // Data de publicação da vaga (OBRIGATÓRIO)

    @Column
    private LocalDate applicationDeadline; // Prazo para candidatura (quando informado)

    // === Controle de Deduplicação ===

    @Column(nullable = false, length = 128)
    private String fingerprintHash;       // SHA-256 de campos canonicais

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DedupStatus dedupStatus;      // NEW, DUPLICATE, UPDATED

    // === Metadados de Extração ===

    @Column(columnDefinition = "jsonb")
    private String payloadJson;           // Payload bruto preservado para auditoria

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;
}
```

**Enums associados:**

```java
public enum JobContractType {
    CLT, PJ, INTERNSHIP, APPRENTICE, TEMPORARY, NOT_SPECIFIED
}

public enum SeniorityLevel {
    JUNIOR, MID, SENIOR, LEAD, NOT_SPECIFIED
}
```

---

### 3. Entidade `PublicContestPosting` — Concurso Público

Representa um concurso público com edital, prazo de inscrição, número de vagas e dados específicos
da administração pública brasileira.

```java
@Entity
@Table(
    name = "public_contest_postings",
    indexes = {
        @Index(name = "idx_contest_published_at", columnList = "published_at DESC"),
        @Index(name = "idx_contest_registration_end", columnList = "registration_end_date"),
        @Index(name = "idx_contest_site_external", columnList = "target_site_id, external_id"),
        @Index(name = "idx_contest_fingerprint", columnList = "fingerprint_hash"),
        @Index(name = "idx_contest_gov_level_state", columnList = "government_level, state")
    }
)
public class PublicContestPostingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "crawl_execution_id")
    private CrawlExecutionEntity crawlExecution;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "target_site_id")
    private TargetSiteEntity targetSite;

    // === Identificação ===

    @Column(length = 255)
    private String externalId;

    @Column(nullable = false, length = 1000)
    private String canonicalUrl;

    // === Dados do Concurso ===

    @Column(nullable = false, length = 500)
    private String contestName;           // ex.: "Concurso Público SERPRO 2026"

    @Column(nullable = false, length = 300)
    private String organizer;             // ex.: "SERPRO", "INSS", "Prefeitura de SP"

    @Column(nullable = false, length = 300)
    private String positionTitle;         // ex.: "Analista de TI — Desenvolvimento de Sistemas"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GovernmentLevel governmentLevel; // FEDERAL, ESTADUAL, MUNICIPAL, AUTARQUIA

    @Column(length = 2)
    private String state;                 // ex.: "SP", "RJ", "DF"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EducationLevel educationLevel;   // FUNDAMENTAL, MEDIO, SUPERIOR, POS_GRADUACAO

    @Column
    private Integer numberOfVacancies;    // Número de vagas

    @Column(precision = 10, scale = 2)
    private BigDecimal baseSalary;        // Salário-base em BRL

    @Column(length = 500)
    private String salaryDescription;     // Texto livre quando não há valor exato

    @Column(length = 1000)
    private String editalUrl;             // URL direta para o edital

    // === Datas — campos críticos para filtragem ===

    @Column(nullable = false)
    private LocalDate publishedAt;        // Data de publicação do edital (OBRIGATÓRIO)

    @Column
    private LocalDate registrationStartDate;  // Início das inscrições

    @Column
    private LocalDate registrationEndDate;    // Prazo final de inscrições (campo mais consultado)

    @Column
    private LocalDate examDate;               // Data prevista da prova

    // === Status do Concurso ===

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContestStatus contestStatus;  // OPEN, CLOSED, RESULTS_PUBLISHED, CANCELLED

    // === Controle de Deduplicação ===

    @Column(nullable = false, length = 128)
    private String fingerprintHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DedupStatus dedupStatus;

    // === Metadados de Extração ===

    @Column(columnDefinition = "jsonb")
    private String payloadJson;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;
}
```

**Enums associados:**

```java
public enum GovernmentLevel {
    FEDERAL, ESTADUAL, MUNICIPAL, AUTARQUIA, EMPRESA_PUBLICA
}

public enum EducationLevel {
    FUNDAMENTAL, MEDIO, TECNICO, SUPERIOR, POS_GRADUACAO, NOT_SPECIFIED
}

public enum ContestStatus {
    OPEN,                // Inscrições abertas
    CLOSED,              // Inscrições encerradas
    EXAM_SCHEDULED,      // Prova agendada
    RESULTS_PUBLISHED,   // Resultado divulgado
    CANCELLED            // Concurso cancelado
}
```

---

### 4. Entidades de Infraestrutura (sem alteração significativa)

#### `TargetSiteEntity`

Campos adicionais relevantes para o domínio de vagas:

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 30)
private JobCategory jobCategory;  // PRIVATE_SECTOR, PUBLIC_CONTEST, BOTH

@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 30)
private LegalStatus legalStatus;  // APPROVED, PENDING_REVIEW, PROHIBITED

@Column(nullable = false, length = 50)
private String selectorBundleVersion;
```

#### `CrawlJob`, `CrawlExecution`, `RawSnapshot`

Sem alterações estruturais em relação ao ADR anterior. Mantidos para auditoria e rastreabilidade.

---

### 5. Repositories

```java
public interface JobPostingRepository extends JpaRepository<JobPostingEntity, Long> {

    // Consultas por data de publicação — caso de uso principal
    List<JobPostingEntity> findByPublishedAtBetweenOrderByPublishedAtDesc(
        LocalDate start, LocalDate end);

    Page<JobPostingEntity> findByPublishedAtGreaterThanEqualOrderByPublishedAtDesc(
        LocalDate since, Pageable pageable);

    // Deduplicação
    Optional<JobPostingEntity> findByFingerprintHash(String hash);

    boolean existsByTargetSiteIdAndExternalId(Long siteId, String externalId);

    // Filtros de domínio
    Page<JobPostingEntity> findBySeniorityAndTechStackTagsContainingIgnoreCase(
        SeniorityLevel seniority, String tech, Pageable pageable);
}

public interface PublicContestPostingRepository
    extends JpaRepository<PublicContestPostingEntity, Long> {

    // Concursos com inscrições abertas, ordenados por prazo crescente
    List<PublicContestPostingEntity> findByContestStatusAndRegistrationEndDateGreaterThanEqualOrderByRegistrationEndDateAsc(
        ContestStatus status, LocalDate today);

    // Por data de publicação
    Page<PublicContestPostingEntity> findByPublishedAtGreaterThanEqualOrderByPublishedAtDesc(
        LocalDate since, Pageable pageable);

    // Deduplicação
    Optional<PublicContestPostingEntity> findByFingerprintHash(String hash);
}
```

---

### 6. Cálculo de Fingerprint (Deduplicação)

O fingerprint é calculado a partir de campos de negócio estáveis para evitar duplicatas entre
execuções diferentes.

Para `JobPosting`:
```java
// SHA-256 de: siteCode + externalId + title + company + publishedAt
public static String computeFingerprint(String siteCode, String externalId,
                                         String title, String company,
                                         LocalDate publishedAt) {
    String raw = siteCode + "|" + externalId + "|"
        + title.trim().toLowerCase() + "|"
        + company.trim().toLowerCase() + "|"
        + publishedAt.toString();
    return DigestUtils.sha256Hex(raw);
}
```

Para `PublicContestPosting`:
```java
// SHA-256 de: siteCode + externalId + contestName + organizer + registrationEndDate
public static String computeFingerprint(String siteCode, String externalId,
                                         String contestName, String organizer,
                                         LocalDate registrationEndDate) {
    String raw = siteCode + "|" + externalId + "|"
        + contestName.trim().toLowerCase() + "|"
        + organizer.trim().toLowerCase() + "|"
        + (registrationEndDate != null ? registrationEndDate : "no-deadline");
    return DigestUtils.sha256Hex(raw);
}
```

---

### 7. Migrations Flyway

Estrutura de migrations sugerida:

```
db/migration/
  V001__create_target_sites.sql
  V002__create_crawl_jobs_and_executions.sql
  V004__create_job_postings.sql
  V005__create_public_contest_postings.sql
  V006__create_selector_bundles.sql
  V007__add_indexes_job_published_at.sql
  V010__create_raw_snapshots.sql
```

Observação:

- `RawSnapshot` foi introduzido depois da sequência inicial de migrations da fase 1.
- Por isso, a materialização real no repositório segue `V010__create_raw_snapshots.sql`, e não
  uma inserção retroativa antes de migrations já publicadas.

---

### 8. Regra TDD para Persistência

Funcionalidades de persistência devem começar com:

1. Teste de mapeamento de entidade.
2. Teste de integração de repositório com Testcontainers (PostgreSQL real).
3. Teste de regra de deduplicação.
4. Teste de consulta por data (`publishedAt`, `registrationEndDate`).
5. Somente então: código de mapeamento/serviço de produção.

---

## Consequences

### Benefícios

- Modelo de domínio específico ao contexto de vagas — sem campos genéricos de "produto".
- `publishedAt` como campo mandatório garante que toda vaga seja disponibilizável por data.
- `fingerprintHash` com algoritmo explícito garante deduplicação determinística.
- Suporte nativo a concursos públicos com prazo de inscrição e status do concurso.
- Indexes otimizados para os casos de uso mais frequentes (filtro por data, status aberto).

### Desafios

- Crescimento de snapshots pode ser caro sem política de retenção.
- Campos `text` e `jsonb` sem esquema interno podem ocultar drift se não monitorados.
- Concursos com inscrições reabertas precisam de lógica de atualização, não apenas inserção.

## Next Steps

1. Definir migrations Flyway a partir deste modelo.
2. Adicionar política de retenção para snapshots e logs de execução.
3. Implementar testes de repositório com Testcontainers primeiro.
4. Implementar endpoint de listagem de vagas ordenadas por `publishedAt DESC`.

## References

- ADR001 — Direção tecnológica
- ADR002 — Taxonomia de sites e análise legal
- ADR004 — Arquitetura de extração
- ADR006 — Resiliência e processamento assíncrono
- ADR009 — Plano de entregas XP
