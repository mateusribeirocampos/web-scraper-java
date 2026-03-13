# STORY 10.1 — Abstração de fila

**Status:** ✅ Concluída
**Iteration:** 10 — Processamento assíncrono
**Data:** 2026-03-13
**Referência ADR:** ADR009 Story 10.1

---

## Objetivo

Introduzir a primeira abstração explícita de fila para execução assíncrona de crawl jobs:

- contrato producer/consumer mínimo
- nomes de fila por perfil de execução
- implementação em memória para validar a borda antes de broker real

---

## Ciclo TDD

### RED — producer/consumer contract primeiro

Foi criado `CrawlJobQueueTest` cobrindo:

- enfileiramento e consumo FIFO
- roteamento para filas distintas conforme o tipo de execução

O RED inicial falhou por compilação, porque ainda não existiam:

- `CrawlJobQueue`
- `CrawlJobQueueName`
- `EnqueuedCrawlJob`
- `InMemoryCrawlJobQueue`
- `CrawlJobQueueRouter`

### GREEN — implementação mínima

Foi implementado:

1. `CrawlJobQueueName`
2. `EnqueuedCrawlJob`
3. `CrawlJobQueue`
4. `CrawlJobQueueRouter`
5. `InMemoryCrawlJobQueue`

Contrato entregue:

- producer enfileira um `CrawlJobEntity` em uma fila nomeada
- consumer recebe a próxima mensagem disponível daquela fila
- a mensagem de fila carrega snapshot materializado, não entidade JPA viva
- jobs `API` vão para `API_JOBS`
- jobs HTML estáticos vão para `STATIC_SCRAPE_JOBS`
- jobs de browser/dinâmicos vão para `DYNAMIC_BROWSER_JOBS`

### REFACTOR

A implementação ficou em memória de propósito:

- suficiente para validar a borda producer/consumer
- sem acoplar a 10.1 a Redis, broker externo ou threading real

Isso deixa a 10.2 livre para conectar o worker à abstração já testada.

### Ajuste pós-review

Após o review, a implementação foi reforçada em dois pontos:

- `EnqueuedCrawlJob` deixou de carregar `CrawlJobEntity` e passou a transportar apenas metadata materializada segura para borda assíncrona
- `InMemoryCrawlJobQueue` trocou `ArrayDeque` por fila concorrente para preservar o contrato producer/consumer na preparação da 10.2
- o snapshot passou a preservar a categoria efetiva inferida do `TargetSite` quando `CrawlJobEntity.jobCategory` vier nulo
- o roteador de filas voltou a aceitar `CrawlJobEntity` transitória, sem exigir `id` persistido
- o overload `route(EnqueuedCrawlJob)` passou a respeitar a fila já atribuída no envelope, preservando `DEAD_LETTER_JOBS` e futuras reclassificações explícitas
- o envelope passou a aceitar `crawlJobId = null` para suportar enqueue de jobs transitórios antes da persistência

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/application/queue/CrawlJobQueueName.java` | Criado | Nomes das filas de crawl |
| `src/main/java/com/campos/webscraper/application/queue/EnqueuedCrawlJob.java` | Criado | Envelope imutável da mensagem |
| `src/main/java/com/campos/webscraper/application/queue/CrawlJobQueue.java` | Criado | Contrato producer/consumer |
| `src/main/java/com/campos/webscraper/application/queue/CrawlJobQueueRouter.java` | Criado | Roteamento por perfil de execução |
| `src/main/java/com/campos/webscraper/application/queue/InMemoryCrawlJobQueue.java` | Criado | Implementação em memória para a fatia inicial |
| `src/test/java/com/campos/webscraper/application/queue/CrawlJobQueueTest.java` | Criado | RED/GREEN da abstração de fila |
| `docs/stories/STORY-10.1-crawl-job-queue-abstraction.md` | Criado | Registro final da story |
| `docs/stories/README.md` | Modificado | Atualização do índice |

---

## Problemas encontrados

### Problema 1 — o domínio já falava em worker/fila, mas ainda não existia uma borda explícita

Os enums e ADRs já mencionavam `PENDING`, worker e filas nomeadas, porém o código ainda executava
todo o dispatch no mesmo caminho síncrono.

### Problema 2 — a 10.2 precisava herdar um contrato pequeno e estável

Sem uma abstração mínima antes, a próxima story correria risco de misturar worker, queue, runner e
persistência de lifecycle na mesma mudança.

---

## Causa raiz

- as iterações anteriores focaram primeiro em resiliência, scheduler, trigger manual e sources
- o projeto ainda não tinha chegado à etapa de separar de fato o dispatch em producer/consumer

---

## Solução aplicada

- criada a abstração `CrawlJobQueue`
- criadas filas nomeadas por perfil de workload
- implementado roteador de fila por `ExtractionMode`
- validada uma implementação FIFO em memória segura para concorrência

---

## Lições aprendidas

- uma fila em memória é suficiente para estabilizar contrato antes de escolher broker real
- o roteamento por `ExtractionMode` encaixa naturalmente com os conceitos já existentes no domínio
- a 10.2 agora pode focar no worker, não mais na definição da borda da fila

---

## Estado final

- abstração de fila implementada
- producer/consumer mínimo validado
- roteamento de workload por fila implementado
- testes unitários da fatia verdes

Validação executada:

- `./mvnw test -DexcludedGroups=integration -Dtest=CrawlJobQueueTest`

Próximo passo natural:

- Story 10.2 — worker de execução
