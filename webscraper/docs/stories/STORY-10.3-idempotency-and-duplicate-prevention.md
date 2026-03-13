# STORY 10.3 — Idempotência e prevenção de duplicatas

**Status:** 🚧 Em andamento
**Iteration:** 10 — Processamento assíncrono
**Data:** 2026-03-13
**Referência ADR:** ADR009 Story 10.3

---

## Objetivo

Introduzir a primeira camada real de idempotência do pipeline assíncrono para que execuções
repetidas não criem novas linhas em `job_postings` e `public_contest_postings` quando o
`fingerprintHash` já existir.

---

## Ciclo TDD

### RED — persistência duplicada primeiro

Os primeiros testes da story cobrem:

- repostagem privada com `fingerprintHash` já persistido
- repostagem de concurso com `fingerprintHash` já persistido
- repetição do mesmo `fingerprintHash` dentro do mesmo batch de scrape
- preservação da ordem do resultado retornado ao caller
- reenqueue do mesmo `CrawlJob` recorrente em ticks consecutivos do scheduler

O objetivo do RED é impedir `saveAll` cego nos imports já existentes.

### GREEN — primeira fatia idempotente

Implementação desta fatia:

1. `IdempotentJobPostingPersistenceService`
2. `IdempotentPublicContestPersistenceService`
3. integração desses serviços aos imports `Indeed`, `Greenhouse`, `DOU` e `PCI`
4. `InFlightCrawlJobRegistry` em memória entre scheduler e worker

Escopo deliberadamente escolhido:

- evitar novas linhas para fingerprints já conhecidos
- evitar duplicatas também dentro do mesmo lote ainda não persistido
- manter o runner/worker contando itens processados sem exigir handoff durável
- absorver duplicações vindas de reenqueue/handoff in-memory na camada de persistência
- impedir reenfileiramento infinito do mesmo `CrawlJob` recorrente dentro do mesmo processo

### REFACTOR

Regras de idempotência ficaram centralizadas em dois serviços pequenos, um por agregado
persistente, evitando duplicação entre use cases.

---

## Arquivos criados / modificados

- `src/main/java/com/campos/webscraper/application/usecase/IdempotentJobPostingPersistenceService.java`
- `src/main/java/com/campos/webscraper/application/usecase/IdempotentPublicContestPersistenceService.java`
- `src/main/java/com/campos/webscraper/application/usecase/IndeedJobImportUseCase.java`
- `src/main/java/com/campos/webscraper/application/usecase/GreenhouseJobImportUseCase.java`
- `src/main/java/com/campos/webscraper/application/usecase/DouContestImportUseCase.java`
- `src/main/java/com/campos/webscraper/application/usecase/PciConcursosImportUseCase.java`
- `src/main/java/com/campos/webscraper/application/orchestrator/CrawlJobWorker.java`
- `src/main/java/com/campos/webscraper/application/queue/InFlightCrawlJobRegistry.java`
- `src/main/java/com/campos/webscraper/application/queue/InMemoryInFlightCrawlJobRegistry.java`
- `src/main/java/com/campos/webscraper/interfaces/scheduler/CrawlJobScheduler.java`
- `src/test/java/com/campos/webscraper/application/usecase/IdempotentJobPostingPersistenceServiceTest.java`
- `src/test/java/com/campos/webscraper/application/usecase/IdempotentPublicContestPersistenceServiceTest.java`
- `src/test/java/com/campos/webscraper/application/orchestrator/CrawlJobWorkerTest.java`
- `src/test/java/com/campos/webscraper/interfaces/scheduler/CrawlJobSchedulerTest.java`

---

## Problemas encontrados

- o primeiro corte ainda permitia duplicata intra-batch quando o fingerprint não existia no banco
- o worker assíncrono podia executar mensagens já enfileiradas mesmo depois de o site ter sido desabilitado
- o scheduler em modo assíncrono podia reenfileirar o mesmo job vencido em todo tick enquanto ele ainda estivesse pendente

---

## Causa raiz

- o lookup inicial só comparava contra estado persistido, não contra fingerprints já vistos no mesmo lote em memória
- a introdução da fila criou atraso entre enqueue e execução, mas o worker não revalidava o `enabled` atual do `TargetSite`
- a troca de `dispatch` síncrono por `enqueue` rápido removeu o “bloqueio natural” que antes impedia múltiplos handoffs do mesmo job no mesmo processo

---

## Solução aplicada

- a persistência idempotente passou a registrar também o primeiro item novo por `fingerprintHash` dentro do próprio batch
- repetições no mesmo lote agora retornam o mesmo objeto salvo/reencontrado, sem novo `saveAll`
- o worker passou a bloquear execução quando o `TargetSite` estiver atualmente `enabled = false`, movendo a mensagem para `DEAD_LETTER_JOBS`
- o scheduler agora tenta fazer `claim` do `crawlJobId` antes do enqueue
- o worker libera esse `claim` em sucesso, dead-letter e encerramento final sem retry
- enquanto a mensagem estiver em retry/backoff, o claim permanece ativo para evitar reenfileiramento duplicado do mesmo job

---

## Lições aprendidas

- idempotência real em pipeline assíncrono precisa cobrir banco e lote em memória
- o stop operacional de uma fonte deve ser revalidado no consumer, não só no momento do enqueue
- em fila in-memory, o controle correto de duplicidade de handoff precisa ser explicitamente separado de durabilidade

---

## Estado final

Primeira fatia da 10.3 implementada e validada, incluindo idempotência de persistência e claim
em memória para handoff do scheduler.

## Bloqueio arquitetural para próximas sessões

Esta story **não elimina** o problema estrutural de durabilidade do handoff `scheduler -> queue`.

Estado real após a 10.3:

- duplicatas de persistência agora estão mitigadas por `fingerprintHash`
- reenfileiramento infinito do mesmo job dentro do mesmo processo está mitigado por
  `InFlightCrawlJobRegistry`
- porém o sistema ainda depende de `InMemoryCrawlJobQueue`, então restart/crash pode perder fila
  pendente e o claim em memória não sobrevive ao processo

Decisão registrada para continuidade do projeto:

- antes de seguir outras tasks funcionais, abrir e fechar uma story técnica de **fila persistida ou
  outbox no Postgres**
- a recomendação atual é implementar primeiro uma fila persistida no banco do próprio projeto,
  porque reduz complexidade operacional em relação a broker externo
- até isso existir, qualquer evolução posterior carrega risco operacional conhecido e deve assumir
  esse limite explicitamente
