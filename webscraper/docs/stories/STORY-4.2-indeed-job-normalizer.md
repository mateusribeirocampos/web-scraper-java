# STORY 4.2 — IndeedJobNormalizer

**Status:** ✅ Concluída
**Iteration:** 4 — Primeira integração API-first: Indeed MCP Connector
**Data:** 2026-03-12
**Referência ADR:** ADR009 Story 4.2

---

## Objetivo

Implementar o `IndeedJobNormalizer` para mapear `IndeedApiResponse` no formato canônico de
`JobPostingEntity`, com foco em:

- `publishedAt`
- `seniority = JUNIOR`
- `techStackTags = "Java,Spring Boot"`
- sinalização de remoto
- preservação do payload bruto para auditoria

---

## Ciclo TDD

### RED — testes escritos antes da implementação

Foi criado `IndeedJobNormalizerTest` cobrindo:

- mapeamento dos campos principais do DTO para `JobPostingEntity`
- aplicação forçada de `seniority = JUNIOR`
- aplicação de `techStackTags = "Java,Spring Boot"`
- detecção de remoto a partir de título/localização
- preservação do payload original em `payloadJson`

A falha inicial de compilação foi:

```text
cannot find symbol: class IndeedJobNormalizer
```

### GREEN — implementação mínima

Foi implementado `IndeedJobNormalizer` com:

- parse de `postedAt` para `LocalDate`
- mapeamento de `jobId` para `externalId`
- mapeamento de `applyUrl` para `canonicalUrl`
- `seniority` fixado em `JUNIOR`
- `techStackTags` fixado em `Java,Spring Boot`
- serialização do DTO original para `payloadJson`

### REFACTOR

Sem refatoração estrutural relevante. A lógica foi mantida direta para não antecipar
comportamentos da strategy ou da persistência completa da 4.4.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/application/normalizer/IndeedJobNormalizer.java` | Criado | Normalização de `IndeedApiResponse` para `JobPostingEntity` |
| `src/test/java/com/campos/webscraper/application/normalizer/IndeedJobNormalizerTest.java` | Criado | Testes unitários de mapeamento da normalização |
| `docs/stories/README.md` | Modificado | Atualização do índice de stories |
| `docs/stories/STORY-4.2-indeed-job-normalizer.md` | Criado | Registro final da execução da story |

---

## Problemas encontrados

### Problema 1 — O ADR não define explicitamente `contractType`

O `JobPostingEntity` já exige vários campos de domínio, mas a Story 4.2 e o ADR desta iteração
especificam explicitamente apenas `publishedAt`, `seniority` e `techStackTags`.

---

## Causa raiz

O objetivo desta story é normalização inicial do payload Indeed, não fechamento completo do fluxo
de persistência. Forçar inferência artificial de campos não descritos abriria escopo indevido.

---

## Solução aplicada

- o normalizer mapeia somente os campos definidos e claramente inferíveis do payload Indeed
- `remote` é inferido por presença de `remote` no título ou `remoto/remote` na localização
- `payloadJson` é preenchido com serialização do DTO original usando `ObjectMapper`
- `createdAt` é preenchido com `Instant.now()` para manter o objeto pronto para etapas seguintes

---

## Lições aprendidas

- a fixture da 4.1 tornou a 4.2 praticamente linear de implementar
- manter o normalizer restrito ao que o ADR pede evita inventar regra de negócio cedo demais
- a próxima story (4.3) já pode integrar client + normalizer sem novo trabalho de modelagem

---

## Estado final

Resultado da suite unitária validada:

```text
Tests run: 145, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Comando executado:

```bash
./mvnw test -DexcludedGroups=integration
```

Conclusão:

- `IndeedJobNormalizer` implementado
- testes unitários verdes
- story pronta para a 4.3 (`IndeedApiJobScraperStrategy`)
