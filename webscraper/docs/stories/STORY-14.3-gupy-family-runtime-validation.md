# Story 14.3 — Gupy family runtime validation

## Objetivo

Fechar o gap de cobertura da família Gupy na trilha de runtime validation.
Antes desta story:

- `GupyJobScraperStrategy` não tinha nenhum teste de `supports()` nem de `scrape()`.
- `GupyJobImportUseCase` não tinha nenhum teste de unidade.
- `GupyJobImportUseCase.execute()` chamava `strategy.scrape(command).items()` diretamente,
  ignorando o campo `success` de `ScrapeResult` — falhas de scrape produziam lista vazia e
  eram persistidas silenciosamente como importação de zero itens, sem propagar o erro.

O padrão correto estava documentado em `WorkdayJobImportUseCase`, que checava `result.success()`
e lançava `IllegalStateException` com a mensagem de erro.

## Ciclo TDD

### RED

Criado `GupyJobImportUseCaseTest` com 2 testes:

1. `shouldPropagateScrapeFailuresInsteadOfPersistingEmptySuccessfulImports` — **falhou**
   porque `GupyJobImportUseCase` não lançava exceção em caso de falha de scrape.
2. `shouldPersistNormalizedItemsWhenSpecialDogScrapeSucceeds` — passava.

Criado `GupyJobScraperStrategyTest` com 6 testes — todos passaram de imediato (lógica de
`supports()` já estava correta, apenas sem cobertura).

### GREEN

Corrigido `GupyJobImportUseCase.execute()`:

```java
// antes:
List<JobPostingEntity> items = strategy.scrape(command).items();

// depois:
ScrapeResult<JobPostingEntity> result = strategy.scrape(command);
if (!result.success()) {
    throw new IllegalStateException("Gupy scrape failed: " + result.errorMessage());
}
```

### REFACTOR

Nenhuma refatoração adicional necessária — a fix foi cirúrgica e o código ficou no mesmo
padrão do `WorkdayJobImportUseCase`.

## Bug encontrado

`GupyJobImportUseCase` não verificava `ScrapeResult.success()`. Um scrape falhado (ex.:
timeout, HTTP 503) produzia `ScrapeResult.failure(source, message)` com `items=[]`.
Chamando `.items()` diretamente, o use case obtinha lista vazia, enriquecia zero itens e
chamava `idempotentPersistenceService.persist([])` — sem propagar o erro.

Efeito prático: o `CrawlExecutionRunner` registrava `itemsFound=0` e `status=SUCCEEDED`
para execuções que deveriam ter status `FAILED`, mascarando falhas de rede ou parsing do
board Gupy.

## Arquivos criados / modificados

| Arquivo | Papel |
|---|---|
| `application/usecase/GupyJobImportUseCaseTest.java` | **novo** — 2 testes unitários |
| `application/strategy/GupyJobScraperStrategyTest.java` | **novo** — 6 testes unitários |
| `application/usecase/GupyJobImportUseCase.java` | fix: adiciona verificação de `result.success()` |

## Validação

- Testes unitários: 575/575 GREEN (567 anteriores + 8 novos)
- Sem regressões

## Lições aprendidas

- `ScrapeResult.failure()` retorna `items=[]` — chamar `.items()` em vez de checar
  `.success()` primeiro é um padrão silenciosamente errado que não gera exceção nem
  compilação error, apenas dados errados.
- O padrão correto — checar `success()` e lançar `IllegalStateException` com a
  mensagem do erro — deve ser replicado em todos os import use cases.
- `GupyJobScraperStrategy.supports()` tinha 7 condições all-AND sem nenhum teste
  negativo — qualquer relaxamento futuro de uma dessas condições passaria despercebido.

## Estado final

- Compilação: OK
- Testes unitários: 575/575 GREEN
- Sem regressões
