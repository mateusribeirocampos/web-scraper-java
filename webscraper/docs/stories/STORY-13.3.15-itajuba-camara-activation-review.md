# Story 13.3.15 — Itajubá pública: ativação operacional/legal da Câmara

## Objetivo

Fechar a revisão de compliance da trilha pública `camara_itajuba` e decidir sua promoção
operacional para `APPROVED/enabled=true`.

## Evidências revisadas

- `https://itajuba.cam.mg.gov.br/robots.txt`
- `https://itajuba.cam.mg.gov.br/site/camara-municipal-de-itajuba-lanca-concurso-publico-para-preenchimento-de-cargos-efetivos/`
- `https://itajuba.cam.mg.gov.br/site/lgpd-lei-geral-de-protecao-de-dados-2/`

## Decisão

- a fonte pública da Câmara de Itajubá foi promovida para `APPROVED/enabled=true`;
- a base revisada foi considerada suficiente por ser:
  - portal institucional oficial;
  - página pública do concurso;
  - política de privacidade/LGPD acessível no mesmo domínio;
  - coleta de dados públicos sem autenticação nem bypass técnico.

## Arquivos modificados

- `webscraper/src/main/java/com/campos/webscraper/application/onboarding/CoreSourceOnboardingProfiles.java`
- `webscraper/src/main/resources/db/migration/V014__approve_camara_itajuba.sql`
- `webscraper/src/test/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalogTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingValidatorTest.java`
- `webscraper/src/test/java/com/campos/webscraper/application/onboarding/BootstrapTargetSiteFromProfileUseCaseTest.java`

## Validação

- testes automatizados:
  - `./mvnw -Dtest=TargetSiteOnboardingProfileCatalogTest,TargetSiteOnboardingValidatorTest,BootstrapTargetSiteFromProfileUseCaseTest test`

## Estado final

- `camara_itajuba` aprovada no perfil curado;
- reconciliação persistida via migration para ambientes já bootstrapados;
- a cidade `Itajubá` segue pendente apenas da trilha privada `Airbus/Helibras` via `Workday`.
