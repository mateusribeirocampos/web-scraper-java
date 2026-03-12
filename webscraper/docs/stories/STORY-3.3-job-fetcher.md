# STORY 3.3 — JobFetcher

**Status:** ✅ Concluída
**Iteration:** 3 — Contratos de Strategy e Factory
**Data:** 2026-03-12
**Referência ADR:** ADR009 Story 3.3 / ADR004 Seção 2

---

## Objetivo

Implementar a abstração `JobFetcher` e a primeira implementação concreta `HttpJobFetcher`,
usando OkHttp para recuperar páginas HTTP e retornar `FetchedPage`.

Essa story fecha o contrato de transporte da camada de extração antes das strategies concretas.

---

## Ciclo TDD

### RED — testes escritos antes da implementação

Foi criado `HttpJobFetcherTest` cobrindo:

- fetch bem-sucedido de uma página HTML
- respeito a `followRedirects = false`
- follow de redirect quando permitido

As primeiras falhas reais vieram pela ausência de:

```text
cannot find symbol: class JobFetcher
cannot find symbol: class HttpJobFetcher
```

Durante a escrita dos testes, surgiu também um detalhe de compatibilidade da versão atual de
WireMock no projeto: o helper `redirectTo(...)` não estava disponível no runtime usado aqui.

### GREEN — implementação mínima

Foi implementado o mínimo necessário:

1. `JobFetcher` como contrato
2. `HttpJobFetcher` com OkHttp
3. mapeamento da resposta HTTP para `FetchedPage`
4. suporte a headers, timeout e política de redirect vindos de `FetchRequest`

### REFACTOR

Dois ajustes de refactor/testabilidade foram necessários:

- o stub de redirect foi reescrito para usar `Location` explícito
- os testes migraram de WireMock/Jetty para `HttpServer` do JDK por conflito de dependências
  e, depois, precisaram ser executados fora do sandbox por bloqueio de socket local

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/infrastructure/http/JobFetcher.java` | Criado | Contrato de transporte HTTP/browser |
| `src/main/java/com/campos/webscraper/infrastructure/http/HttpJobFetcher.java` | Criado | Implementação OkHttp do fetcher |
| `src/test/java/com/campos/webscraper/infrastructure/http/HttpJobFetcherTest.java` | Criado | Testes unitários com transporte HTTP mockado local |
| `docs/stories/README.md` | Modificado | Atualização do índice de stories |
| `docs/stories/STORY-3.3-job-fetcher.md` | Criado | Registro final da execução da story |

---

## Problemas encontrados

### Problema 1 — Incompatibilidade de WireMock/Jetty no runtime de testes

**Sintoma:**

```text
NoSuchMethodError: org.eclipse.jetty.util.component.Environment.ensure(...)
```

### Problema 2 — Sandbox bloqueia abertura de socket local

**Sintoma:**

```text
java.net.SocketException: Operation not permitted
```

Isso aconteceu quando o teste tentou subir um servidor HTTP local para mockar o transporte.

---

## Causa raiz

### Problema 1

O conjunto atual de dependências de teste do projeto introduz conflito entre o WireMock presente
e as bibliotecas Jetty disponíveis no classpath do runtime.

### Problema 2

O sandbox não permite bind de socket local para esse tipo de teste de transporte HTTP.

---

## Solução aplicada

### Para a implementação da story

- criado `JobFetcher` com método:

```java
FetchedPage fetch(FetchRequest request);
```

- criado `HttpJobFetcher` com:
  - `OkHttpClient` configurado por request
  - timeout derivado de `FetchRequest.timeoutMs()`
  - `followRedirects` e `followSslRedirects` derivados de `FetchRequest.followRedirects()`
  - mapeamento para `FetchedPage`
  - retorno de `statusCode = 599` em caso de `IOException`

### Para a validação

- os testes foram adaptados para usar `HttpServer` do JDK como transporte mockado local
- o teste específico foi executado fora do sandbox:

```bash
./mvnw test -DexcludedGroups="integration" -Dtest=HttpJobFetcherTest
```

- a suite unitária completa também foi executada fora do sandbox:

```bash
./mvnw test -DexcludedGroups=integration
```

---

## Lições aprendidas

- a ideia central da story é proteger o contrato de transporte; o servidor mock pode variar
  quando a infraestrutura de teste do projeto estiver conflitando
- o sandbox atual não é suficiente para testes unitários que precisem abrir socket local
- `FetchRequest` e `FetchedPage` já estavam bem preparados para receber um fetcher concreto

---

## Estado final

Resultado da suite unitária validada fora do sandbox:

```text
Tests run: 139, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Comando executado:

```bash
./mvnw test -DexcludedGroups=integration
```

Conclusão:

- `JobFetcher` implementado
- `HttpJobFetcher` implementado
- testes unitários verdes
- story pronta para a próxima camada de strategies concretas
