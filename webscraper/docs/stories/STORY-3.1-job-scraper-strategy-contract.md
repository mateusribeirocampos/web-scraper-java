# STORY 3.1 — Contrato JobScraperStrategy

**Status:** ✅ Concluída
**Iteration:** 3 — Contratos de Strategy e Factory
**Data:** 2026-03-12
**Referência ADR:** ADR009 Story 3.1 / ADR004 Seção 2

---

## Objetivo

Definir o contrato base `JobScraperStrategy` para a arquitetura de extração do projeto,
estabelecendo:

- resolução de suporte por metadados explícitos do site
- execução de scraping via `ScrapeCommand`
- retorno padronizado via `ScrapeResult<T>`

Essa story cria a interface que será usada pelas strategies concretas e pela futura factory.

---

## Ciclo TDD

### RED — testes escritos antes da implementação

Foi criado `JobScraperStrategyContractTest` com implementações fake para fixar o comportamento
esperado do contrato:

- `supports(TargetSiteEntity)` deve usar metadados explícitos
- `supports(TargetSiteEntity)` não deve inferir suporte apenas pela URL
- `scrape(ScrapeCommand)` deve retornar `ScrapeResult` de sucesso quando a extração funciona
- `scrape(ScrapeCommand)` deve encapsular falhas esperadas em `ScrapeResult.failure(...)`

As falhas iniciais foram de compilação:

```text
cannot find symbol: class JobScraperStrategy
```

### GREEN — implementação mínima

Foi adicionada a interface `JobScraperStrategy<T>` em `application/strategy` com os dois métodos:

```java
boolean supports(TargetSiteEntity targetSite);
ScrapeResult<T> scrape(ScrapeCommand command);
```

### REFACTOR

Sem necessidade de refatoração estrutural. A story ficou intencionalmente mínima para consolidar
o contrato antes de introduzir factory, fetcher e strategies concretas.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/application/strategy/JobScraperStrategy.java` | Criado | Contrato base das strategies de extração |
| `src/test/java/com/campos/webscraper/application/strategy/JobScraperStrategyContractTest.java` | Criado | Contract tests do comportamento esperado da interface |
| `docs/stories/README.md` | Modificado | Atualização do índice de stories |
| `docs/stories/STORY-3.1-job-scraper-strategy-contract.md` | Criado | Registro final da execução da story |

---

## Problemas encontrados

### Problema 1 — Necessidade de definir o contrato sem antecipar implementação concreta

O ADR descreve várias strategies concretas futuras, mas a Story 3.1 pedia apenas o contrato.
Havia risco de introduzir abstrações extras desnecessárias cedo demais.

---

## Causa raiz

A arquitetura ainda está no momento de consolidar as interfaces centrais. Qualquer tipo adicional
não exigido pelos testes abriria escopo indevido para a story.

---

## Solução aplicada

- definido `JobScraperStrategy<T>` como interface genérica e mínima
- mantido o contrato alinhado ao que o projeto já possui:
  - `ScrapeCommand`
  - `ScrapeResult<T>`
  - `TargetSiteEntity`
- usados contract tests com fakes locais para validar sem depender de strategies reais

---

## Lições aprendidas

- contract tests com implementações fake são suficientes para estabilizar a interface antes de
  existir a primeira strategy concreta
- a regra "suporte por metadados explícitos" já ficou protegida por teste antes da factory existir
- a interface genérica permite reaproveitar o contrato tanto para vagas privadas quanto para concursos

---

## Estado final

Resultado da suite unitária:

```text
Tests run: 133, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Comando executado:

```bash
./mvnw test -DexcludedGroups="integration"
```

Conclusão:

- contrato `JobScraperStrategy` implementado
- contract tests verdes
- story pronta para a 3.2 (`JobScraperFactory`)
