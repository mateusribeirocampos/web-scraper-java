# STORY 9.2 — Implementar GreenhouseJobBoardClient

**Status:** ✅ Concluída
**Iteration:** 9 — Expansão do setor privado para PMEs via ATS público
**Data:** 2026-03-13
**Referência ADR:** ADR009 Story 9.2

---

## Objetivo

Implementar o client HTTP da Greenhouse Job Board API para consumir o board escolhido na 9.1:

- desserializar a resposta pública de vagas
- padronizar o DTO mínimo que a 9.3 vai normalizar
- validar a integração por fixture JSON antes de qualquer normalização

---

## Ciclo TDD

### RED — fixture JSON do board escolhido primeiro

Foi criado `GreenhouseJobBoardClientTest` cobrindo:

- desserialização bem-sucedida de vagas publicadas do board `bitso`
- exceção descritiva quando a Greenhouse Job Board API responde com status não-2xx

O RED inicial falhou por compilação, porque ainda não existiam:

- `GreenhouseJobBoardClient`
- `GreenhouseJobBoardResponse`
- `GreenhouseJobBoardItemResponse`
- `GreenhouseLocationResponse`

### GREEN — implementação mínima

Foi implementado:

1. `GreenhouseJobBoardClient`
2. `GreenhouseJobBoardResponse`
3. `GreenhouseJobBoardItemResponse`
4. `GreenhouseLocationResponse`
5. fixture `greenhouse-bitso-jobs-response.json`

Contrato entregue:

- `fetchPublishedJobs(String url)` faz `GET` no endpoint público do board
- resposta 2xx com body válido retorna a lista de `jobs`
- body ausente ou status não-2xx gera `IllegalStateException` descritiva
- campos snake_case da Greenhouse (`absolute_url`, `company_name`, `first_published`) foram
  mapeados explicitamente para DTOs Java
- o endpoint curado do board `bitso` usa `?content=true`, preservando `content` no fluxo real da
  integração

### REFACTOR

O client foi mantido simples e sem lógica de normalização. A story precisava apenas fechar a borda
HTTP da Greenhouse; parsing de campos de negócio fica para a 9.3.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/infrastructure/http/GreenhouseJobBoardClient.java` | Criado | Client HTTP da Greenhouse Job Board API |
| `src/main/java/com/campos/webscraper/interfaces/dto/GreenhouseJobBoardResponse.java` | Criado | DTO raiz da resposta |
| `src/main/java/com/campos/webscraper/interfaces/dto/GreenhouseJobBoardItemResponse.java` | Criado | DTO de vaga publicada da Greenhouse |
| `src/main/java/com/campos/webscraper/interfaces/dto/GreenhouseLocationResponse.java` | Criado | DTO do bloco de localização |
| `src/test/java/com/campos/webscraper/infrastructure/http/GreenhouseJobBoardClientTest.java` | Criado | RED/GREEN do client HTTP |
| `src/test/resources/fixtures/greenhouse/greenhouse-bitso-jobs-response.json` | Criado | Fixture do board `bitso` |
| `docs/stories/STORY-9.2-greenhouse-job-board-client.md` | Criado | Registro final da story |
| `docs/stories/README.md` | Modificado | Atualização do índice |

---

## Problemas encontrados

### Problema 1 — a Greenhouse usa nomes snake_case diferentes dos DTOs já existentes no projeto

Campos como `absolute_url`, `company_name` e `first_published` não seguem o mesmo padrão dos DTOs
de Indeed e DOU, então a desserialização precisava de mapeamento explícito.

### Problema 2 — a 9.3 depende de descrição e metadata básicas já presentes no client

Se o client devolvesse um DTO pobre demais, a normalização da 9.3 precisaria reabrir a borda HTTP
ou redefinir o contrato do payload.

### Problema 3 — o DTO inicial quebrava com campos extras da resposta real da Greenhouse

No review apareceu a lacuna principal da 9.2:

- a fixture inicial modelava só um subconjunto enxuto do payload
- a API real da Greenhouse costuma enviar campos adicionais como `updated_at`, `departments` e
  `metadata`
- com o comportamento default do Jackson, isso poderia quebrar a desserialização com
  `UnrecognizedPropertyException`

---

## Causa raiz

- a Greenhouse Job Board API tem shape próprio e mais rico que o Indeed MCP usado antes
- o projeto ainda não tinha nenhum DTO preparado para o padrão snake_case da Greenhouse
- a 9.2 precisava fechar primeiro a borda de transporte, antes de discutir regras de normalização
- o primeiro DTO estava estrito demais para o payload real da API

---

## Solução aplicada

- criado client HTTP dedicado para a Greenhouse Job Board API
- criado DTO raiz com lista de jobs
- criado DTO por vaga com mapeamento explícito dos campos snake_case
- criada fixture do board `bitso` com campos suficientes para a 9.3
- mantido tratamento de erro consistente com os clients já existentes do projeto
- ajustado pós-review: DTOs da Greenhouse agora ignoram campos desconhecidos
- a fixture foi enriquecida com campos extras reais para validar compatibilidade com o payload live
- ajustado pós-review: o profile onboardado do board `bitso` foi alinhado para usar `?content=true`

---

## Lições aprendidas

- a 9.1 acertou em escolher o board antes do client; a fixture da 9.2 já nasce amarrada a um alvo
  real
- vale manter DTOs de transporte mínimos, mas já compatíveis com a próxima camada de normalização
- a Greenhouse merece um contrato próprio, não uma adaptação forçada dos DTOs existentes
- para clients de API pública, fixture reduzida demais pode esconder incompatibilidades reais do
  payload

---

## Estado final

- `GreenhouseJobBoardClient` implementado
- fixture JSON do board `bitso` criada
- DTOs mínimos da Greenhouse definidos
- testes da fatia HTTP verdes

Validação executada:

- `./mvnw test -DexcludedGroups=integration -Dtest=GreenhouseJobBoardClientTest`

Próximo passo natural:

- Story 9.3 — implementar `GreenhouseJobNormalizer`
