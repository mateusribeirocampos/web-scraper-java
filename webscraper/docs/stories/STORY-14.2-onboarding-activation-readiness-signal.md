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

## Correções pós-revisão — rodada 1 (P2)

### [P2] smokeRunRequested=false não bloqueava

`activationBlockers()` não verificava `workflow.smokeRunRequested()`. Uma fonte com execução
anterior bem-sucedida e postings recentes era reportada como pronta mesmo sem o smoke run ter
sido exercido neste check — compliance só é detectada quando `workflow.smokeRun()` está presente.

**Correção:** `smokeRunRequested=false` adicionado como blocker imediato (rodada 1, revertido na rodada 2).

### [P2] recentPostingsCount como gate acoplava o sinal ao parâmetro daysBack

Usar `recentPostingsCount == 0` como blocker fazia `activationReady` depender do parâmetro de
lookback do caller — a mesma fonte podia ser "não pronta" com `daysBack=1` e "pronta" com
`daysBack=60`, apenas por diferença de janela de inspeção.

**Correção:** `recentPostingsCount` removido dos critérios de `activationBlockers()`.

## Correções pós-revisão — rodada 2 (P2)

### [P2] smokeRunRequested=false bloqueava chamadas observation-only válidas

Bloquear `smokeRunRequested=false` imediatamente tornava `activationReady=false` mesmo para
fontes com execução bem-sucedida anterior — o sinal ficou acoplado ao parâmetro da chamada,
não ao estado da fonte. Um caller que omite `smokeRun=true` para apenas inspecionar postings
recentes não deveria ver uma fonte válida marcada como não pronta.

**Correção:** gate `smokeRunRequested` removido. `activationBlockers()` avalia apenas o estado
da fonte: compliance block, execução ausente, execução não-SUCCEEDED, ou `itemsFound == 0`.

### [P2] SUCCEEDED + itemsFound=0 não era detectado

Sem o gate de `recentPostingsCount`, uma execução que retornou SUCCEEDED mas não coletou nenhum
item passaria como pronta — regressão de seletor/parsing ficaria invisível.

**Correção:** `executionSummary.itemsFound() == 0` adicionado como blocker não-acoplado ao
parâmetro `daysBack`. Usa o campo da execução, não a janela de lookback do caller.

Critérios finais de `activationBlockers()`:
1. `smokeRun != null && status == BLOCKED_BY_COMPLIANCE` → compliance block
2. `executionSummary == null` → nenhuma execução registrada
3. `executionSummary.status() != SUCCEEDED` → execução não bem-sucedida
4. `executionSummary.itemsFound() == 0` → execução SUCCEEDED sem itens coletados

Novos testes adicionados:
- `returnsTrueForObservationOnlyCallWhenPreviousExecutionSucceededWithItems`
- `returnsFalseWhenExecutionSucceededButFoundNoItems`
- `doesNotBlockObservationOnlyCallsWhenPreviousExecutionSucceededWithItems`
- `reportsEmptyItemsFoundWhenExecutionSucceededWithNoItems`
- `doesNotReportZeroPostingsCountAsABlocker`

## Correções pós-revisão — rodada 3 (P2)

### [P2] itemsFound == 0 não é sinal de regressão

`executionSummary.itemsFound() == 0` é um estado válido — uma fonte que não tem vagas abertas
no momento retorna SUCCEEDED com 0 itens. Usar isso como blocker marcaria permanentemente como
"não pronta" qualquer fonte legítima com pipeline saudável mas vaga zerada.

**Correção:** gate `itemsFound == 0` removido.

### [P2] recentPostingsCount deve ser o gate — captura imports archive-only

Um crawl que importa apenas postings históricos (fora da janela `daysBack`) pode ter
`itemsFound > 0` mas `recentPostingsCount == 0`. Com `itemsFound` como único gate, essa fonte
apareceria como `activationReady=true` mesmo que a resposta mostrasse zero postings recentes
e sample vazio.

`RunOnboardingOperationalCheckUseCase` já computa `recentPostingsCount` de `publishedAt >= since`,
o que é exatamente o sinal correto: a fonte está entregando conteúdo dentro da janela solicitada?

**Correção:** `recentPostingsCount == 0` restaurado como gate com mensagem descritiva:
`"execution succeeded but no postings found in the requested window — source may only contain historical items"`

Critérios finais de `activationBlockers()`:
1. `smokeRun != null && status == BLOCKED_BY_COMPLIANCE` → compliance block (retorno imediato)
2. `executionSummary == null` → nenhuma execução registrada
3. `executionSummary.status() != SUCCEEDED` → execução não bem-sucedida
4. `recentPostingsCount == 0` → sem postings na janela solicitada (possível import archive-only)

## Estado final

- Compilação: OK
- Testes unitários: 567/567 GREEN
- Sem regressões
