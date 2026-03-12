# STORY 6.2 — Endpoint de execução manual

**Status:** ✅ Concluída
**Iteration:** 6 — Agendamento e execução manual
**Data:** 2026-03-12
**Referência ADR:** ADR009 Story 6.2

---

## Objetivo

Expor o endpoint manual:

`POST /api/v1/crawl-jobs/{jobId}/execute`

reutilizando o mesmo `CrawlJobDispatcher` introduzido na Story 6.1, para que scheduler e trigger
manual compartilhem o mesmo ponto de entrada de dispatch.

---

## Ciclo TDD

### RED — testes escritos antes da implementação

Foram criados dois testes:

- `ExecuteCrawlJobManuallyUseCaseTest`
- `CrawlJobControllerTest`

Cobertura inicial:

- dispatch manual de job existente por id
- erro de negócio quando o job não existe
- retorno HTTP `202 Accepted` para dispatch aceito
- retorno HTTP `404 Not Found` para job inexistente

As falhas iniciais de compilação foram:

```text
cannot find symbol: class ExecuteCrawlJobManuallyUseCase
cannot find symbol: class CrawlJobNotFoundException
cannot find symbol: class CrawlJobController
```

### GREEN — implementação mínima

Foi implementado o mínimo necessário:

- `ExecuteCrawlJobManuallyUseCase`
- `CrawlJobNotFoundException`
- `CrawlJobController`
- `RestExceptionHandler`
- DTOs REST mínimos para sucesso e erro

O fluxo final ficou:

`controller -> use case -> crawlJobRepository.findById -> crawlJobDispatcher.dispatch`

### REFACTOR

Sem refatoração estrutural ampla. O endpoint foi mantido enxuto e toda a regra de lookup/dispatch
ficou concentrada no use case.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/application/usecase/ExecuteCrawlJobManuallyUseCase.java` | Criado | Use case de execução manual por id |
| `src/main/java/com/campos/webscraper/shared/CrawlJobNotFoundException.java` | Criado | Exceção de negócio para job inexistente |
| `src/main/java/com/campos/webscraper/interfaces/rest/CrawlJobController.java` | Criado | Endpoint REST de execução manual |
| `src/main/java/com/campos/webscraper/interfaces/rest/RestExceptionHandler.java` | Criado | Mapeamento REST para exceções da aplicação |
| `src/main/java/com/campos/webscraper/interfaces/dto/CrawlJobExecutionResponse.java` | Criado | Payload de sucesso do endpoint |
| `src/main/java/com/campos/webscraper/interfaces/dto/ErrorResponse.java` | Criado | Payload mínimo de erro HTTP |
| `src/test/java/com/campos/webscraper/application/usecase/ExecuteCrawlJobManuallyUseCaseTest.java` | Criado | Testes unitários do use case manual |
| `src/test/java/com/campos/webscraper/interfaces/rest/CrawlJobControllerTest.java` | Criado | Testes HTTP do endpoint manual |
| `docs/stories/README.md` | Modificado | Atualização do índice de stories |
| `docs/stories/STORY-6.2-manual-crawl-job-execution-endpoint.md` | Criado | Registro final da execução da story |

---

## Problemas encontrados

### Problema 1 — O projeto ainda não tinha camada REST implementada

A story precisou introduzir ao mesmo tempo o primeiro controller e o tratamento mínimo de erro
HTTP, sem inflar o escopo para uma API mais ampla.

### Problema 2 — O trigger manual precisava reutilizar exatamente o mesmo contrato do scheduler

Criar um fluxo paralelo para execução manual quebraria a direção estabelecida na Story 6.1.

---

## Causa raiz

### Problema 1

Até aqui o projeto estava concentrado em domínio, persistência, strategies e use cases internos.

### Problema 2

Sem um use case dedicado, o controller poderia facilmente chamar repositório e dispatcher direto,
duplicando a responsabilidade de lookup/dispatch.

---

## Solução aplicada

- introduzido `ExecuteCrawlJobManuallyUseCase` como camada de aplicação
- controller REST delega integralmente ao use case
- `CrawlJobDispatcher` foi reaproveitado sem alterações de contrato
- `CrawlJobNotFoundException` é convertida em `404` por `RestExceptionHandler`

---

## Lições aprendidas

- o contrato de dispatch definido na 6.1 ficou validado por dois pontos de entrada distintos
- a separação `controller -> use case -> dispatcher` evita acoplamento prematuro da camada HTTP
- a próxima story (`6.3`) já pode registrar ciclo de vida de execução sem reabrir a interface REST

---

## Estado final

Resultado da suite unitária validada:

```text
Tests run: 160, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Comando executado:

```bash
./mvnw test -DexcludedGroups=integration
```

Conclusão:

- endpoint manual implementado
- scheduler e endpoint manual compartilham o mesmo `CrawlJobDispatcher`
- story pronta para a 6.3
