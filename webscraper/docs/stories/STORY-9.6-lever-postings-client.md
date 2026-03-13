# STORY 9.6 — Generalizar provider ATS para Lever

**Status:** ✅ Concluída
**Iteration:** 9 — Expansão do setor privado para PMEs via ATS público
**Data:** 2026-03-13
**Referência ADR:** ADR009 Story 9.6

---

## Objetivo

Validar que a expansão ATS não ficou acoplada à Greenhouse, introduzindo um segundo provider real:

- extrair uma abstração mínima compartilhada de provider ATS público
- implementar `LeverPostingsClient`
- validar o contrato com fixture JSON e teste de compatibilidade do provider

---

## Ciclo TDD

### RED — contract test do provider e fixture Lever primeiro

Foram criados:

- `AtsJobProviderClientContractTest`
- `LeverPostingsClientTest`

O RED inicial falhou por compilação, porque ainda não existiam:

- `AtsJobProviderClient`
- `LeverPostingsClient`
- DTOs do Lever

### GREEN — implementação mínima

Foi implementado:

1. `AtsJobProviderClient<T>`
2. adaptação de `GreenhouseJobBoardClient` para o contrato compartilhado
3. `LeverPostingsClient`
4. `LeverPostingResponse`
5. `LeverCategoriesResponse`
6. fixture `lever-postings-response.json`

Contrato entregue:

- Greenhouse e Lever agora compartilham a mesma interface de provider ATS
- o client Lever desserializa a lista pública de postings
- status não-2xx gera exceção descritiva

### REFACTOR

A abstração foi mantida propositalmente pequena:

- só o que já é comum entre Greenhouse e Lever
- sem antecipar normalização, strategy ou persistência do Lever antes da hora

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/infrastructure/http/AtsJobProviderClient.java` | Criado | Contrato compartilhado de provider ATS |
| `src/main/java/com/campos/webscraper/infrastructure/http/LeverPostingsClient.java` | Criado | Client HTTP do Lever |
| `src/main/java/com/campos/webscraper/interfaces/dto/LeverPostingResponse.java` | Criado | DTO de posting do Lever |
| `src/main/java/com/campos/webscraper/interfaces/dto/LeverCategoriesResponse.java` | Criado | DTO de categorias do Lever |
| `src/main/java/com/campos/webscraper/infrastructure/http/GreenhouseJobBoardClient.java` | Modificado | Passa a implementar o contrato ATS compartilhado |
| `src/test/java/com/campos/webscraper/infrastructure/http/AtsJobProviderClientContractTest.java` | Criado | Contract test entre Greenhouse e Lever |
| `src/test/java/com/campos/webscraper/infrastructure/http/LeverPostingsClientTest.java` | Criado | RED/GREEN do client Lever |
| `src/test/resources/fixtures/lever/lever-postings-response.json` | Criado | Fixture pública do Lever |
| `docs/stories/STORY-9.6-lever-postings-client.md` | Criado | Registro final da story |
| `docs/stories/README.md` | Modificado | Atualização do índice |

---

## Problemas encontrados

### Problema 1 — o provider ATS ainda estava implícito demais no nome Greenhouse

Depois da 9.5, o fluxo ATS funcionava, mas ainda não existia um contrato explícito que comprovasse
que um segundo provider poderia entrar sem duplicar toda a borda.

### Problema 2 — a 9.6 precisava provar generalização sem abrir uma segunda trilha completa

Se a story tentasse resolver client, normalizer, strategy, onboarding e persistência Lever de uma
vez, ela deixaria de validar a estabilidade da abstração e viraria outra mini-iteração inteira.

---

## Causa raiz

- Greenhouse foi o primeiro ATS público implementado
- até então, ainda não havia um segundo provider para forçar a borda comum a emergir

---

## Solução aplicada

- criada a interface `AtsJobProviderClient<T>`
- adaptado o client Greenhouse para o contrato compartilhado
- implementado `LeverPostingsClient` com fixture própria
- adicionado contract test mostrando que os dois providers respeitam a mesma forma de uso

---

## Lições aprendidas

- a abstração certa nesta fase é pequena e focada no transporte
- Greenhouse não era um caso especial; ele realmente já representava um padrão ATS reutilizável
- a próxima fatia Lever pode escolher com mais segurança se para em client/normalizer ou avança
  até strategy, porque a borda compartilhada já foi comprovada

---

## Estado final

- provider ATS compartilhado implementado
- Greenhouse adaptado ao contrato comum
- Lever client implementado com fixture e contract test
- testes unitários da fatia verdes

Validação executada:

- `./mvnw test -DexcludedGroups=integration -Dtest=AtsJobProviderClientContractTest,LeverPostingsClientTest,GreenhouseJobBoardClientTest`

Próximo passo natural:

- próxima story da iteração seguinte do ADR009
