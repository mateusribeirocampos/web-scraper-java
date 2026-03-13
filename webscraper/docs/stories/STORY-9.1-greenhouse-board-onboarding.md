# STORY 9.1 — Onboarding legal e seleção do primeiro board PME

**Status:** ✅ Concluída
**Iteration:** 9 — Expansão do setor privado para PMEs via ATS público
**Data:** 2026-03-13
**Referência ADR:** ADR009 Story 9.1

---

## Objetivo

Fechar a seleção do primeiro board PME para a trilha Greenhouse e transformar essa escolha em um
artefato reutilizável para as próximas stories:

- escolher um board real, atual e aderente ao foco Java / backend / TI
- registrar a evidência mínima de onboarding legal/técnico
- reutilizar o `TargetSiteOnboardingValidator` da 8.4, sem criar lógica paralela
- deixar `TargetSiteEntity + checklist + endpoint` prontos para a 9.2

---

## Ciclo TDD

### RED — onboarding API-first com endpoint obrigatório

O RED começou ampliando `TargetSiteOnboardingValidatorTest` com um caso novo:

- `API_OFICIAL` deve permanecer `PENDING_REVIEW` quando o endpoint oficial ainda não foi
  documentado

Também foi criado `GreenhouseBoardOnboardingProfileTest` para forçar a existência de um perfil
concreto do primeiro board Greenhouse selecionado.

O RED inicial falhou por compilação, porque o checklist ainda não carregava o endpoint oficial da
API e o projeto ainda não tinha um perfil curado para o board escolhido.

### GREEN — implementação mínima

Foi implementado:

1. ampliação de `SiteOnboardingChecklist` com `officialApiEndpointUrl`
2. extensão de `TargetSiteOnboardingValidator` para exigir endpoint documentado em `API_OFICIAL`
3. ampliação do checklist com `strategySupportVerified`
4. `GreenhouseBoardOnboardingProfile`
5. `GreenhouseBoardOnboardingProfiles.bitso()`

Board selecionado:

- `board_token = bitso`
- `jobsApiUrl = https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true`
- `siteCode = greenhouse_bitso`
- `siteType = TYPE_E`
- `extractionMode = API`
- `jobCategory = PRIVATE_SECTOR`

Evidência usada em 2026-03-13:

- `https://boards.greenhouse.io/robots.txt` retornando `Disallow: /embed/` para `User-agent: *`
- `https://boards-api.greenhouse.io/v1/boards/bitso/jobs` retornando vagas públicas
- o endpoint público atual inclui títulos aderentes ao escopo do projeto, como `Senior Java Engineer`
  e `Senior Security Operations (SecOps) Engineer`

Resultado entregue:

- o board `bitso` ficou materializado como perfil reutilizável para a 9.2
- o validator mantém esse perfil em `PENDING_REVIEW` enquanto a strategy ainda não existir na
  factory
- o endpoint curado do board já sai com `content=true`, preservando a descrição HTML para a 9.2/9.3
- a seleção não depende de pesquisa solta em ADR/doc; agora existe uma representação executável em
  código

### REFACTOR

