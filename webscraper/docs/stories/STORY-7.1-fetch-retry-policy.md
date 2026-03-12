# STORY 7.1 — Política de retry

**Status:** ✅ Concluída
**Iteration:** 7 — Baseline de resiliência
**Data:** 2026-03-12
**Referência ADR:** ADR009 Story 7.1

---

## Objetivo

Introduzir a política inicial de retry em torno do contrato `JobFetcher`, distinguindo falhas:

- retryable
- non-retryable

A story fecha a primeira fatia de resiliência com Resilience4j sem acoplar ainda a política ao
roteamento completo das sources.

---

## Ciclo TDD

### RED — testes escritos antes da implementação

Foi criado `RetryableJobFetcherTest` cobrindo:

- retry de falhas transitórias até sucesso
- ausência de retry para falhas non-retryable

As falhas iniciais de compilação foram:

```text
cannot find symbol: class RetryableFetchException
cannot find symbol: class NonRetryableFetchException
```

### GREEN — implementação mínima

Foi implementado o mínimo necessário:

- `RetryableFetchException`
- `NonRetryableFetchException`
- `RetryableJobFetcher`
- `FetchRetryConfiguration`
- ajuste no `HttpJobFetcher` para propagar falhas transitórias como `RetryableFetchException`
- propriedades base de retry em `application.properties`

O `RetryableJobFetcher` usa `Resilience4j Retry` como decorator sobre `JobFetcher`.

### REFACTOR

Sem refatoração estrutural ampla. O `HttpJobFetcher` foi mantido puro, enquanto a política de retry
ficou isolada em decorator e configuração, o que preserva a separação de responsabilidades.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/shared/RetryableFetchException.java` | Criado | Exceção para falhas transitórias elegíveis a retry |
| `src/main/java/com/campos/webscraper/shared/NonRetryableFetchException.java` | Criado | Exceção para falhas que não devem ser repetidas |
| `src/main/java/com/campos/webscraper/infrastructure/http/RetryableJobFetcher.java` | Criado | Decorator Resilience4j sobre `JobFetcher` |
| `src/main/java/com/campos/webscraper/shared/FetchRetryConfiguration.java` | Criado | Beans do retry e do `JobFetcher` resiliente |
| `src/main/resources/application.properties` | Modificado | Defaults de tentativas e intervalo de retry |
| `src/test/java/com/campos/webscraper/infrastructure/http/RetryableJobFetcherTest.java` | Criado | Testes unitários da política de retry |
| `docs/stories/README.md` | Modificado | Atualização do índice de stories |
| `docs/stories/STORY-7.1-fetch-retry-policy.md` | Criado | Registro final da execução da story |

---

## Problemas encontrados

### Problema 1 — O contrato atual de fetch não distinguia explicitamente falha retryable de non-retryable

Sem essa separação, qualquer política de retry ficaria ambígua e tenderia a repetir cenários
incorretos.

### Problema 2 — O `HttpJobFetcher` não deveria absorver sozinho a lógica de resiliência

Misturar transporte HTTP com política de retry dificultaria testes e evolução futura da camada.

---

## Causa raiz

### Problema 1

Até aqui o projeto só tinha o fetch básico, sem um vocabulário explícito para classificar falhas.

### Problema 2

A resiliência da Iteration 7 é uma preocupação transversal e deve ficar fora do adaptador HTTP cru.

---

## Solução aplicada

- criadas exceções específicas para classificar falhas retryable e non-retryable
- implementado `RetryableJobFetcher` com `Retry.decorateSupplier(...)`
- configurado `Retry` do `jobFetcher` com:
  - `maxAttempts = 3`
  - `waitDuration = 200ms`
  - retry somente para `RetryableFetchException`
- registrado um bean `JobFetcher` resiliente no contexto Spring
- alterado o caminho real do `HttpJobFetcher` para converter `IOException` em
  `RetryableFetchException`, tornando o retry observável em produção
- configurado `failAfterMaxAttempts = false` para preservar a exceção retryable original após
  esgotar as tentativas

---

## Lições aprendidas

- a política de retry fica mais previsível quando a classificação da falha é explícita
- decorator + configuração Spring preservam melhor a testabilidade do que embutir retry no fetcher
- a próxima story (`7.2`) pode aplicar rate limiting sobre o mesmo contrato sem retrabalho

---

## Estado final

Resultado da suite unitária validada:

```text
Tests run: 169, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Comando executado:

```bash
./mvnw test -DexcludedGroups=integration
```

Conclusão:

- política inicial de retry implementada
- retry ocorre apenas para falhas classificadas como transitórias
- workspace pronto para review antes de commit/push
