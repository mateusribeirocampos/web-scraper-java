# Story 13.3.6 — Santa Rita do Sapucaí Private Track via WatchGuard Lever

## Status

Implemented and ready for review

## Objective

Abrir a primeira implementação de `Santa Rita do Sapucaí` pela trilha privada, reaproveitando o
pipeline `Lever` já estabilizado em `Campinas` para onboardar `WatchGuard Technologies`.

## TDD Cycle

1. Red:
   - adicionar regressões para o catálogo curado expor `lever_watchguard`;
   - provar no runner que um novo `lever_*` continua sendo roteado pelo caminho genérico de `Lever`.
2. Green:
   - materializar o novo perfil curado em `LeverBoardOnboardingProfiles`;
   - registrar o board no catálogo.
3. Refactor:
   - manter o pipeline `Lever` genérico, sem duplicar strategy/import/normalizer;
   - sincronizar documentação da nova frente híbrida.

## Files Created / Modified

- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/LeverBoardOnboardingProfiles.java`
- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalog.java`
- `webscraper/src/test/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalogTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/orchestrator/ImportingCrawlJobExecutionRunnerTest.java`
- `ADRs-web-scraping-vagas/ADR009-XP-Delivery-Plan-and-Detailed-Tasks-for-WebScraper.md`
- `README.md`
- `webscraper/README.md`
- `webscraper/docs/stories/README.md`
- `webscraper/docs/stories/STORY-13.3.6-santa-rita-watchguard-lever.md`

## Applied Solution

- novo perfil curado `lever_watchguard`
- board token: `watchguard`
- jobs API: `https://api.lever.co/v0/postings/watchguard?mode=json`
- site code: `lever_watchguard`
- família: `LEVER`
- status inicial:
  - `PENDING_REVIEW`
  - `enabled=false`

## Why This Is Enough

- `LeverPostingsClient`, `LeverJobNormalizer`, `LeverJobScraperStrategy` e `LeverJobImportUseCase`
  já são genéricos para qualquer `lever_*`.
- portanto, a entrada correta desta fatia é onboardar o novo board com cobertura de catálogo e
  roteamento, não duplicar a integração existente.

## Validation

Validação automatizada esperada desta fatia:

- catálogo expõe `lever_watchguard`
- `siteCode`, `boardToken` e `jobsApiUrl` ficam corretos
- `ImportingCrawlJobExecutionRunner` continua roteando `lever_watchguard` para
  `LeverJobImportUseCase`

Validação real da aplicação deve acontecer na etapa seguinte da story:

- bootstrap do perfil `lever_watchguard`
- `operational-check` real com a aplicação rodando

## Final State

- `Santa Rita do Sapucaí` passa a ter a trilha privada `WatchGuard` formalmente implementada no
  catálogo curado
- a validação operacional real fica como próximo passo antes de qualquer ativação
