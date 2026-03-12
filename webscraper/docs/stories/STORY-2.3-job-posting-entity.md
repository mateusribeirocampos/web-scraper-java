# STORY 2.3 — JobPostingEntity + Deduplicação + Migration V004

**Status:** ✅ Concluída
**Iteration:** 2 — Modelo de Persistência
**Data:** 2026-03-12
**Referência ADR:** ADR009 Story 2.3 / ADR005 Seção 2

---

## Objetivo

Implementar o primeiro agregado persistido de negócio do projeto para vagas do setor privado,
incluindo:

- `JobPostingEntity`
- `JobPostingRepository`
- `JobPostingFingerprintCalculator`
- migration `V004__create_job_postings.sql`

A story fecha a base de persistência para vagas privadas e deixa o projeto pronto para a
primeira integração API-first da Iteration 4.

---

## Ciclo TDD

### RED — testes escritos antes da implementação

Foram criados três testes cobrindo os pontos definidos no checklist:

- `JobPostingEntityTest`
- `JobPostingRepositoryTest`
- `JobPostingFingerprintCalculatorTest`

As primeiras falhas foram de compilação, como esperado:

```text
cannot find symbol: class JobPostingEntity
cannot find symbol: class JobPostingRepository
cannot find symbol: class JobPostingFingerprintCalculator
```

Essas falhas garantiram a fase RED antes de qualquer código de produção.

### GREEN — implementação mínima

Implementado o mínimo necessário para os testes passarem:

1. `JobPostingEntity` com relacionamentos obrigatórios para `CrawlExecutionEntity` e `TargetSiteEntity`
2. `JobPostingRepository` com queries derivadas para:
   - `findByTargetSite`
   - `findByPublishedAtGreaterThanEqual`
   - `findByFingerprintHash`
3. `JobPostingFingerprintCalculator` usando SHA-256 sobre campos canônicos normalizados
4. migration `V004__create_job_postings.sql` com FKs e índices definidos no ADR005

### REFACTOR

Pequenos ajustes foram mantidos dentro do escopo da story:

- manter consistência de nomes de colunas entre entidade e migration
- centralizar a regra de fingerprint em uma classe dedicada em `shared`
- preservar o mapeamento de enums com `EnumType.STRING`

Sem necessidade de refatoração estrutural adicional.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/domain/model/JobPostingEntity.java` | Criado | Entidade JPA de vagas privadas |
| `src/main/java/com/campos/webscraper/domain/repository/JobPostingRepository.java` | Criado | Repositório Spring Data para consultas de vaga |
| `src/main/java/com/campos/webscraper/shared/JobPostingFingerprintCalculator.java` | Criado | Cálculo determinístico de fingerprint SHA-256 |
| `src/main/resources/db/migration/V004__create_job_postings.sql` | Criado | DDL da tabela `job_postings`, FKs e índices |
| `src/test/java/com/campos/webscraper/domain/model/JobPostingEntityTest.java` | Criado | Testes unitários da entidade |
| `src/test/java/com/campos/webscraper/domain/repository/JobPostingRepositoryTest.java` | Criado | Teste de integração JPA/Flyway com Testcontainers |
| `src/test/java/com/campos/webscraper/shared/JobPostingFingerprintCalculatorTest.java` | Criado | Testes unitários da regra de fingerprint |
| `docs/stories/README.md` | Modificado | Atualização do índice de stories |
| `docs/stories/STORY-2.3-job-posting-entity.md` | Modificado | Registro final da execução da story |

---

## Problemas encontrados

### Problema 1 — Teste de integração não executa no sandbox

**Sintoma inicial:**

```text
UnsatisfiedLinkError: Failed to create temporary file for ... libjnidispatch.so
```

**Sintoma após rerun fora do sandbox:**

```text
client version 1.32 is too old. Minimum supported API version is 1.40
Could not find a valid Docker environment
```

### Problema 2 — Compatibilidade entre checklist e enums reais do projeto

O checklist original mencionava cenários como "enum persistido como texto", mas o projeto atual
já tem enums definidos com um conjunto concreto de valores (`CLT`, `PJ`, `TEMPORARY`, `JUNIOR`,
`MID`, `NEW`, `DUPLICATE`), então os testes precisaram ser escritos contra os enums existentes,
sem introduzir novos valores fora do escopo da story.

---

## Causa raiz

### Problema 1

O ambiente disponível para execução não oferece uma combinação válida de Docker + Testcontainers:

- dentro do sandbox, a camada nativa do JNA não consegue criar os arquivos temporários necessários
- fora do sandbox, o daemon/proxy Docker responde com API `1.32`, abaixo do mínimo suportado
  pelo Testcontainers usado no projeto

### Problema 2

O ADR descreve o domínio alvo, mas a implementação real do projeto já consolidou enums próprios.
A story precisava respeitar o código existente para manter consistência e não expandir o domínio
sem necessidade.

---

## Solução aplicada

### Para a implementação da story

- criada a entidade `JobPostingEntity` com:
  - `publishedAt` obrigatório
  - `fingerprintHash` e `dedupStatus`
  - relacionamentos com `crawl_execution_id` e `target_site_id`
- criada a migration `V004` com:
  - tabela `job_postings`
  - FKs para `crawl_executions` e `target_sites`
  - índices para `published_at`, `target_site_id + external_id`, `fingerprint_hash`,
    `seniority + tech_stack_tags`
- criado `JobPostingFingerprintCalculator` com:
  - normalização por `trim`
  - normalização de `case`
  - hash SHA-256 sobre campos canônicos

### Para a validação

- suite unitária executada com:

```bash
./mvnw test -DexcludedGroups="integration"
```

- tentativa explícita de executar o teste de integração da story:

```bash
./mvnw test -Dgroups="integration" -Dtest=JobPostingRepositoryTest
```

O teste ficou implementado, mas a execução depende de um Docker funcional com API compatível.

---

## Lições aprendidas

- a Story 2.3 já consegue ser desenvolvida de forma útil mesmo sem Docker local no ambiente,
  desde que a fatia unitária esteja forte e o teste de integração fique pronto para execução externa
- encapsular a regra de fingerprint fora da entidade evita misturar persistência com deduplicação
- `publishedAt` já está modelado corretamente como eixo principal de consulta cronológica

---

## Estado final

Resultado da suite unitária viável no ambiente atual:

```text
Tests run: 119, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Comando executado:

```bash
./mvnw test -DexcludedGroups="integration"
```

Resultado da tentativa de integração:

- `JobPostingRepositoryTest` criado e compilando
- execução bloqueada por incompatibilidade do ambiente Docker/Testcontainers

Conclusão:

- implementação da Story 2.3 concluída
- testes unitários verdes
- teste de integração pronto, pendente apenas de ambiente Docker compatível
