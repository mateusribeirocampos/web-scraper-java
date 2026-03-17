# Story 11.2 — Playwright dynamic strategy

**Status:** ✅ Concluída  
**Iteration:** 11 — Fallback dinâmico (Playwright)  
**Data:** 2026-03-17  
**Referência ADR:** ADR009 Story 11.2

---

## Objetivo

- Implementar a strategy `PlaywrightDynamicScraperStrategy` que consome `PlaywrightJobFetcher` para sites Tipo C.
- Garantir que o fluxo falhe primeiro quando o browser retorna um status diferente de 200 e, depois, valide a extração real usando fixtures JS-heavy.

## Ciclo TDD

### RED

- `PlaywrightDynamicScraperStrategyTest` foi escrito antes do código de produção. O teste valida o corte de erro (`status != 200`) e o parsing do fixture dinâmico.

### GREEN

- Implementamos `DynamicJobListing`, o parser JS (`DynamicJobListingParser`) e o normalizador `DynamicJobNormalizer`, além da strategy propriamente dita.
- O fixture `dynamic-site.html` simula páginas renderizadas por Playwright que contêm dois cartões de vaga.

### REFACTOR

- Encapsulamos o parsing do HTML dinâmico para manter a strategy focada no fluxo `fetch -> parse -> normalize -> result`.
- Documentamos o caminho de teste/falha no story log e atualizamos README/ADR para registrar que a story foi concluída.

## Arquivos criados / modificados

- `src/main/java/com/campos/webscraper/infrastructure/parser/DynamicJobListing.java`
- `src/main/java/com/campos/webscraper/infrastructure/parser/DynamicJobListingParser.java`
- `src/main/java/com/campos/webscraper/application/normalizer/DynamicJobNormalizer.java`
- `src/main/java/com/campos/webscraper/application/strategy/PlaywrightDynamicScraperStrategy.java`
- `src/test/java/com/campos/webscraper/application/strategy/PlaywrightDynamicScraperStrategyTest.java`
- `src/test/resources/fixtures/playwright/dynamic-site.html`
- `docs/stories/STORY-11.2-playwright-dynamic-strategy.md`
- `docs/stories/README.md`
- `ADRs-web-scraping-vagas/ADR009-XP-Delivery-Plan-and-Detailed-Tasks-for-WebScraper.md`

## Problemas encontrados

- O Playwright necessitava um parser específico para os “job cards” porque o HTML renderizado não era estático.
- Precisávamos comprovar que, ao falhar o browser, a strategy devolve uma mensagem de falha antes de tentar parsing.

## Causa raiz

- Sites Tipo C exigem que o browser execute toda a renderização JS, e o código atual só dava suporte a fetchers HTTP/JSON.
 
## Solução aplicada

- Criamos o parser dedicado, o normalizador e a strategy, garantindo que o resultado `ScrapeResult` siga a mesma assinatura usada pelos demais strategies.
- O teste `PlaywrightDynamicScraperStrategyTest` cobre o cenário de falha primeiro e depois o parsing bem-sucedido usando fixture real.

## Lições aprendidas

- Testes de contrato/falha primeiro evitam misturar a complexidade do Playwright com o parser quando a execução falha.
- Centralizar o parsing em `DynamicJobListingParser` facilita criar outras strategies baseadas nesses cartões.

## Estado final

- Testes: `./mvnw test -DexcludedGroups=integration -Dtest=PlaywrightDynamicScraperStrategyTest`
- Build completo não foi disparado nesta story (já temos confiança na suite da story 10.4).

## Próxima fatia planejada

- Story 11.3 — Bulkhead para browser jobs
