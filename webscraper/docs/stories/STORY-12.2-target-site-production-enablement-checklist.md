# STORY-12.2 — Checklist de habilitação de site em produção

## Objetivo

Tornar o checklist de onboarding consumível pela aplicação, bloquear ativação de `TargetSite`
sem compliance completo e expor um endpoint explícito para esse gate operacional.

## Ciclo TDD

### Red

- Criados testes para o novo caso de uso `ActivateTargetSiteUseCase`.
- Criados testes para o endpoint `POST /api/v1/target-sites/{siteId}/activation`.
- O primeiro RED falhou por ausência de:
  - caso de uso de ativação;
  - controller dedicado;
  - exceção específica para bloqueio por compliance.

### Green

- Implementado `ActivateTargetSiteUseCase`.
- `TargetSiteOnboardingValidator` virou bean Spring explícito.
- Implementado `TargetSiteActivationController`.
- Adicionados DTOs de request/response e erro de bloqueio.
- `RestExceptionHandler` passou a mapear bloqueio de ativação para `409 CONFLICT`.

### Refactor

- O fluxo foi ajustado para persistir sempre o estado derivado do onboarding (`APPROVED`,
  `PENDING_REVIEW` ou `SCRAPING_PROIBIDO`), mesmo quando a ativação fica bloqueada.
- O gate continua impedindo `enabled=true` sem checklist completo, mas a aplicação agora mantém
  memória operacional do resultado da avaliação.

## Arquivos criados / modificados

- `src/main/java/com/campos/webscraper/application/usecase/ActivateTargetSiteUseCase.java`
- `src/main/java/com/campos/webscraper/interfaces/rest/TargetSiteActivationController.java`
- `src/main/java/com/campos/webscraper/interfaces/dto/TargetSiteActivationRequest.java`
- `src/main/java/com/campos/webscraper/interfaces/dto/TargetSiteActivationResponse.java`
- `src/main/java/com/campos/webscraper/interfaces/dto/TargetSiteActivationBlockedErrorResponse.java`
- `src/main/java/com/campos/webscraper/shared/TargetSiteActivationBlockedException.java`
- `src/main/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingValidator.java`
- `src/main/java/com/campos/webscraper/interfaces/rest/RestExceptionHandler.java`
- `src/test/java/com/campos/webscraper/application/usecase/ActivateTargetSiteUseCaseTest.java`
- `src/test/java/com/campos/webscraper/interfaces/rest/TargetSiteActivationControllerTest.java`

## Problemas encontrados

- O projeto já tinha validator e checklist, mas eles existiam apenas como lógica isolada.
- Não havia nenhum fluxo de aplicação/REST que impedisse a ativação de `TargetSite` sem passar por
  essa avaliação.
- Persistir nada em caso de bloqueio deixaria o resultado do onboarding invisível operacionalmente.

## Causa raiz

O onboarding foi introduzido em stories anteriores como regra de domínio e documentação, mas não
foi conectado a um caso de uso transacional de ativação.

## Solução aplicada

- Materialização do checklist como payload HTTP.
- Avaliação centralizada via `TargetSiteOnboardingValidator`.
- Persistência do estado reconciliado do `TargetSite`.
- Bloqueio explícito da ativação com `409 CONFLICT` quando a compliance não fecha.

## Lições aprendidas

- Regra de domínio sem fluxo aplicacional continua sendo só documentação executável em teste.
- Para governança operacional, não basta rejeitar a ação; é útil persistir o status derivado da
  revisão legal/técnica.
- Wiring por construtor e bean explícito evita regressões de runtime/IntelliJ no crescimento do
  fluxo.

## Estado final

- `ActivateTargetSiteUseCaseTest` verde
- `TargetSiteActivationControllerTest` verde
- `TargetSiteOnboardingValidatorTest` verde
- `./mvnw -q -DskipTests compile` verde
