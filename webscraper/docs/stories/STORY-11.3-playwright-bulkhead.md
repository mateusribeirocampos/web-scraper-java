# Story 11.3 — Bulkhead para Playwright

**Status:** ✅ Concluída  
**Iteration:** 11 — Fallback dinâmico (Playwright)  
**Data:** 2026-03-17  
**Referência ADR:** ADR009 Story 11.3

---

## Objetivo

- Evitar que vários jobs Playwright disparem browsers simultâneos além de um limite seguro.
- Encapsular essa limitação num serviço reutilizável e aplicar à `PlaywrightDynamicScraperStrategy`.

## Ciclo TDD

### RED

- Escrevemos `PlaywrightConcurrencyServiceTest` primeiro: o teste garante que o segundo job espera pelo primeiro e que interrupções são convertidas em `IllegalStateException`.

### GREEN

- Criamos `PlaywrightConcurrencyService`, um wrapper de `Semaphore` com um `@Value("${playwright.concurrent-jobs:2}")` e API `execute(Supplier<T>)`.
- A strategy agora utiliza o serviço para envolver cada `PlaywrightJobFetcher.fetch` dentro do bloqueio.

### REFACTOR

- Registramos o serviço como Spring bean via `PlaywrightConfiguration`.
- Atualizamos os testes da strategy para usar o novo construtor (já exigido desde a story anterior) e documentamos o fluxo.

## Arquivos criados / modificados

- `src/main/java/com/campos/webscraper/application/strategy/PlaywrightConcurrencyService.java`
- `src/main/java/com/campos/webscraper/config/PlaywrightConfiguration.java`
- `src/main/java/com/campos/webscraper/application/strategy/PlaywrightDynamicScraperStrategy.java`
- `src/test/java/com/campos/webscraper/application/strategy/PlaywrightConcurrencyServiceTest.java`
- `docs/stories/STORY-11.3-playwright-bulkhead.md`
- `docs/stories/README.md`
- `ADRs-web-scraping-vagas/ADR009-XP-Delivery-Plan-and-Detailed-Tasks-for-WebScraper.md`

## Problemas encontrados

- A strategy precisava do serviço logo na sua construção, então os testes também precisavam ser atualizados para passar o bean e manter o contrato.

## Causa raiz

- Sem limites de concorrência, vários Playwrights poderiam ser disparados (gestão de browser). O novo serviço oferece controle fino antes de investir em scaling por worker.

## Solução aplicada

- `PlaywrightConcurrencyService` controla o número de execuções simultâneas e transforma `InterruptedException` em `IllegalStateException`.
- A strategy passa a chamar `concurrencyService.execute(...)` ao redor do fetch, garantindo que nenhum job ultrapasse a cota.
- O serviço é registrado via `PlaywrightConfiguration`, mantendo o wiring Spring.

## Lições aprendidas

- Um bulkhead leve no nível do código é suficiente antes de expandir para uma camada de worker/queue.
- Expor o serviço via `@Configuration` permite reuso em outras stories de fallback de browser.

## Estado final

- Testes: `./mvnw test -DexcludedGroups=integration -Dtest=PlaywrightDynamicScraperStrategyTest,PlaywrightConcurrencyServiceTest`
- Build completo não foi reexecutado inteiramente nesta story (confia-se na execução da story 10.4).

## Próxima fatia planejada

- Iteration 12 — Observabilidade e governança.
