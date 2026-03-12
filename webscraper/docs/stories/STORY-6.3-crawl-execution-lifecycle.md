# STORY 6.3 — Log de execução

**Status:** ✅ Concluída
**Iteration:** 6 — Agendamento e execução manual
**Data:** 2026-03-12
**Referência ADR:** ADR009 Story 6.3

---

## Objetivo

Persistir o ciclo de vida operacional de uma execução de crawl com:

- `RUNNING`
- `SUCCEEDED`
- `FAILED`
- `pagesVisited`
- `itemsFound`
- `errorMessage`

A story fecha o fluxo de logging operacional em torno do `CrawlJobDispatcher`.

---

## Ciclo TDD

### RED — testes escritos antes da implementação

Foi criado `PersistentCrawlJobDispatcherTest` cobrindo:

- criação inicial da execução em `RUNNING`
- finalização em `SUCCEEDED` com contadores retornados pelo runner
- finalização em `FAILED` com mensagem de erro quando o runner lança exceção

A falha inicial de compilação foi:

```text
cannot find symbol: class CrawlJobExecutionRunner
```

### GREEN — implementação mínima

Foi implementado o mínimo necessário:

- `CrawlExecutionOutcome`
- contrato `CrawlJobExecutionRunner`
- `PersistentCrawlJobDispatcher`
- `NoOpCrawlJobExecutionRunner` como runner padrão temporário

O fluxo final ficou:

`dispatch -> save RUNNING -> run -> save SUCCEEDED/FAILED`

### REFACTOR

O dispatcher provisório baseado apenas em log foi removido. A responsabilidade passou a ficar
concentrada em uma implementação única que persiste o lifecycle e delega o trabalho real a um
contrato separado.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/application/orchestrator/CrawlExecutionOutcome.java` | Criado | Value object de contadores da execução |
| `src/main/java/com/campos/webscraper/application/orchestrator/CrawlJobExecutionRunner.java` | Criado | Contrato para executar um `CrawlJob` e devolver contadores |
| `src/main/java/com/campos/webscraper/application/orchestrator/NoOpCrawlJobExecutionRunner.java` | Criado | Runner padrão temporário para manter o pipeline operacional |
| `src/main/java/com/campos/webscraper/application/orchestrator/PersistentCrawlJobDispatcher.java` | Criado | Dispatcher que persiste status e contadores |
| `src/main/java/com/campos/webscraper/application/orchestrator/LoggingCrawlJobDispatcher.java` | Removido | Implementação provisória substituída pelo dispatcher persistente |
| `src/test/java/com/campos/webscraper/application/orchestrator/PersistentCrawlJobDispatcherTest.java` | Criado | Testes unitários do lifecycle de execução |
| `docs/stories/README.md` | Modificado | Atualização do índice de stories |
| `docs/stories/STORY-6.3-crawl-execution-lifecycle.md` | Criado | Registro final da execução da story |

---

## Problemas encontrados

### Problema 1 — O dispatcher anterior não persistia nada

O projeto já tinha trigger agendado e execução manual, mas o `dispatch` apenas registrava log e não
gerava `CrawlExecutionEntity`.

### Problema 2 — O lifecycle precisava registrar contadores sem acoplar o dispatcher ao scraper real

Ligar o dispatcher diretamente às integrações Indeed/DOU nesta story criaria um salto de escopo.

---

## Causa raiz

### Problema 1

A Story 6.1 foi propositalmente mínima para destravar scheduler e endpoint manual.

### Problema 2

O projeto ainda não tem um orquestrador completo que resolva source, strategy e persistência ponta
a ponta a partir de um `CrawlJobEntity`.

---

## Solução aplicada

- introduzido `CrawlJobExecutionRunner` como contrato de trabalho real
- criado `PersistentCrawlJobDispatcher` para:
  - abrir execução em `RUNNING`
  - persistir `startedAt`
  - registrar `pagesVisited` e `itemsFound`
  - fechar em `SUCCEEDED` ou `FAILED`
- mantido `NoOpCrawlJobExecutionRunner` como implementação padrão temporária, retornando `0/0`
  até a próxima fatia de execução concreta

---

## Lições aprendidas

- scheduler e endpoint manual agora compartilham não só o contrato de dispatch, mas também o mesmo
  lifecycle persistente
- separar `dispatcher` de `runner` evita acoplamento e prepara melhor a execução real das fontes
- a próxima story já pode integrar execução concreta sem reabrir o modelo operacional de status

---

## Estado final

Resultado da suite unitária validada:

```text
Tests run: 162, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Comando executado:

```bash
./mvnw test -DexcludedGroups=integration
```

Conclusão:

- lifecycle de execução persistido
- status `RUNNING/SUCCEEDED/FAILED` e contadores já ficam registrados
- story pronta para a 6.4 ou para a próxima fatia de execução concreta
