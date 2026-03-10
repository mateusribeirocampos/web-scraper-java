# Story 2.2 — CrawlJobEntity + CrawlExecutionEntity + Migration V002

**Status:** Done
**Iteration:** 2 — Domain Model Layer
**Branch:** main
**TDD Cycle:** RED → GREEN ✓

---

## Objetivo

Implementar as entidades `CrawlJobEntity` e `CrawlExecutionEntity` com seus repositórios
Spring Data JPA e a migration Flyway `V002__create_crawl_jobs_and_executions.sql`.

---

## Modelo de Domínio

Seguindo a hierarquia definida no ADR005:

```
TargetSite (1) → CrawlJob (N) → CrawlExecution (N) → JobPosting (N)
                                                     → PublicContestPosting (N)
```

**CrawlJob** — representa um job de scraping agendado para um site-alvo específico.
**CrawlExecution** — representa uma tentativa de execução de um CrawlJob.
Um job pode gerar múltiplas execuções (política de retry: FAILED → nova execução → DEAD_LETTER).

---

## Arquivos Criados

| Arquivo | Tipo |
|---------|------|
| `src/main/java/.../domain/model/CrawlJobEntity.java` | Entidade JPA |
| `src/main/java/.../domain/model/CrawlExecutionEntity.java` | Entidade JPA |
| `src/main/java/.../domain/repository/CrawlJobRepository.java` | Repositório |
| `src/main/java/.../domain/repository/CrawlExecutionRepository.java` | Repositório |
| `src/main/resources/db/migration/V002__create_crawl_jobs_and_executions.sql` | Migration |
| `src/test/java/.../domain/model/CrawlJobEntityTest.java` | Teste unitário |
| `src/test/java/.../domain/model/CrawlExecutionEntityTest.java` | Teste unitário |
| `src/test/java/.../domain/repository/CrawlJobRepositoryTest.java` | Teste integração |

---

## TDD — Fase RED

Testes escritos **antes** das implementações. Falhas esperadas:

```
[ERROR] cannot find symbol: class CrawlJobEntity
[ERROR] cannot find symbol: class CrawlExecutionEntity
[ERROR] cannot find symbol: class CrawlJobRepository
[ERROR] cannot find symbol: class CrawlExecutionRepository
```

---

## TDD — Fase GREEN

### Problema encontrado: enum constants incorretos

Durante a compilação do GREEN, os testes referenciavam constantes inexistentes:

```java
// ERRADO — constantes não existem
JobCategory.TECH_BACKEND
SiteType.JOB_BOARD

// CORRETO — constantes reais do projeto
JobCategory.PRIVATE_SECTOR
SiteType.TYPE_E
```

**Causa:** O enum `JobCategory` foi projetado com foco em *tipo de contratação*
(`PRIVATE_SECTOR` vs `PUBLIC_CONTEST`), não em categoria técnica. O enum `SiteType`
segue nomenclatura TYPE_A..E conforme ADR002 (baseado na estratégia de extração).

**Fix:** `sed -i 's/TECH_BACKEND/PRIVATE_SECTOR/g; s/JOB_BOARD/TYPE_E/g'` nos arquivos de teste.

---

### @Builder.Default em campos primitivos

`CrawlExecutionEntity` usa `@Builder.Default` para `pagesVisited`, `itemsFound` e `retryCount`:

```java
@Column(nullable = false)
@Builder.Default
private int pagesVisited = 0;
```

Isso é necessário para que o Lombok Builder inicialize os campos com 0 quando não definidos.
Sem `@Builder.Default`, o builder usa 0 por padrão (default Java para `int`), mas a anotação
torna a intenção explícita e evita regressões se o tipo mudar para `Integer`.

---

## Migration V002

```sql
CREATE TABLE crawl_jobs (
    id              BIGSERIAL    PRIMARY KEY,
    target_site_id  BIGINT       NOT NULL,
    scheduled_at    TIMESTAMPTZ  NOT NULL,
    job_category    VARCHAR(30),              -- nullable (override opcional)
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_crawl_jobs_target_site
        FOREIGN KEY (target_site_id) REFERENCES target_sites (id)
);

CREATE TABLE crawl_executions (
    id              BIGSERIAL    PRIMARY KEY,
    crawl_job_id    BIGINT       NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    started_at      TIMESTAMPTZ,
    finished_at     TIMESTAMPTZ,
    pages_visited   INTEGER      NOT NULL DEFAULT 0,
    items_found     INTEGER      NOT NULL DEFAULT 0,
    retry_count     INTEGER      NOT NULL DEFAULT 0,
    error_message   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_crawl_executions_crawl_job
        FOREIGN KEY (crawl_job_id) REFERENCES crawl_jobs (id)
);
```

**Índices criados:**

| Tabela | Índice | Coluna | Motivo |
|--------|--------|--------|--------|
| crawl_jobs | idx_crawl_jobs_target_site | target_site_id | FK lookup |
| crawl_jobs | idx_crawl_jobs_scheduled_at | scheduled_at DESC | ordenação |
| crawl_executions | idx_crawl_executions_crawl_job | crawl_job_id | FK lookup |
| crawl_executions | idx_crawl_executions_status | status | filtro por estado |
| crawl_executions | idx_crawl_executions_started | started_at DESC | ordenação |

---

## Validação Manual da Migration

A migration foi aplicada via `psql` contra o banco local para verificar sintaxe e estrutura:

```bash
psql "postgresql://mrc:lamat@localhost:5432/webscraper" \
  -f src/main/resources/db/migration/V002__create_crawl_jobs_and_executions.sql
# CREATE TABLE, CREATE INDEX x5 — sem erros

\d crawl_jobs        # verificado: BIGSERIAL, TIMESTAMPTZ, FK para target_sites
\d crawl_executions  # verificado: defaults corretos (status='PENDING', contadores=0)
```

As tabelas foram removidas após a validação para que o Flyway as recrie na próxima startup.

---

## Resultados dos Testes

### Testes Unitários (sem Docker)

```
Tests run: 109, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Comando:
```bash
./mvnw test -DexcludedGroups="integration"
```

### Testes de Integração (requerem Docker local)

```bash
./mvnw test -Dgroups="integration"
```

Testes cobertos (`CrawlJobRepositoryTest`):
- Persistência de `CrawlJob` com `TargetSite` associado
- `findByTargetSite` — consulta derivada
- Persistência de `CrawlExecution` com status `PENDING`
- `findByStatus` — filtragem por estado
- Persistência de execução `SUCCEEDED` com métricas (`pagesVisited`, `itemsFound`)
- Persistência de execução `FAILED` com `errorMessage`
- `findByCrawlJob` — execuções de um job específico

---

## Ciclo de Vida do Status (CrawlExecutionStatus)

```
PENDING → RUNNING → SUCCEEDED
                  ↘ FAILED → DEAD_LETTER (retentativas esgotadas)
```

Enum criado na Story 1.2 e reutilizado aqui sem modificações.

---

## Fix Adicional: @Tag("integration") em WebscraperApplicationTests

`WebscraperApplicationTests` estava sem `@Tag("integration")`, causando 3 erros
(em vez de 2) ao rodar `./mvnw test -DexcludedGroups="integration"` no sandbox.

**Fix:** adicionado `@Tag("integration")` na classe para consistência com o restante
dos testes que dependem de Docker/Testcontainers.
