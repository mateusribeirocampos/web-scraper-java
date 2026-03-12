# STORY 4.3 — IndeedApiJobScraperStrategy

**Status:** ✅ Concluída
**Iteration:** 4 — Primeira integração API-first: Indeed MCP Connector
**Data:** 2026-03-12
**Referência ADR:** ADR009 Story 4.3 / ADR004 Seção 6

---

## Objetivo

Implementar a `IndeedApiJobScraperStrategy`, integrando:

- `IndeedApiClient`
- `IndeedJobNormalizer`
- contrato `JobScraperStrategy<JobPostingEntity>`

Essa story fecha a primeira strategy concreta API-first do projeto.

---

## Ciclo TDD

### RED — testes escritos antes da implementação

Foi criado `IndeedApiJobScraperStrategyTest` cobrindo:

- `supports(TargetSiteEntity)` usando metadados explícitos do site
- fluxo de `scrape(ScrapeCommand)` integrando client + normalizer e retornando `ScrapeResult`

As falhas iniciais foram de compilação:

```text
cannot find symbol: class IndeedApiJobScraperStrategy
```

### GREEN — implementação mínima

Foi implementado o mínimo necessário:

1. `IndeedApiJobScraperStrategy implements JobScraperStrategy<JobPostingEntity>`
2. `supports(...)` validando:
   - `siteCode = indeed-br`
   - `siteType = TYPE_E`
   - `extractionMode = API`
   - `jobCategory = PRIVATE_SECTOR`
   - `legalStatus = APPROVED`
3. `scrape(...)` executando:
   - `indeedApiClient.fetchJob(command.targetUrl())`
   - `normalizer.normalize(response)`
   - `ScrapeResult.success(List.of(posting), command.siteCode())`

### REFACTOR

Sem necessidade de refatoração estrutural. A strategy foi mantida intencionalmente enxuta para
encapsular apenas orquestração de client + normalizer.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/application/strategy/IndeedApiJobScraperStrategy.java` | Criado | Strategy concreta API-first do Indeed |
| `src/test/java/com/campos/webscraper/application/strategy/IndeedApiJobScraperStrategyTest.java` | Criado | Testes unitários de suporte e extração |
| `docs/stories/README.md` | Modificado | Atualização do índice de stories |
| `docs/stories/STORY-4.3-indeed-api-job-scraper-strategy.md` | Criado | Registro final da execução da story |

---

## Problemas encontrados

### Problema 1 — A strategy precisava permanecer no papel de orquestradora

Havia risco de empurrar regra de normalização ou regra de cliente HTTP para dentro da strategy,
o que misturaria responsabilidades cedo demais.

---

## Causa raiz

Nesta etapa, a arquitetura já separou contrato, client e normalizer. A strategy precisava apenas
ligar essas peças, não reimplementar partes delas.

---

## Solução aplicada

- a strategy só coordena dependências
- `supports(...)` ficou explícito e coerente com a resolução da factory
- `scrape(...)` retorna `ScrapeResult<JobPostingEntity>` com o item normalizado já pronto

---

## Lições aprendidas

- a separação entre client, normalizer e strategy está funcionando como previsto no ADR004
- a próxima story de persistência (4.4) já pode focar no fluxo completo sem revisitar parsing
  ou normalização
- os testes da 4.3 mostraram que a primeira strategy concreta pode ser desenvolvida sem
  acoplamento a Spring beans ou infraestrutura externa

---

## Estado final

Resultado da suite unitária validada:

```text
Tests run: 147, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Comando executado:

```bash
./mvnw test -DexcludedGroups=integration
```

Conclusão:

- `IndeedApiJobScraperStrategy` implementada
- testes unitários verdes
- story pronta para a 4.4, que fecha a persistência do fluxo Indeed
