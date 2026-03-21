# STORY 12.1 — Métricas e Logs Estruturados

**Status:** ✅ Concluída
**Iteration:** 12 — Observabilidade e governança
**Data:** 2026-03-21
**Referência ADR:** ADR009 Story 12.1

---

## Objetivo

Adicionar observabilidade operacional mínima no fluxo de execução assíncrona já consolidado:

- emitir métricas por dispatch;
- emitir métricas por outcome do worker;
- registrar logs estruturados para eventos de dispatch e consumo de fila;
- manter o wiring Spring/IntelliJ simples, via injeção por construtor.

---

## Ciclo TDD

### RED — testes primeiro

Foram escritos testes novos para forçar telemetria em dois pontos críticos:

1. `CircuitBreakingCrawlJobDispatcherTest`
   - exige contador, summary de `items_found` e timer no sucesso do dispatch.
2. `CrawlJobWorkerTest`
   - exige contador para `retry_scheduled` quando o worker reencaminha uma mensagem.

O RED inicial falhou porque não existia nenhum componente emitindo métricas nem logs estruturados.

### GREEN — implementação mínima

Foi criado `CrawlObservabilityService`, responsável por:

- publicar métricas Micrometer para dispatch:
  - `webscraper.crawl.dispatch.total`
  - `webscraper.crawl.dispatch.items_found`
  - `webscraper.crawl.dispatch.duration`
- publicar métrica de worker:
  - `webscraper.crawl.worker.total`
- emitir logs estruturados com chaves estáveis (`event`, `siteCode`, `status`, `queue`, `outcome`, etc.).

O serviço foi injetado por construtor em:

- `CircuitBreakingCrawlJobDispatcher`
- `CrawlJobWorker`

### REFACTOR

- a telemetria ficou centralizada em um único serviço, sem espalhar `MeterRegistry` pela aplicação;
- testes existentes que constroem dispatcher/worker manualmente foram atualizados com
  `SimpleMeterRegistry`;
- `PersistentQueueWorkflowTest` também foi ajustado para refletir o novo contrato de wiring.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/application/orchestrator/CrawlObservabilityService.java` | Criado | Serviço central de métricas e logs estruturados |
| `src/main/java/com/campos/webscraper/application/orchestrator/CircuitBreakingCrawlJobDispatcher.java` | Modificado | Emissão de telemetria por status final do dispatch |
| `src/main/java/com/campos/webscraper/application/orchestrator/CrawlJobWorker.java` | Modificado | Emissão de telemetria por outcome de fila |
| `src/test/java/com/campos/webscraper/application/orchestrator/CircuitBreakingCrawlJobDispatcherTest.java` | Modificado | Teste RED/GREEN para métricas de dispatch |
| `src/test/java/com/campos/webscraper/application/orchestrator/CrawlJobWorkerTest.java` | Modificado | Teste RED/GREEN para métricas do worker |
| `src/test/java/com/campos/webscraper/application/orchestrator/PersistentQueueWorkflowTest.java` | Modificado | Ajuste do novo construtor com observabilidade |
| `docs/stories/STORY-12.1-structured-metrics-and-logs.md` | Criado | Registro final da story |
| `docs/stories/README.md` | Modificado | Atualização do índice |

---

## Problemas encontrados

### Problema 1 — falta de visibilidade operacional

O projeto já tinha:

- fila persistida,
- retry,
- dead-letter,
- worker assíncrono,
- perfis de busca mais honestos.

Mas ainda faltava visibilidade objetiva sobre:

- quantidade de dispatches por status;
- volume de itens encontrados;
- outcomes reais do worker (`done`, `retry`, `dead_letter`, `empty`).

### Problema 2 — risco de espalhar dependência de métrica

Adicionar `MeterRegistry` diretamente em vários componentes aumentaria acoplamento e criaria mais
superfície para quebra de wiring em testes e no IntelliJ.

---

## Causa raiz

- o backlog tinha amadurecido primeiro a parte de execução/resiliência;
- a observabilidade ainda estava só no nível implícito de `CrawlExecutionEntity`;
- faltava uma camada leve de telemetria operacional orientada a runtime.

---

## Solução aplicada

- criado `CrawlObservabilityService` como façade de telemetria;
- métricas Micrometer nomeadas por domínio do projeto;
- logs estruturados com campos estáveis e baixo custo de parse;
- integração por construtor para manter o contrato Spring explícito.

Eventos observados nesta story:

- dispatch `SUCCEEDED`
- dispatch `FAILED`
- dispatch `DEAD_LETTER`
- worker `done`
- worker `retry_scheduled`
- worker `dead_letter_dispatcher`
- worker `dead_letter_resolution`
- worker `dead_letter_disabled_site`
- worker `empty`

---

## Lições aprendidas

- observabilidade útil não precisa começar grande; começar nos pontos onde já existe estado de
  domínio (`dispatch` e `worker`) produz valor imediato;
- `SimpleMeterRegistry` deixa o TDD de métricas barato e rápido;
- concentrar telemetria em um serviço único reduz risco de problemas de DI e ajuda a manter o
  projeto amigável ao IntelliJ/runtime local.

---

## Estado final

- métricas básicas de dispatch e worker implementadas;
- logs estruturados emitidos durante os testes;
- wiring Spring explícito por construtor preservado;
- testes focados verdes.

Validação executada:

- `./mvnw -q -Dtest=CircuitBreakingCrawlJobDispatcherTest,CrawlJobWorkerTest,PersistentQueueWorkflowTest test`
- `./mvnw -q -DskipTests compile`

Próximo passo natural:

- Story 12.3 — endpoint `GET /api/v1/scraper/health`
