# STORY 12.3 — Endpoint de Health Summary

**Status:** ✅ Concluída
**Iteration:** 12 — Observabilidade e governança
**Data:** 2026-03-21
**Referência ADR:** ADR009 Story 12.3

---

## Objetivo

Expor um resumo operacional consumível por API, para que a operação do scraper não dependa mais
de consultas SQL manuais nem inspeção de logs.

O endpoint entregue foi:

- `GET /api/v1/scraper/health`

com três blocos de informação:

- contagens por status de execução;
- contagens por fila/status da fila persistida;
- últimas execuções persistidas.

---

## Ciclo TDD

### RED — testes primeiro

Foram escritos dois testes antes da implementação:

1. `GetScraperHealthSummaryUseCaseTest`
   - exige agregação de contagens por `CrawlExecutionStatus`;
   - exige agregação de contagens por `CrawlJobQueueName` + `QueueMessageStatus`;
   - exige mapeamento das últimas execuções.
2. `ScraperHealthControllerTest`
   - exige o contrato HTTP de `GET /api/v1/scraper/health`.

### GREEN — implementação mínima

Foi implementado:

- `GetScraperHealthSummaryUseCase`
- `ScraperHealthController`
- DTOs de resposta:
  - `ScraperHealthSummaryResponse`
  - `ScraperExecutionStatusCountResponse`
  - `ScraperQueueStatusCountResponse`
  - `RecentCrawlExecutionResponse`

Também foi necessário expandir os repositórios com consultas derivadas simples:

- `CrawlExecutionRepository.countByStatus(...)`
- `CrawlExecutionRepository.findTop10ByOrderByCreatedAtDesc()`
- `PersistentQueueMessageRepository.countByQueueNameAndStatus(...)`

### REFACTOR

- o endpoint ficou fino: só delega ao use case;
- a agregação ficou centralizada no use case;
- o contrato ficou estável e explícito, com listas de contagens em vez de mapas ad hoc.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/application/usecase/GetScraperHealthSummaryUseCase.java` | Criado | Agregação do resumo operacional |
| `src/main/java/com/campos/webscraper/interfaces/rest/ScraperHealthController.java` | Criado | Endpoint `GET /api/v1/scraper/health` |
| `src/main/java/com/campos/webscraper/interfaces/dto/ScraperHealthSummaryResponse.java` | Criado | Payload raiz do resumo |
| `src/main/java/com/campos/webscraper/interfaces/dto/ScraperExecutionStatusCountResponse.java` | Criado | Contagem por status de execução |
| `src/main/java/com/campos/webscraper/interfaces/dto/ScraperQueueStatusCountResponse.java` | Criado | Contagem por fila/status |
| `src/main/java/com/campos/webscraper/interfaces/dto/RecentCrawlExecutionResponse.java` | Criado | Últimas execuções |
| `src/main/java/com/campos/webscraper/domain/repository/CrawlExecutionRepository.java` | Modificado | Queries derivadas para agregação |
| `src/main/java/com/campos/webscraper/domain/repository/PersistentQueueMessageRepository.java` | Modificado | Query derivada para fila |
| `src/test/java/com/campos/webscraper/application/usecase/GetScraperHealthSummaryUseCaseTest.java` | Criado | Teste do use case |
| `src/test/java/com/campos/webscraper/interfaces/rest/ScraperHealthControllerTest.java` | Criado | Teste HTTP do endpoint |
| `docs/stories/STORY-12.3-scraper-health-summary-endpoint.md` | Criado | Registro final da story |
| `docs/stories/README.md` | Modificado | Atualização do índice |

---

## Problemas encontrados

### Problema 1 — observabilidade ainda exigia banco/log

Mesmo com a Story 12.1 concluída, a visão operacional ainda estava fragmentada:

- métricas via Micrometer;
- logs estruturados;
- estado persistido em `crawl_executions` e `persistent_queue_messages`.

Faltava uma superfície única de leitura para operação manual.

### Problema 2 — evitar endpoint “esperto demais”

Era importante não transformar a 12.3 em dashboard completo ou em lógica de monitoramento
complexa demais. O endpoint precisava ser simples, estável e apoiado no estado persistido já
existente.

---

## Causa raiz

- o projeto evoluiu primeiro a execução e a resiliência;
- depois adicionou telemetria;
- só então ficou claro o valor de um resumo HTTP consolidado para operação.

---

## Solução aplicada

- criado `GET /api/v1/scraper/health`;
- resumo baseado no estado persistido que o sistema já mantém;
- últimas execuções limitadas às 10 mais recentes;
- contrato orientado a operação, não a domínio de negócio.

---

## Lições aprendidas

- observabilidade útil vem em camadas:
  1. métricas/logs;
  2. endpoint resumido;
  3. dashboards/alertas depois.
- um endpoint operacional fica mais robusto quando lê o estado persistido que já faz parte do
  desenho da aplicação, em vez de tentar reconstruir tudo a partir de logs.

---

## Estado final

- endpoint `/api/v1/scraper/health` implementado;
- testes focados verdes;
- compilação verde;
- observabilidade operacional básica agora acessível por HTTP.

Validação executada:

- `./mvnw -q -Dtest=GetScraperHealthSummaryUseCaseTest,ScraperHealthControllerTest test`
- `./mvnw -q -Dtest=CircuitBreakingCrawlJobDispatcherTest,CrawlJobWorkerTest,PersistentQueueWorkflowTest,GetScraperHealthSummaryUseCaseTest,ScraperHealthControllerTest test`
- `./mvnw -q -DskipTests compile`

Próximo passo natural:

- Story 12.2 — checklist de habilitação de site em produção
