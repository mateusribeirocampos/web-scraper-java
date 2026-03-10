# STORY 1.2 — Enums e Value Objects do Domínio

**Iteration:** 1 — Foundation
**Status:** ✅ Concluída
**Data:** 2026-03-10
**Referência ADR:** ADR009 Story 1.2

---

## Objetivo

Completar o conjunto de enums e value objects do domínio definidos no ADR009.
Duas constantes estavam faltando na entrega anterior:

- `DedupStatus` — resultado da avaliação de deduplicação por fingerprint SHA-256
- `CrawlExecutionStatus` — ciclo de vida de uma execução de crawl

Ambas são necessárias para as entidades JPA da Iteration 2
(`JobPostingEntity.dedupStatus`, `CrawlExecutionEntity.status`).

---

## Ciclo TDD

### RED — testes escritos antes da implementação

Adicionadas duas classes `@Nested` ao arquivo existente `DomainEnumsTest.java`:

```java
// DedupStatus — 4 testes
@Test void shouldHaveThreeDedupStatuses()     // hasSize(3)
@Test void newShouldBeFirst()                  // values()[0] == NEW
@Test void shouldContainDuplicate()
@Test void shouldContainUpdated()

// CrawlExecutionStatus — 6 testes
@Test void shouldHaveFiveExecutionStatuses()  // hasSize(5)
@Test void pendingShouldBeFirst()             // values()[0] == PENDING
@Test void shouldContainRunning()
@Test void shouldContainSucceeded()
@Test void shouldContainFailed()
@Test void shouldContainDeadLetter()
```

Os testes foram escritos com os enums ainda inexistentes — a compilação de teste
falha antes mesmo de executar (fase RED garantida por ausência de símbolo).

### GREEN — implementação mínima

Criados os dois arquivos de enum com javadoc completo explicando cada constante.

### REFACTOR

Sem necessidade de refatoração — enums são estruturas simples e já saíram corretos
na primeira passagem.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/.../domain/enums/DedupStatus.java` | Criado | Enum com 3 constantes: `NEW`, `DUPLICATE`, `UPDATED` |
| `src/main/java/.../domain/enums/CrawlExecutionStatus.java` | Criado | Enum com 5 constantes: `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`, `DEAD_LETTER` |
| `src/test/java/.../domain/enums/DomainEnumsTest.java` | Modificado | Adicionados blocos `@Nested` para os dois novos enums |

---

## Problemas encontrados

Nenhum problema de implementação. A story foi direta.

O único ponto de atenção foi verificar se os testes de integração (que dependem
de Docker) poderiam validar os enums — mas enums são `@Tag("unit")` e não
precisam de container.

---

## Design das constantes

### DedupStatus

Controla o comportamento do pipeline de persistência:

| Constante | Significado | Ação no pipeline |
|---|---|---|
| `NEW` | Fingerprint nunca visto | Inserir (`INSERT`) |
| `DUPLICATE` | Fingerprint idêntico já existe | Descartar (sem operação no banco) |
| `UPDATED` | Mesmo `siteCode + externalId`, campo mutável alterado | Atualizar (`UPDATE`) |

A ordem `NEW` primeiro é intencional — é o caso mais frequente em execuções normais.

### CrawlExecutionStatus

Representa as transições de estado de uma `CrawlExecutionEntity`:

```
PENDING → RUNNING → SUCCEEDED
                 ↘ FAILED → ... → DEAD_LETTER
```

| Constante | Significado |
|---|---|
| `PENDING` | Aguardando worker (estado inicial) |
| `RUNNING` | Worker assumiu, crawl em andamento |
| `SUCCEEDED` | Concluída com sucesso, contadores registrados |
| `FAILED` | Falhou mas dentro da política de retry (Resilience4j) |
| `DEAD_LETTER` | Retentativas esgotadas, movida para fila `dead-letter-jobs` |

---

## Estado final

```
Tests run: 102, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Todos os 102 testes unitários verdes, incluindo os 10 novos testes dos enums
`DedupStatus` e `CrawlExecutionStatus`.
