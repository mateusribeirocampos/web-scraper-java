# STORY-12.6 — Bootstrap de TargetSite a partir de perfis curados

## Objetivo

Eliminar mais um passo manual entre o catálogo operacional de onboarding e o gate de ativação,
permitindo materializar um `TargetSite` persistido diretamente de um `profileKey` curado.

## Ciclo TDD

### Red

- Criados testes para o caso de uso `BootstrapTargetSiteFromProfileUseCase`.
- Criados testes para o endpoint `POST /api/v1/onboarding-profiles/{profileKey}/bootstrap-target-site`.
- O RED inicial falhou por ausência de:
  - resultado de bootstrap;
  - caso de uso de materialização/upsert;
  - controller REST;
  - DTO de resposta.

### Green

- Implementado `BootstrapTargetSiteFromProfileUseCase` com upsert por `siteCode`.
- Implementado `TargetSiteBootstrapController`.
- Exposto endpoint REST para criar/atualizar `TargetSite` a partir do catálogo curado.
- O endpoint retorna:
  - `201 CREATED` quando cria um site novo;
  - `200 OK` quando atualiza um `TargetSite` já existente.

### Refactor

- O fluxo preserva `enabled`, `legalStatus` e `createdAt` quando o `TargetSite` já existe, para
  não desligar nem regredir compliance de um site operacional.
- O perfil curado Greenhouse passou a persistir o endpoint executável da API (`boards-api`) como
  `baseUrl`, evitando que o bootstrap gere um site incompatível com a strategy de runtime.

## Arquivos criados / modificados

- `src/main/java/com/campos/webscraper/application/onboarding/BootstrapStatus.java`
- `src/main/java/com/campos/webscraper/application/onboarding/BootstrappedTargetSite.java`
- `src/main/java/com/campos/webscraper/application/onboarding/BootstrapTargetSiteFromProfileUseCase.java`
- `src/main/java/com/campos/webscraper/application/onboarding/GreenhouseBoardOnboardingProfiles.java`
- `src/main/java/com/campos/webscraper/interfaces/dto/TargetSiteBootstrapResponse.java`
- `src/main/java/com/campos/webscraper/interfaces/rest/TargetSiteBootstrapController.java`
- `src/test/java/com/campos/webscraper/application/onboarding/BootstrapTargetSiteFromProfileUseCaseTest.java`
- `src/test/java/com/campos/webscraper/interfaces/rest/TargetSiteBootstrapControllerTest.java`
- `src/test/java/com/campos/webscraper/interfaces/rest/TargetSiteOnboardingProfileControllerTest.java`

## Problemas encontrados

- O catálogo operacional ainda exigia criação manual de `TargetSite` no banco antes da ativação.
- O perfil Greenhouse curado ainda carregava `baseUrl` HTML, enquanto a strategy exigia o
  endpoint executável da API.

## Solução aplicada

- Introdução de um bootstrap REST sem body, orientado por `profileKey`.
- Materialização idempotente por `siteCode`.
- Preservação de estado operacional em updates.
- Alinhamento do perfil Greenhouse ao `baseUrl` realmente consumido pela strategy.

## Estado final

- `BootstrapTargetSiteFromProfileUseCaseTest` verde
- `TargetSiteBootstrapControllerTest` verde
- `TargetSiteOnboardingProfileCatalogTest` verde
- `TargetSiteOnboardingProfileControllerTest` verde
- `GreenhouseBoardOnboardingProfileTest` verde
- `./mvnw -q -DskipTests compile` verde
