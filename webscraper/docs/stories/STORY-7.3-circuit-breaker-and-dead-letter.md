# STORY 7.3 — Circuit breaker e dead-letter

**Status:** ✅ Concluída
**Iteration:** 7 — Baseline de resiliência
**Data:** 2026-03-12
**Referência ADR:** ADR009 Story 7.3

---

## Objetivo

Introduzir a primeira proteção de circuit breaker sobre a execução de crawl e o roteamento mínimo
para dead-letter quando a execução estiver bloqueada pelo breaker.

A story fecha o recorte:

- circuit breaker em torno do `CrawlJobExecutionRunner`
- persistência de `DEAD_LETTER`
- roteamento para `DeadLetterQueue`

---

## Ciclo TDD

### RED — testes escritos antes da implementação

Foi criado `CircuitBreakingCrawlJobDispatcherTest` cobrindo:

- `DEAD_LETTER` quando o circuit breaker está aberto
- roteamento para dead-letter com motivo explícito
- preservação de `FAILED` para falhas normais quando o breaker ainda está fechado

A falha inicial de compilação foi:

```text
cannot find symbol: class DeadLetterQueue
```

### GREEN — implementação mínima

Foi implementado o mínimo necessário:

- `DeadLetterQueue`
- `LoggingDeadLetterQueue`
- `CircuitBreakingCrawlJobDispatcher`
- `CircuitBreakerConfiguration`

Após review, o breaker foi corrigido para ser resolvido por fonte (`siteCode`) via registry, e não
como singleton global da aplicação.

Também foi removido o dispatcher anterior sem breaker, porque a nova implementação cobre tanto:

- `FAILED`
- `DEAD_LETTER`

### REFACTOR

O teste anterior do dispatcher persistente foi removido porque sua cobertura funcional foi absorvida
pela nova suíte do dispatcher com circuit breaker.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/application/orchestrator/DeadLetterQueue.java` | Criado | Contrato de roteamento para dead-letter |
| `src/main/java/com/campos/webscraper/application/orchestrator/LoggingDeadLetterQueue.java` | Criado | Implementação temporária de dead-letter baseada em log |
| `src/main/java/com/campos/webscraper/application/orchestrator/CircuitBreakingCrawlJobDispatcher.java` | Criado | Dispatcher com circuit breaker e roteamento para dead-letter |
| `src/main/java/com/campos/webscraper/shared/CircuitBreakerConfiguration.java` | Criado | Bean do circuit breaker de execução |
| `src/main/java/com/campos/webscraper/application/orchestrator/PersistentCrawlJobDispatcher.java` | Removido | Substituído pela versão com circuit breaker |
| `src/test/java/com/campos/webscraper/application/orchestrator/CircuitBreakingCrawlJobDispatcherTest.java` | Criado | Testes unitários de open-state e dead-letter |
| `src/test/java/com/campos/webscraper/application/orchestrator/PersistentCrawlJobDispatcherTest.java` | Removido | Cobertura absorvida pela nova suíte |
| `docs/stories/README.md` | Modificado | Atualização do índice de stories |
| `docs/stories/STORY-7.3-circuit-breaker-and-dead-letter.md` | Criado | Registro final da execução da story |

---

## Problemas encontrados

### Problema 1 — O projeto já persistia `FAILED`, mas ainda não distinguia bloqueio operacional de exaustão normal

Sem o circuit breaker, o sistema continuaria tentando executar jobs mesmo em cenário de fonte
completamente instável.

### Problema 2 — O dead-letter ainda não tinha contrato explícito

O domínio já mencionava `DEAD_LETTER`, mas faltava uma interface mínima de roteamento.

---

## Causa raiz

### Problema 1

As stories 6.1–6.3 focaram no lifecycle operacional básico, não nas proteções de resiliência.

### Problema 2

O pipeline ainda não tinha chegado à etapa de filas reais, então o dead-letter existia apenas como
estado de enum e conceito de ADR.

---

## Solução aplicada

- aplicado `CircuitBreaker.decorateSupplier(...)` em torno do runner
- resolvido `CircuitBreaker` por `siteCode` usando `CircuitBreakerRegistry`
- quando o breaker está aberto:
  - o runner não é chamado
  - a execução é persistida como `DEAD_LETTER`
  - o job é roteado para `DeadLetterQueue`
- quando o breaker está fechado e a execução falha normalmente:
  - a execução continua em `FAILED`
  - não há roteamento para dead-letter

---

## Lições aprendidas

- `FAILED` e `DEAD_LETTER` agora têm papéis operacionais distintos no código, não só no enum
- breaker global para múltiplas fontes é perigoso; o isolamento por fonte precisa ser explícito
- o contrato de dead-letter pode evoluir para fila real sem reabrir o dispatcher
- a próxima iteração pode avançar para abstração de filas e async com base mais sólida

---

## Estado final

Resultado da suite unitária validada:

```text
Tests run: 174, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Comando executado:

```bash
./mvnw test -DexcludedGroups=integration
```

Conclusão:

- circuit breaker básico implementado
- dead-letter mínimo implementado
- workspace pronto para review antes de commit/push
