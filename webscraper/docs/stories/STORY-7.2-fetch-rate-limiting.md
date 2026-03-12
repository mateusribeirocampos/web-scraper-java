# STORY 7.2 — Rate limiting

**Status:** ✅ Concluída
**Iteration:** 7 — Baseline de resiliência
**Data:** 2026-03-12
**Referência ADR:** ADR009 Story 7.2

---

## Objetivo

Introduzir o baseline de rate limiting por site no contrato `JobFetcher`, garantindo que:

- cada fonte use sua própria chave lógica de limitação
- requisições acima do limite sejam negadas antes do transporte HTTP
- não exista comportamento “sem limite” por default no contexto Spring

---

## Ciclo TDD

### RED — testes escritos antes da implementação

Foram criados os testes:

- `RateLimitedJobFetcherTest`
- extensão de `FetchRequestTest`

Cobertura inicial:

- requisição permitida quando há token disponível
- negação imediata quando o limite do site já foi consumido
- resolução de `siteKey` explícito no `FetchRequest`

As primeiras falhas vieram da ausência de:

```text
RateLimitedJobFetcher
RateLimitDeniedException
siteKey em FetchRequest
```

### GREEN — implementação mínima

Foi implementado o mínimo necessário:

- `siteKey` opcional em `FetchRequest`
- `rateLimitKey()` com fallback para host da URL
- `RateLimitDeniedException`
- `RateLimitedJobFetcher`
- `FetchRateLimitConfiguration`
- composição do bean real:
  `HttpJobFetcher -> RateLimitedJobFetcher -> RetryableJobFetcher`

### REFACTOR

Sem refatoração estrutural ampla. O rate limiting ficou isolado em decorator, mantendo o
`HttpJobFetcher` simples e reutilizável.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/shared/RateLimitDeniedException.java` | Criado | Exceção para negação de rate limiter |
| `src/main/java/com/campos/webscraper/infrastructure/http/RateLimitedJobFetcher.java` | Criado | Decorator Resilience4j de rate limiting |
| `src/main/java/com/campos/webscraper/shared/FetchRateLimitConfiguration.java` | Criado | Beans de `RateLimiterRegistry` e composição do `JobFetcher` real |
| `src/main/java/com/campos/webscraper/shared/FetchRequest.java` | Modificado | Adiciona `siteKey` e `rateLimitKey()` |
| `src/main/java/com/campos/webscraper/shared/FetchRetryConfiguration.java` | Modificado | Passa a fornecer apenas o bean de retry |
| `src/main/resources/application.properties` | Modificado | Defaults do rate limiter |
| `src/test/java/com/campos/webscraper/infrastructure/http/RateLimitedJobFetcherTest.java` | Criado | Testes unitários do rate limiter |
| `src/test/java/com/campos/webscraper/shared/FetchRequestTest.java` | Modificado | Testes do `siteKey` e fallback por host |
| `src/test/java/com/campos/webscraper/infrastructure/http/HttpJobFetcherTest.java` | Modificado | Ajuste de compatibilidade com novo `FetchRequest` |
| `docs/stories/README.md` | Modificado | Atualização do índice de stories |
| `docs/stories/STORY-7.2-fetch-rate-limiting.md` | Criado | Registro final da execução da story |

---

## Problemas encontrados

### Problema 1 — O contrato atual de fetch não tinha uma chave lógica de site

Sem esse identificador, o limiter ficaria acoplado apenas ao host da URL, o que não representa bem
as fontes do domínio.

### Problema 2 — Retry e rate limiting precisavam coexistir no bean real de `JobFetcher`

Aplicar rate limiting só em testes ou fora da composição final não atenderia o objetivo da story.

---

## Causa raiz

### Problema 1

`FetchRequest` foi criado inicialmente só como comando HTTP, sem preocupação com políticas
operacionais por source.

### Problema 2

A Story 7.1 introduziu retry no bean real, então a 7.2 precisava reorganizar a composição sem
quebrar o comportamento já validado.

---

## Solução aplicada

- introduzido `siteKey` opcional em `FetchRequest`
- implementado fallback para host quando `siteKey` não é informado
- criado `RateLimitedJobFetcher` baseado em `RateLimiterRegistry`
- configurado `RateLimiterRegistry` com defaults:
  - `limitForPeriod = 10`
  - `refreshPeriod = 60s`
  - `timeoutDuration = 0`
- composto o bean real na ordem:
  `Http -> RateLimit -> Retry`

Essa ordem faz cada tentativa real de fetch consumir permissão do limiter, o que é coerente com o
objetivo operacional do ADR006.

---

## Lições aprendidas

- policies por site exigem chave lógica explícita no request, não só URL
- retry e rate limiting funcionam melhor como decorators independentes e componíveis
- a próxima story (`7.3`) pode atacar circuit breaker e dead-letter sem reabrir o contrato do
  fetcher

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

- rate limiting por site implementado
- negação ocorre antes do transporte HTTP
- workspace pronto para review antes de commit/push
