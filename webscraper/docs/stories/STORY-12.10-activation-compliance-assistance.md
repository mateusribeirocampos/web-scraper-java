# STORY-12.10 — Assistente de evidências de compliance

## Objetivo

Reduzir o preenchimento manual antes da ativação de um `TargetSite`, gerando um draft assistido de
compliance para `robots.txt`, ToS e endpoint oficial.

## Ciclo TDD

### Red

- Criados testes para o caso de uso `GetTargetSiteActivationAssistanceUseCase`.
- Criados testes para o endpoint `GET /api/v1/target-sites/{siteId}/activation-assistance`.
- O RED inicial falhou por ausência de:
  - caso de uso de assistência;
  - DTO consolidado de resposta;
  - controller REST;
  - lookup do catálogo por `siteCode`.

### Green

- Implementado `GetTargetSiteActivationAssistanceUseCase`.
- Exposto endpoint `GET /api/v1/target-sites/{siteId}/activation-assistance`.
- O fluxo:
  - usa checklist curado quando o `siteCode` bate com um perfil do catálogo;
  - deriva um draft mínimo de evidências quando não há perfil curado;
  - calcula `blockingReasonsIfActivatedNow` pelo mesmo `TargetSiteOnboardingValidator` do gate real.

### Refactor

- A assistência não ativa o site nem burla o gate.
- O contrato devolve um draft revisável pelo operador, não uma aprovação automática.
- O fallback derivado reaproveita `baseUrl`, `siteType`, `extractionMode` e `jobCategory` do
  `TargetSite` persistido.

## Arquivos criados / modificados

- `src/main/java/com/campos/webscraper/application/onboarding/ActivationAssistanceSource.java`
- `src/main/java/com/campos/webscraper/application/onboarding/TargetSiteActivationAssistance.java`
- `src/main/java/com/campos/webscraper/application/onboarding/GetTargetSiteActivationAssistanceUseCase.java`
- `src/main/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalog.java`
- `src/main/java/com/campos/webscraper/interfaces/dto/TargetSiteActivationAssistanceResponse.java`
- `src/main/java/com/campos/webscraper/interfaces/rest/TargetSiteActivationAssistanceController.java`
- `src/test/java/com/campos/webscraper/application/onboarding/GetTargetSiteActivationAssistanceUseCaseTest.java`
- `src/test/java/com/campos/webscraper/interfaces/rest/TargetSiteActivationAssistanceControllerTest.java`
- `src/test/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalogTest.java`

## Estado final

- `GetTargetSiteActivationAssistanceUseCaseTest` verde
- `TargetSiteActivationAssistanceControllerTest` verde
- `TargetSiteOnboardingProfileCatalogTest` verde
- `./mvnw -q -DskipTests compile` verde
