# STORY 4.1 — IndeedApiClient

**Status:** ✅ Concluída
**Iteration:** 4 — Primeira integração API-first: Indeed MCP Connector
**Data:** 2026-03-12
**Referência ADR:** ADR009 Story 4.1

---

## Objetivo

Implementar o cliente HTTP para o Indeed MCP connector, incluindo:

- `IndeedApiClient`
- DTO `IndeedApiResponse`
- desserialização JSON da resposta

Essa story estabelece o primeiro adaptador API-first concreto do projeto.

---

## Ciclo TDD

### RED — testes escritos antes da implementação

Foi criado `IndeedApiClientTest` cobrindo:

- desserialização de uma resposta JSON válida do Indeed MCP
- erro descritivo quando a API retorna status não-2xx

O teste usa fixture JSON local e transporte fake com interceptor do OkHttp, evitando dependência
de socket ou servidor externo.

A falha inicial de compilação foi:

```text
package com.campos.webscraper.interfaces.dto does not exist
```

### GREEN — implementação mínima

Foi implementado o mínimo necessário:

1. `IndeedApiResponse` como record DTO
2. `IndeedApiClient` usando OkHttp + Jackson
3. método `fetchJob(String url)` com:
   - `GET`
   - validação de status HTTP
   - desserialização para `IndeedApiResponse`

### REFACTOR

Sem necessidade de refatoração estrutural. O cliente foi mantido simples e focado na story:
um endpoint, um DTO, um fluxo de desserialização.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/infrastructure/http/IndeedApiClient.java` | Criado | Cliente HTTP para o Indeed MCP |
| `src/main/java/com/campos/webscraper/interfaces/dto/IndeedApiResponse.java` | Criado | DTO da resposta JSON do Indeed |
| `src/test/java/com/campos/webscraper/infrastructure/http/IndeedApiClientTest.java` | Criado | Testes unitários do cliente com fixture JSON |
| `src/test/resources/fixtures/indeed/indeed-job-response.json` | Criado | Fixture de resposta da API |
| `docs/stories/README.md` | Modificado | Atualização do índice de stories |
| `docs/stories/STORY-4.1-indeed-api-client.md` | Criado | Registro final da execução da story |

---

## Problemas encontrados

### Problema 1 — WireMock não era necessário para esta fatia

O ADR menciona WireMock para esse tipo de teste, mas para esta story específica um transporte fake
com interceptor do OkHttp foi suficiente e mais estável no ambiente atual.

---

## Causa raiz

O objetivo da story é validar cliente HTTP + desserialização. Isso pode ser feito de maneira mais
determinística com `OkHttpClient` fake, sem subir servidor local nem depender do runtime do WireMock.

---

## Solução aplicada

- criado DTO `IndeedApiResponse` com os campos:
  - `jobId`
  - `title`
  - `company`
  - `location`
  - `postedAt`
  - `applyUrl`
- criado `IndeedApiClient` com:
  - construtor default
  - construtor injetável para testes com `OkHttpClient`
  - desserialização via `ObjectMapper`
  - `IllegalStateException` para status HTTP inválido ou falha de IO

---

## Lições aprendidas

- para clients HTTP simples, um interceptor fake do OkHttp é suficiente para o ciclo TDD
- a primeira integração API-first pode avançar sem depender ainda de Spring beans ou configuração
- a fixture JSON local deixa a 4.2 (`IndeedJobNormalizer`) imediata de implementar

---

## Estado final

Resultado da suite unitária validada:

```text
Tests run: 141, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Comando executado:

```bash
./mvnw test -DexcludedGroups=integration
```

Conclusão:

- `IndeedApiClient` implementado
- desserialização JSON implementada
- testes unitários verdes
- story pronta para a 4.2 (`IndeedJobNormalizer`)
