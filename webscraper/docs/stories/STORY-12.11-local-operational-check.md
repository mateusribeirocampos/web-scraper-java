# STORY-12.11 — Check operacional local ponta a ponta

## Objetivo

Transformar o fluxo real do usuário em um caminho reproduzível ponta a ponta, cobrindo start local
da aplicação, bootstrap/execução e leitura consolidada do resultado operacional.

## Ciclo TDD

### Red

- Criados testes para `RunOnboardingOperationalCheckUseCase`.
- Criados testes para `POST /api/v1/onboarding-profiles/{profileKey}/operational-check`.
- O RED inicial falhou por ausência de:
  - caso de uso consolidado;
  - DTOs de resumo operacional;
  - controller REST;
  - automação local do fluxo.

### Green

- Implementado `RunOnboardingOperationalCheckUseCase`.
- Exposto endpoint `POST /api/v1/onboarding-profiles/{profileKey}/operational-check`.
- O fluxo consolida:
  - bootstrap do `TargetSite`;
  - bootstrap do `CrawlJob`;
  - smoke run opcional;
  - última execução observada;
  - contagem e amostra de vagas recentes persistidas.
- O script local também consulta `/api/v1/job-postings` ao final do fluxo.
- O default dessa consulta ficou em `JAVA_JUNIOR_BACKEND` sem `seniority` explícito, para validar
  `junior + pleno` no mercado real sem abrir para `senior/lead`.

### Refactor

- A regra de negócio fica no backend; o script local só orquestra start + health + chamada única.
- O script não reimplementa bootstrap, dispatch nem consultas SQL fora da aplicação.
- O resumo final do endpoint já nasce consumível para validação operacional do usuário.
- A validação funcional local deixa de depender apenas do recorte estrito `JUNIOR`, reduzindo falso
  negativo operacional em um mercado mais seco.

## Arquivos criados / modificados

- `src/main/java/com/campos/webscraper/application/onboarding/OnboardingOperationalCheckExecutionSummary.java`
- `src/main/java/com/campos/webscraper/application/onboarding/OnboardingOperationalCheckResult.java`
- `src/main/java/com/campos/webscraper/application/onboarding/RunOnboardingOperationalCheckUseCase.java`
- `src/main/java/com/campos/webscraper/interfaces/dto/OnboardingOperationalCheckExecutionResponse.java`
- `src/main/java/com/campos/webscraper/interfaces/dto/OnboardingOperationalCheckResponse.java`
- `src/main/java/com/campos/webscraper/interfaces/rest/OnboardingOperationalCheckController.java`
- `src/test/java/com/campos/webscraper/application/onboarding/RunOnboardingOperationalCheckUseCaseTest.java`
- `src/test/java/com/campos/webscraper/interfaces/rest/OnboardingOperationalCheckControllerTest.java`
- `scripts/run-local-operational-check.sh`

## Estado final

- `RunOnboardingOperationalCheckUseCaseTest` verde
- `OnboardingOperationalCheckControllerTest` verde
- `./mvnw -q -DskipTests compile` verde
