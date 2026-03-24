# STORY-12.4 — Catálogo operacional de onboarding por fonte

## Objetivo

Transformar templates de onboarding por fonte em algo consumível pela aplicação, para reduzir
trabalho manual e padronizar o checklist operacional antes da ativação de novos `TargetSite`s.

## Ciclo TDD

### Red

- Criados testes para o catálogo `TargetSiteOnboardingProfileCatalog`.
- Criados testes para o controller `GET /api/v1/onboarding-profiles` e
  `GET /api/v1/onboarding-profiles/{profileKey}`.
- O RED inicial falhou por ausência de:
  - catálogo de perfis;
  - use cases de leitura;
  - DTOs REST;
  - controller e exceção específica para perfil inexistente.

### Green

- Implementado catálogo em memória com o perfil curado `greenhouse_bitso`.
- Implementados use cases de listagem e detalhe.
- Expostos endpoints REST para leitura operacional dos perfis.
- `RestExceptionHandler` passou a responder `404` para perfil de onboarding inexistente.

### Refactor

- O desenho foi deixado genérico via `TargetSiteOnboardingProfileTemplate`, evitando acoplamento
  do fluxo operacional a Greenhouse, mesmo começando com um único perfil curado.
- O perfil Greenhouse existente foi reaproveitado como fonte do catálogo, sem duplicar checklist
  em outro formato.

## Arquivos criados / modificados

- `src/main/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileTemplate.java`
- `src/main/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalog.java`
- `src/main/java/com/campos/webscraper/application/onboarding/GetTargetSiteOnboardingProfileUseCase.java`
- `src/main/java/com/campos/webscraper/application/onboarding/ListTargetSiteOnboardingProfilesUseCase.java`
- `src/main/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileNotFoundException.java`
- `src/main/java/com/campos/webscraper/interfaces/dto/TargetSiteOnboardingProfileSummaryResponse.java`
- `src/main/java/com/campos/webscraper/interfaces/dto/TargetSiteOnboardingProfileResponse.java`
- `src/main/java/com/campos/webscraper/interfaces/rest/TargetSiteOnboardingProfileController.java`
- `src/main/java/com/campos/webscraper/interfaces/rest/RestExceptionHandler.java`
- `src/test/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalogTest.java`
- `src/test/java/com/campos/webscraper/interfaces/rest/TargetSiteOnboardingProfileControllerTest.java`

## Problemas encontrados

- O projeto já tinha perfis curados, mas eles ainda viviam como helpers internos.
- Não havia uma forma operacional de consultar “qual checklist usar” antes de ativar uma nova
  fonte.

## Causa raiz

Os perfis existentes foram criados para suportar stories anteriores, mas ainda não eram expostos
como catálogo reutilizável pela aplicação.

## Solução aplicada

- Introdução de um catálogo operacional de perfis.
- Exposição REST de sumário e detalhe.
- Padronização do formato de resposta com metadata do `TargetSite` e checklist completo.

## Lições aprendidas

- Um gate de ativação sem catálogo/template continua exigindo operação manual demais.
- Vale separar “perfil curado de onboarding” de “site persistido em produção”.

## Estado final

- `TargetSiteOnboardingProfileCatalogTest` verde
- `TargetSiteOnboardingProfileControllerTest` verde
- `GreenhouseBoardOnboardingProfileTest` verde
- `./mvnw -q -DskipTests compile` verde
