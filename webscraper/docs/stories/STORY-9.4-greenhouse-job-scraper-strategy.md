# STORY 9.4 — Implementar GreenhouseJobScraperStrategy

**Status:** ✅ Concluída
**Iteration:** 9 — Expansão do setor privado para PMEs via ATS público
**Data:** 2026-03-13
**Referência ADR:** ADR009 Story 9.4

---

## Objetivo

Integrar a borda HTTP da 9.2 com a normalização da 9.3 e fechar a primeira strategy Greenhouse
API-first do projeto:

- suportar o `siteCode = greenhouse_bitso`
- consumir o endpoint público curado do board
- normalizar a lista de jobs em `JobPostingEntity`
- devolver `ScrapeResult.success(...)` para a factory/use cases seguintes

---

## Ciclo TDD

### RED — support contract e integração client + normalizer primeiro

Foi criado `GreenhouseJobScraperStrategyTest` cobrindo:

- resolução de suporte para um `TargetSiteEntity` Greenhouse aprovado
- integração ponta a ponta `client -> normalizer -> ScrapeResult`

O RED inicial falhou por compilação, porque ainda não existia `GreenhouseJobScraperStrategy`.

### GREEN — implementação mínima

Foi implementado:

1. `GreenhouseJobScraperStrategy`

Contrato entregue:

- `supports(...)` reconhece `greenhouse_bitso` como `TYPE_E + API + PRIVATE_SECTOR + APPROVED`
- `scrape(...)` chama `GreenhouseJobBoardClient.fetchPublishedJobs(...)`
- cada item da Greenhouse é normalizado por `GreenhouseJobNormalizer`
- o resultado sai como `ScrapeResult.success(items, siteCode)`

### REFACTOR

Também foi alinhado o profile da 9.1:

- `strategySupportVerified` passou a `true` para o board `bitso`
- o onboarding curado agora fecha como `APPROVED` e `enabled = true`

Isso remove a pendência operacional aberta no final da 9.1.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/application/strategy/GreenhouseJobScraperStrategy.java` | Criado | Strategy API-first da Greenhouse |
| `src/test/java/com/campos/webscraper/application/strategy/GreenhouseJobScraperStrategyTest.java` | Criado | RED/GREEN da strategy |
| `src/main/java/com/campos/webscraper/application/onboarding/GreenhouseBoardOnboardingProfiles.java` | Modificado | Marca `bitso` com strategy disponível |
| `src/test/java/com/campos/webscraper/application/onboarding/GreenhouseBoardOnboardingProfileTest.java` | Modificado | Valida aprovação do onboarding após a strategy existir |
| `docs/stories/STORY-9.4-greenhouse-job-scraper-strategy.md` | Criado | Registro final da story |
| `docs/stories/README.md` | Modificado | Atualização do índice |

---

## Problemas encontrados

### Problema 1 — o board Greenhouse já estava escolhido, mas ainda não havia strategy conectando as camadas

A 9.1 fechou o onboarding e a 9.2/9.3 fecharam client e normalizer, mas ainda faltava a costura
explícita em uma `JobScraperStrategy`.

### Problema 2 — o onboarding do board Bitso ainda carregava a pendência operacional da 9.1

Mesmo com o board escolhido e o client existente, o checklist curado continuava marcando
`strategySupportVerified = false`, então o validator mantinha o site fora de produção.

---

## Causa raiz

- as stories 9.2 e 9.3 fecharam as camadas de transporte e mapeamento isoladamente
- ainda faltava a adaptação para o contrato comum de `JobScraperStrategy`
- a evidência de readiness operacional da 9.1 dependia exatamente desta story

---

## Solução aplicada

- criada `GreenhouseJobScraperStrategy`
- conectados `GreenhouseJobBoardClient` e `GreenhouseJobNormalizer`
- garantido `ScrapeResult` bem-sucedido com múltiplos jobs publicados
- atualizado o profile `bitso` para refletir que a strategy agora existe
- ajustado o teste de onboarding para validar o estado aprovado/ativável

---

## Lições aprendidas

- a sequência 9.1 -> 9.2 -> 9.3 -> 9.4 funcionou bem porque cada camada chegou na strategy já
  com contrato claro
- checklist curado de onboarding precisa acompanhar o estado real do runtime, não só a intenção do
  roadmap
- a strategy Greenhouse encaixa naturalmente no mesmo padrão API-first já usado em Indeed e DOU

---

## Estado final

- `GreenhouseJobScraperStrategy` implementada
- board `greenhouse_bitso` já suportado no contrato de strategy
- onboarding curado do board agora pode fechar como `APPROVED`
- testes unitários da fatia verdes

Validação executada:

- `./mvnw test -DexcludedGroups=integration -Dtest=GreenhouseJobScraperStrategyTest,GreenhouseBoardOnboardingProfileTest`

Próximo passo natural:

- Story 9.5 — persistência ponta a ponta das vagas Greenhouse
