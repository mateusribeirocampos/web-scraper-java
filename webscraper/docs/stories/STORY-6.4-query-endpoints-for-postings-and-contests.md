# STORY 6.4 — Endpoints de listagem por data

**Status:** ✅ Concluída
**Iteration:** 6 — Agendamento e execução manual
**Data:** 2026-03-12
**Referência ADR:** ADR009 Story 6.4

---

## Objetivo

Expor os primeiros endpoints de consulta do projeto:

- `GET /api/v1/job-postings?since=2026-03-01&category=PRIVATE_SECTOR&seniority=JUNIOR`
- `GET /api/v1/public-contests?status=OPEN&orderBy=registrationEndDate`

A story fecha a leitura HTTP das duas coleções principais já persistidas: `job_postings` e
`public_contest_postings`.

---

## Ciclo TDD

### RED — testes escritos antes da implementação

Foram criados quatro testes:

- `ListJobPostingsUseCaseTest`
- `ListPublicContestsUseCaseTest`
- `JobPostingQueryControllerTest`
- `PublicContestQueryControllerTest`

Cobertura inicial:

- consulta de vagas privadas por `publishedAt >= since` e `seniority`
- consulta de concursos por `status` ordenados por `registrationEndDate`
- serialização HTTP das respostas resumidas
- validação de `orderBy` suportado na consulta de concursos

As falhas iniciais de compilação foram:

```text
cannot find symbol: class ListJobPostingsUseCase
cannot find symbol: class ListPublicContestsUseCase
cannot find symbol: class JobPostingQueryController
cannot find symbol: class PublicContestQueryController
```

### GREEN — implementação mínima

Foi implementado o mínimo necessário:

- métodos derivados de repositório para consulta ordenada
- `ListJobPostingsUseCase`
- `ListPublicContestsUseCase`
- `JobPostingQueryController`
- `PublicContestQueryController`
- DTOs de resposta resumida para evitar expor entidades JPA completas
- tratamento `400 Bad Request` para argumentos inválidos

### REFACTOR

Sem refatoração estrutural ampla. A leitura HTTP ficou separada da camada JPA por use cases e DTOs
resumidos, o que evita acoplamento indevido com relacionamentos lazy das entidades.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/application/usecase/ListJobPostingsUseCase.java` | Criado | Consulta vagas privadas por data e senioridade |
| `src/main/java/com/campos/webscraper/application/usecase/ListPublicContestsUseCase.java` | Criado | Consulta concursos por status e ordenação suportada |
| `src/main/java/com/campos/webscraper/interfaces/rest/JobPostingQueryController.java` | Criado | Endpoint REST de listagem de vagas privadas |
| `src/main/java/com/campos/webscraper/interfaces/rest/PublicContestQueryController.java` | Criado | Endpoint REST de listagem de concursos |
| `src/main/java/com/campos/webscraper/interfaces/dto/JobPostingSummaryResponse.java` | Criado | Payload resumido de vaga privada |
| `src/main/java/com/campos/webscraper/interfaces/dto/PublicContestSummaryResponse.java` | Criado | Payload resumido de concurso |
| `src/main/java/com/campos/webscraper/domain/repository/JobPostingRepository.java` | Modificado | Query por `since` + `seniority` ordenada por `publishedAt desc` |
| `src/main/java/com/campos/webscraper/domain/repository/PublicContestPostingRepository.java` | Modificado | Query por `status` ordenada por `registrationEndDate asc` |
| `src/main/java/com/campos/webscraper/interfaces/rest/RestExceptionHandler.java` | Modificado | Mapeia `IllegalArgumentException` para `400` |
| `src/test/java/com/campos/webscraper/application/usecase/ListJobPostingsUseCaseTest.java` | Criado | Teste unitário da consulta de vagas |
| `src/test/java/com/campos/webscraper/application/usecase/ListPublicContestsUseCaseTest.java` | Criado | Teste unitário da consulta de concursos |
| `src/test/java/com/campos/webscraper/interfaces/rest/JobPostingQueryControllerTest.java` | Criado | Teste HTTP do endpoint de vagas |
| `src/test/java/com/campos/webscraper/interfaces/rest/PublicContestQueryControllerTest.java` | Criado | Teste HTTP do endpoint de concursos |
| `docs/stories/README.md` | Modificado | Atualização do índice de stories |
| `docs/stories/STORY-6.4-query-endpoints-for-postings-and-contests.md` | Criado | Registro final da execução da story |

---

## Problemas encontrados

### Problema 1 — Os repositórios ainda não tinham consultas prontas para os filtros do endpoint

As queries existentes cobriam somente lookup simples e deduplicação, não o recorte de leitura HTTP
exigido pela story.

### Problema 2 — Expor entidades JPA diretamente no endpoint seria frágil

As entidades possuem relacionamentos lazy e vários campos internos que não fazem parte da resposta
mínima da API.

---

## Causa raiz

### Problema 1

Até aqui o foco do projeto estava em ingestão, persistência e execução operacional, não em leitura.

### Problema 2

O modelo de domínio foi desenhado para persistência e auditoria, não como contrato HTTP público.

---

## Solução aplicada

- adicionadas queries derivadas específicas para leitura
- criados use cases de consulta separados por coleção
- controllers REST retornam DTOs resumidos
- `orderBy` de concursos ficou explicitamente limitado a `registrationEndDate`
- argumentos inválidos agora retornam `400` via `RestExceptionHandler`

---

## Lições aprendidas

- a separação entre endpoint de leitura e entidade persistida evita vazamento acidental de detalhes
  internos
- a API pública começa a se estabilizar sem forçar mudanças na modelagem JPA
- a próxima iteração pode avançar para resiliência sem reabrir o contrato básico de leitura

---

## Estado final

Resultado da suite unitária validada:

```text
Tests run: 167, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Comando executado:

```bash
./mvnw test -DexcludedGroups=integration
```

Conclusão:

- endpoints de consulta para vagas privadas e concursos implementados
- filtros principais do ADR009 expostos via HTTP
- story pronta para a próxima iteração
