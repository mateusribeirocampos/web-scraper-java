# Story 14.2 — Onboarding/activation readiness signal

## Objetivo

Fechar o loop operacional do fluxo `profile → bootstrap → smoke-run → activation-assistance →
activation` adicionando um sinal derivado de `activationReady` ao resultado do operational check.
Antes desta story, após rodar o check o operador precisava interpretar manualmente o resultado e
decidir se a fonte estava pronta — não havia sinal explícito.

## Ciclo TDD

### RED

Criado `OnboardingOperationalCheckResultTest` com 11 testes cobrindo `activationReady()` e
`activationBlockers()` — falha de compilação confirmada (métodos inexistentes).

### GREEN

Adicionados `activationReady()` e `activationBlockers()` ao record
`OnboardingOperationalCheckResult` com lógica mínima:

- `BLOCKED_BY_COMPLIANCE` → retorno imediato com um blocker de compliance
- `executionSummary == null` → blocker "no execution recorded"
- `status != SUCCEEDED` → blocker com status atual
- `recentPostingsCount == 0` → blocker "no postings collected"
- `activationReady()` → verdadeiro somente quando `activationBlockers()` está vazio

### REFACTOR

Exposto `activationReady` e `activationBlockers` no DTO
`OnboardingOperationalCheckResponse` e no `OnboardingOperationalCheckController`.
Testes do controller atualizados para cobrir os novos campos nas respostas de sucesso e de
compliance bloqueado.

## Arquivos criados / modificados

| Arquivo | Papel |
|---|---|
| `application/onboarding/OnboardingOperationalCheckResult.java` | +`activationReady()`, +`activationBlockers()` |
| `interfaces/dto/OnboardingOperationalCheckResponse.java` | +`activationReady`, +`activationBlockers` |
| `interfaces/rest/OnboardingOperationalCheckController.java` | projeta os novos campos no DTO |
| `application/onboarding/OnboardingOperationalCheckResultTest.java` | **novo** — 11 testes unitários |
| `interfaces/rest/OnboardingOperationalCheckControllerTest.java` | +2 asserções nos testes existentes |

## Validação

- 564 testes unitários passando (553 anteriores + 11 novos)
- Resposta `GET /api/v1/onboarding-profiles/{profileKey}/operational-check` agora inclui:
  - `activationReady: true/false`
  - `activationBlockers: [...]` com razões legíveis quando não está pronto

## Lições aprendidas

- O record `OnboardingOperationalCheckResult` concentrou toda a lógica de derivação — o controller
  ficou simples de atualizar, apenas projetando campos já calculados.
- O padrão de retornar `BLOCKED_BY_COMPLIANCE` como blocker exclusivo (retorno imediato) evita
  combinar mensagens de compliance com mensagens de runtime — o operador vê exatamente um motivo
  claro quando o legal está pendente.

## Estado final

- Compilação: OK
- Testes unitários: 564/564 GREEN
- Sem regressões
