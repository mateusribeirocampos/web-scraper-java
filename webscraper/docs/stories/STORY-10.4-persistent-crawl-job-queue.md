# STORY 10.4 — PersistentCrawlJobQueue no Postgres

**Status:** 🚧 Em andamento
**Iteration:** 10 — Processamento assíncrono
**Data:** 2026-03-16
**Referência ADR:** ADR009 Story 10.4

---

## Objetivo

Substituir a dependência exclusiva de memória no handoff assíncrono por uma fila durável em tabela
Postgres, preparando claim/ack/retry/dead-letter persistentes.

---

## Ciclo TDD

### RED — storage durável primeiro

Os primeiros testes da story cobrem:

- entidade de mensagem persistente
- estados mínimos do lifecycle da fila
- claim atômico inicial por `queueName + availableAt`

### GREEN — primeira fatia persistente

Implementação desta fatia:

1. `QueueMessageStatus`
2. `PersistentQueueMessageEntity`
3. `PersistentQueueMessageRepository`
4. migration `V007`

Esta primeira fatia ainda não substitui scheduler/worker. Ela cria o storage durável para as
próximas sessões.

### GREEN — segunda fatia persistente

Implementação desta fatia:

1. `PersistentCrawlJobQueue`
2. serialização de `EnqueuedCrawlJob` em `payload_json`
3. `enqueue(CrawlJobEntity, queueName)`
4. `enqueue(EnqueuedCrawlJob, queueName)`
5. `consume(queueName)` via `claimNextReadyMessage(...)`

Esta fatia ainda não troca o wiring de produção. Ela fecha o adaptador persistente antes da
migração de scheduler/worker.

### GREEN — terceira fatia persistente

Implementação desta fatia:

1. `markDone(...)`
2. `scheduleRetry(...)`
3. `moveToDeadLetter(...)`
4. transições restritas a mensagens já `CLAIMED`

Esta fatia fecha o lifecycle persistente básico da mensagem antes da migração de scheduler/worker.

Correção pós-review:

- `RETRY_WAIT` passou a continuar elegível para claim quando `availableAt` vencer
- o retry persistente deixou de criar beco sem saída no lifecycle da fila
- `scheduleRetry(...)` agora atualiza também o `payload_json` materializado
- `updatedAt` do retry passou a registrar o instante real da transição, não o próximo horário de execução

### GREEN — quarta fatia persistente

Implementação desta fatia:

1. `PersistentCrawlJobQueue` promovida a bean primária
2. contrato `CrawlJobQueue` ampliado com `markDone`, `scheduleRetry` e `moveToDeadLetter`
3. `CrawlJobWorker` migrado para lifecycle explícito da fila

Esta fatia conecta scheduler/worker ao contrato persistente da fila, mantendo a implementação em
memória apenas como fallback temporário até a limpeza final.

### REFACTOR

O modelo ficou separado da abstração atual de fila para permitir migração incremental sem quebrar
o fluxo já existente.

---

## Arquivos criados / modificados

- `src/main/java/com/campos/webscraper/domain/enums/QueueMessageStatus.java`
- `src/main/java/com/campos/webscraper/application/queue/PersistentCrawlJobQueue.java`
- `src/main/java/com/campos/webscraper/domain/model/PersistentQueueMessageEntity.java`
- `src/main/java/com/campos/webscraper/domain/repository/PersistentQueueMessageRepository.java`
- `src/main/resources/db/migration/V007__create_persistent_queue_messages.sql`
- `src/test/java/com/campos/webscraper/application/queue/PersistentCrawlJobQueueTest.java`
- `src/test/java/com/campos/webscraper/domain/model/PersistentQueueMessageEntityTest.java`
- `src/test/java/com/campos/webscraper/domain/repository/PersistentQueueMessageRepositoryTest.java`
- `src/test/java/com/campos/webscraper/domain/enums/DomainEnumsTest.java`
- `docs/stories/STORY-10.4-persistent-crawl-job-queue.md`

---

## Problemas encontrados

- a fila atual só existia em memória, então não havia storage durável para iniciar a migração incremental
- era necessário introduzir a estrutura persistente sem quebrar scheduler/worker ainda apoiados na fila atual
- o teste real do repositório continua dependendo do mesmo ambiente Docker/Testcontainers das outras fatias de integração

---

## Causa raiz

- o projeto evoluiu o pipeline assíncrono antes de ter uma fila durável
- faltava um modelo persistente explícito de mensagem para que as próximas stories pudessem implementar claim/ack/retry no banco

---

## Solução aplicada

- criado o enum `QueueMessageStatus` com os estados mínimos do lifecycle persistente
- criada a entidade `PersistentQueueMessageEntity` com payload, disponibilidade, `claimedAt`, `retryCount` e erro
- criado o `PersistentQueueMessageRepository` com claim atômico `READY -> CLAIMED` usando `FOR UPDATE SKIP LOCKED`
- criada a `PersistentCrawlJobQueue` com persistência de envelope em `payload_json` e round-trip `enqueue/consume`
- adicionadas transições persistentes `CLAIMED -> DONE`, `CLAIMED -> RETRY_WAIT` e `CLAIMED -> DEAD_LETTER`
- `PersistentCrawlJobQueue` passou a ser a implementação primária de `CrawlJobQueue`
- `CrawlJobWorker` deixou de reenfileirar manualmente mensagens persistidas e passou a usar `done/retry/dead-letter` pela própria fila
- criada a migration `V007` para materializar a fila durável no Postgres
- adicionada cobertura unitária para entidade, enum e fila persistida, além do teste de integração do repositório

Correção pós-review:

- removido o contrato de leitura simples, que permitia múltiplos consumidores enxergarem a mesma linha `READY`
- a primeira operação pública do repositório passou a ser um claim transacional de uma única mensagem elegível

---

## Lições aprendidas

- a migração segura para fila persistida precisa acontecer em fatias: storage primeiro, orchestration depois
- ter o payload já materializado em tabela reduz o acoplamento com estado efêmero da memória

---

## Estado final

10.4.1, 10.4.2, 10.4.3 e 10.4.4 implementadas e validadas.

Validação executada:

- `./mvnw test -DexcludedGroups=integration -Dtest=PersistentQueueMessageEntityTest,DomainEnumsTest`
- `./mvnw test -DexcludedGroups=integration -Dtest=PersistentCrawlJobQueueTest,PersistentQueueMessageEntityTest,DomainEnumsTest`
- `./mvnw test -DexcludedGroups=integration`

Pendência conhecida:

- `PersistentQueueMessageRepositoryTest` foi criado e a tentativa de execução confirmou que o ambiente
  continua bloqueado pelo Docker/Testcontainers (`client version 1.32 is too old; minimum supported API version is 1.40`)

Próxima fatia planejada:

- 10.4.5 — revisão de simplificação e limpeza
