# STORY 2.4 — PublicContestPostingEntity + Deduplicação + Migration V005

**Status:** ✅ Concluída
**Iteration:** 2 — Modelo de Persistência
**Data:** 2026-03-12
**Referência ADR:** ADR009 Story 2.4 / ADR005 Seção 3

---

## Objetivo

Implementar o agregado persistido de concursos públicos do projeto, incluindo:

- `PublicContestPostingEntity`
- `PublicContestPostingRepository`
- `ContestPostingFingerprintCalculator`
- migration `V005__create_public_contest_postings.sql`

Essa story fecha a camada de persistência para concursos e prepara o projeto para a futura
integração API-first com a DOU API.

---

## Ciclo TDD

### RED — testes escritos antes da implementação

Foram criados três testes cobrindo o checklist da story:

- `PublicContestPostingEntityTest`
- `PublicContestPostingRepositoryTest`
- `ContestPostingFingerprintCalculatorTest`

As falhas iniciais foram de compilação, como esperado:

```text
cannot find symbol: class PublicContestPostingEntity
cannot find symbol: class PublicContestPostingRepository
cannot find symbol: class ContestPostingFingerprintCalculator
```

Isso garantiu a fase RED antes de qualquer código de produção.

### GREEN — implementação mínima

Foi implementado o mínimo necessário para a fatia passar:

1. `PublicContestPostingEntity` com relacionamentos para `CrawlExecutionEntity` e `TargetSiteEntity`
2. `PublicContestPostingRepository` com queries derivadas para:
   - `findByFingerprintHash`
   - `findByContestStatusAndRegistrationEndDateGreaterThanEqualOrderByRegistrationEndDateAsc`
3. `ContestPostingFingerprintCalculator` com SHA-256 sobre campos canônicos normalizados
4. migration `V005__create_public_contest_postings.sql` com FKs e índices alinhados ao ADR005

### REFACTOR

Os ajustes de refactor foram pequenos e locais:

- manter consistência entre nomes de colunas da entidade e da migration
- isolar a regra de fingerprint em `shared`
- preservar mapeamento de enums com `EnumType.STRING`
- manter `registrationEndDate` como campo central de consulta para concursos abertos

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/domain/model/PublicContestPostingEntity.java` | Criado | Entidade JPA de concursos públicos |
| `src/main/java/com/campos/webscraper/domain/repository/PublicContestPostingRepository.java` | Criado | Repositório Spring Data para consultas de concursos |
| `src/main/java/com/campos/webscraper/shared/ContestPostingFingerprintCalculator.java` | Criado | Cálculo determinístico de fingerprint SHA-256 para concursos |
| `src/main/resources/db/migration/V005__create_public_contest_postings.sql` | Criado | DDL da tabela `public_contest_postings`, FKs e índices |
| `src/test/java/com/campos/webscraper/domain/model/PublicContestPostingEntityTest.java` | Criado | Testes unitários da entidade |
| `src/test/java/com/campos/webscraper/domain/repository/PublicContestPostingRepositoryTest.java` | Criado | Teste de integração JPA/Flyway com Testcontainers |
| `src/test/java/com/campos/webscraper/shared/ContestPostingFingerprintCalculatorTest.java` | Criado | Testes unitários da regra de fingerprint |
| `docs/stories/README.md` | Modificado | Atualização do índice de stories |
| `docs/stories/STORY-2.4-public-contest-posting-entity.md` | Criado | Registro final da execução da story |

---

## Problemas encontrados

### Problema 1 — Teste de integração bloqueado por Docker/Testcontainers

**Sintoma:**

```text
client version 1.32 is too old. Minimum supported API version is 1.40
Could not find a valid Docker environment
```

### Problema 2 — Divergência entre ADR e enums reais do código

O ADR descreve nomes conceituais para alguns enums, mas o código vigente usa:

- `GovernmentLevel.AUTARCHY`
- `ContestStatus.REGISTRATION_CLOSED`
- `ContestStatus.RESULT_PUBLISHED`

Os testes e a implementação precisaram seguir o código atual para manter consistência do projeto.

---

## Causa raiz

### Problema 1

O ambiente de execução continua sem um Docker compatível com o Testcontainers do projeto.
O daemon/proxy expõe API `1.32`, abaixo do mínimo suportado (`1.40`).

### Problema 2

Os ADRs descrevem a direção arquitetural, mas a fonte de verdade para compilar e manter o projeto
é o código já consolidado. A story precisava aderir ao domínio existente, não redefini-lo.

---

## Solução aplicada

### Para a implementação da story

- criada a entidade `PublicContestPostingEntity` com:
  - `publishedAt` obrigatório
  - `registrationEndDate` para consultas de concurso aberto
  - `contestStatus`, `governmentLevel`, `educationLevel`
  - `fingerprintHash` e `dedupStatus`
  - relacionamentos com `crawl_execution_id` e `target_site_id`
- criada a migration `V005` com:
  - tabela `public_contest_postings`
  - FKs para `crawl_executions` e `target_sites`
  - índices para `published_at`, `registration_end_date`, `target_site_id + external_id`,
    `fingerprint_hash`, `government_level + state`
- criado `ContestPostingFingerprintCalculator` com:
  - normalização por `trim`
  - normalização de `case`
  - fallback `no-deadline` quando `registrationEndDate` é nulo
  - hash SHA-256 sobre campos canônicos

### Para a validação

- suite unitária executada com:

```bash
./mvnw test -DexcludedGroups="integration"
```

- tentativa explícita de executar o teste de integração da story:

```bash
./mvnw test -Dgroups="integration" -Dtest=PublicContestPostingRepositoryTest
```

O teste ficou implementado e compilando, mas a execução depende de um ambiente Docker compatível.

---

## Lições aprendidas

- a persistência de concursos segue o mesmo padrão saudável da Story 2.3, o que reduz atrito
  para as próximas integrações API-first
- `registrationEndDate` é o campo de consulta mais importante para concursos e precisava estar
  coberto desde a primeira versão da entidade
- isolar a regra de fingerprint também aqui mantém o modelo de domínio limpo e previsível

---

## Estado final

Resultado da suite unitária viável no ambiente atual:

```text
Tests run: 129, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Comando executado:

```bash
./mvnw test -DexcludedGroups="integration"
```

Resultado da tentativa de integração:

- `PublicContestPostingRepositoryTest` criado e compilando
- execução bloqueada por incompatibilidade do ambiente Docker/Testcontainers

Conclusão:

- implementação da Story 2.4 concluída
- testes unitários verdes
- teste de integração pronto, pendente apenas de ambiente Docker compatível