O perfil do board foi mantido isolado no pacote de onboarding para não antecipar o client HTTP da
9.2. A ideia aqui foi fechar primeiro a decisão de fonte e o gate legal/técnico, deixando a
integração HTTP para a próxima story.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/application/onboarding/SiteOnboardingChecklist.java` | Modificado | Checklist agora inclui endpoint oficial da API e readiness de strategy |
| `src/main/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingValidator.java` | Modificado | Exige endpoint documentado em `API_OFICIAL` e bloqueia ativação sem strategy |
| `src/main/java/com/campos/webscraper/application/onboarding/GreenhouseBoardOnboardingProfile.java` | Criado | Perfil reutilizável de board Greenhouse selecionado |
| `src/main/java/com/campos/webscraper/application/onboarding/GreenhouseBoardOnboardingProfiles.java` | Criado | Catálogo inicial com o board `bitso` |
| `src/test/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingValidatorTest.java` | Modificado | RED/GREEN do endpoint oficial obrigatório |
| `src/test/java/com/campos/webscraper/application/onboarding/GreenhouseBoardOnboardingProfileTest.java` | Criado | Valida board escolhido e aprovação do onboarding |
| `docs/stories/STORY-9.1-greenhouse-board-onboarding.md` | Criado | Registro final da story |
| `docs/stories/README.md` | Modificado | Atualização do índice |
| `ADRs-web-scraping-vagas/ADR002-Target-Site-Taxonomy-and-Requirements-for-WebScraper.md` | Modificado | Checklist ampliado e Bitso registrado como primeiro board |

---

## Problemas encontrados

### Problema 1 — o checklist de onboarding ainda não carregava o endpoint oficial usado pela integração

Na 8.4 o validator já fechava `robots`, ToS, categoria legal e metadados técnicos do site, mas
ainda faltava um campo explícito para registrar qual endpoint oficial seria usado no caminho
`API_OFICIAL`.

### Problema 2 — a seleção do primeiro board PME ainda estava só no plano, não no código

O ADR009 já apontava Greenhouse como próximo passo seguro, mas o projeto ainda não tinha um
`TargetSiteEntity` concreto e reutilizável para o primeiro board escolhido.

### Problema 3 — a 9.2 correria risco de começar sem um token/endpoint validado

Sem fechar a decisão na 9.1, a implementação do client Greenhouse poderia começar em cima de um
board inexistente, irrelevante para Java/TI ou juridicamente mal documentado.

### Problema 4 — o onboarding ainda podia habilitar um site sem strategy registrada

No review apareceu a lacuna operacional:

- o board `greenhouse_bitso` podia sair `APPROVED` e `enabled = true`
- a `DefaultJobScraperFactory` ainda não suporta esse `siteCode`
- isso quebraria em runtime assim que algum job tentasse resolver a strategy

---

## Causa raiz

- a 8.4 resolveu o gate genérico de compliance, mas ainda não precisava registrar um endpoint API
  específico
- o plano de iteração já dizia “Greenhouse first”, porém ainda faltava transformar isso em seleção
  executável e testável
- o projeto ainda não tinha um artefato simples que ligasse board token, endpoint público,
  `TargetSiteEntity` e checklist de onboarding
- o gate de onboarding ainda não separava “fonte escolhida e revisada” de “fonte já suportada pela
  factory em runtime”

---

## Solução aplicada

- o checklist foi ampliado com `officialApiEndpointUrl`
- o checklist foi ampliado com `strategySupportVerified`
- `API_OFICIAL` agora exige endpoint documentado para aprovação
- ativação em produção agora também exige strategy/factory registrada para o `siteCode`
- foi criado um perfil selecionado para `bitso`, com:
  - board token
  - endpoint público
  - `TargetSiteEntity`
  - `SiteOnboardingChecklist`
- o validator da 8.4 foi reutilizado diretamente para validar esse board
- após o review, o perfil `bitso` passou a permanecer `PENDING_REVIEW` até a story 9.4 registrar
  a strategy concreta
- o ADR002 passou a registrar `bitso` como primeiro board Greenhouse selecionado

---

## Lições aprendidas

- onboarding de fonte API também precisa carregar o endpoint oficial exato, não só uma categoria
  legal genérica
- registrar o primeiro board PME em código reduz risco para as stories 9.2, 9.3 e 9.4
- a 8.4 realmente virou fundação da 9.1: o caminho correto foi estender o validator existente,
  não criar outro fluxo de compliance
- escolher a fonte antes do client HTTP melhora os testes e evita retrabalho de fixtures
- ativação em produção depende de compliance e de suporte operacional real na factory

---

## Estado final

- `bitso` definido como primeiro board Greenhouse para a trilha PME
- checklist API-first ampliado com endpoint oficial
- checklist ampliado também com readiness de strategy
- validator reutilizado e cobrindo endpoint oficial + suporte operacional
- testes da fatia de onboarding Greenhouse verdes

Validação executada:

- `./mvnw test -DexcludedGroups=integration -Dtest=TargetSiteOnboardingValidatorTest,GreenhouseBoardOnboardingProfileTest`

Próximo passo natural:

- Story 9.2 — implementar `GreenhouseJobBoardClient` consumindo
  `https://boards-api.greenhouse.io/v1/boards/bitso/jobs`
