# STORY 6.1 — Trigger por agendador

**Status:** ✅ Concluída
**Iteration:** 6 — Agendamento e execução manual
**Data:** 2026-03-12
**Referência ADR:** ADR009 Story 6.1

---

## Objetivo

Introduzir o primeiro trigger agendado do projeto para localizar `CrawlJobEntity` elegíveis e
despachar sua execução sem ainda acoplar o scheduler às integrações Indeed/DOU diretamente.

Essa story fecha apenas o recorte:

`clock -> query de jobs habilitados e vencidos -> dispatcher`

---

## Ciclo TDD

### RED — testes escritos antes da implementação

Foi criado `CrawlJobSchedulerTest` cobrindo:

- busca apenas de jobs habilitados e com `scheduledAt <= now`
- ordenação por `scheduledAt` ascendente
- ausência de dispatch quando não houver jobs elegíveis

A falha inicial de compilação foi:

```text
package com.campos.webscraper.application.orchestrator does not exist
cannot find symbol: class CrawlJobDispatcher
```

### GREEN — implementação mínima

Foi implementado o mínimo necessário:

- contrato `CrawlJobDispatcher`
- implementação provisória `LoggingCrawlJobDispatcher`
- query de repositório para jobs elegíveis
- `CrawlJobScheduler` com `@Scheduled`
- `Clock` como bean compartilhado para manter o horário testável
- `@EnableScheduling` no bootstrap da aplicação

### REFACTOR

Sem refatoração estrutural ampla. O dispatcher ficou propositalmente simples para evitar acoplar a
story 6.1 ao use case real de execução, que pertence às próximas stories da iteração.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/application/orchestrator/CrawlJobDispatcher.java` | Criado | Contrato de dispatch reutilizável por scheduler e execução manual |
| `src/main/java/com/campos/webscraper/application/orchestrator/LoggingCrawlJobDispatcher.java` | Criado | Implementação provisória que registra o dispatch no log |
| `src/main/java/com/campos/webscraper/interfaces/scheduler/CrawlJobScheduler.java` | Criado | Trigger agendado de jobs vencidos |
| `src/main/java/com/campos/webscraper/shared/TimeConfiguration.java` | Criado | Bean de `Clock` para produção e testes |
| `src/main/java/com/campos/webscraper/domain/repository/CrawlJobRepository.java` | Modificado | Query derivada para jobs habilitados e vencidos |
| `src/main/java/com/campos/webscraper/WebscraperApplication.java` | Modificado | Habilita scheduling no contexto Spring |
| `src/test/java/com/campos/webscraper/interfaces/scheduler/CrawlJobSchedulerTest.java` | Criado | Testes unitários do trigger do scheduler |
| `docs/stories/README.md` | Modificado | Atualização do índice de stories |
| `docs/stories/STORY-6.1-crawl-job-scheduler.md` | Criado | Registro final da execução da story |

---

## Problemas encontrados

### Problema 1 — O scheduler ainda não tinha um ponto de integração estável para disparar execuções

Sem endpoint manual implementado e sem orquestrador concreto, ligar o scheduler diretamente aos
use cases de importação criaria acoplamento prematuro.

### Problema 2 — O horário do gatilho precisava ser testável

Usar `Instant.now()` diretamente dentro do scheduler tornaria o teste unitário mais frágil e menos
determinístico.

---

## Causa raiz

### Problema 1

A Story 6.1 vem antes das stories de execução manual e lifecycle operacional. O projeto ainda não
tinha um contrato explícito de dispatch.

### Problema 2

O tempo ainda não era tratado como dependência explícita da aplicação.

---

## Solução aplicada

- introduzido `CrawlJobDispatcher` como contrato mínimo de dispatch
- criada implementação temporária `LoggingCrawlJobDispatcher` para manter o contexto Spring válido
- implementado `CrawlJobScheduler` consultando apenas jobs:
  - com `targetSite.enabled = true`
  - com `scheduledAt <= now`
  - ordenados por `scheduledAt asc`
- extraído `Clock` para bean compartilhado

---

## Lições aprendidas

- o scheduler precisa disparar um contrato de aplicação, não conhecer integrações Indeed/DOU
- injetar `Clock` cedo evita testes frágeis em qualquer funcionalidade temporal futura
- a próxima story (`6.2`) já pode reutilizar o mesmo dispatcher para a execução manual

---

## Estado final

Resultado da suite unitária validada:

```text
Tests run: 156, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Comando executado:

```bash
./mvnw test -DexcludedGroups=integration
```

Conclusão:

- scheduler implementado
- jobs elegíveis são consultados e despachados corretamente
- story pronta para a 6.2, que deve reutilizar o contrato `CrawlJobDispatcher`
