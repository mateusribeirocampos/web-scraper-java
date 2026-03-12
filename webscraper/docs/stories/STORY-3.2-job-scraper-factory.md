# STORY 3.2 — JobScraperFactory

**Status:** ✅ Concluída
**Iteration:** 3 — Contratos de Strategy e Factory
**Data:** 2026-03-12
**Referência ADR:** ADR009 Story 3.2 / ADR004 Seção 2 e 4

---

## Objetivo

Implementar a factory responsável por resolver a strategy correta a partir dos metadados
explícitos de `TargetSiteEntity`, incluindo:

- contrato `JobScraperFactory`
- implementação `DefaultJobScraperFactory`
- exceção descritiva `UnsupportedSiteException`

Essa story fecha a base de resolução de strategies para o pipeline de extração.

---

## Ciclo TDD

### RED — testes escritos antes da implementação

Foi criado `JobScraperFactoryTest` cobrindo:

- resolução da primeira strategy compatível
- preservação da ordem de registro quando múltiplas strategies suportam o mesmo site
- erro descritivo quando nenhuma strategy suporta o site

A falha inicial veio pela ausência de `UnsupportedSiteException`, parte do escopo da story:

```text
cannot find symbol: class UnsupportedSiteException
```

### GREEN — implementação mínima

Foi implementado o mínimo necessário:

1. `JobScraperFactory` como contrato
2. `DefaultJobScraperFactory` como resolvedor por ordem de registro
3. `UnsupportedSiteException` como `RuntimeException`

A resolução final ficou baseada em:

- `siteCode`
- `siteType`
- `extractionMode`
- `jobCategory`

conforme os metadados usados pelas próprias strategies.

### REFACTOR

Sem necessidade de refatoração estrutural. A implementação foi mantida simples e in-memory,
o suficiente para sustentar a próxima story sem antecipar integração Spring ou scanning automático.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/application/factory/JobScraperFactory.java` | Criado | Contrato da factory de resolução |
| `src/main/java/com/campos/webscraper/application/factory/DefaultJobScraperFactory.java` | Criado | Implementação padrão baseada em lista ordenada de strategies |
| `src/main/java/com/campos/webscraper/shared/UnsupportedSiteException.java` | Criado | Exceção descritiva para sites sem suporte |
| `src/test/java/com/campos/webscraper/application/factory/JobScraperFactoryTest.java` | Criado | Testes unitários de resolução da factory |
| `docs/stories/README.md` | Modificado | Atualização do índice de stories |
| `docs/stories/STORY-3.2-job-scraper-factory.md` | Criado | Registro final da execução da story |

---

## Problemas encontrados

### Problema 1 — A história dependia de uma exceção de domínio ainda inexistente

Os testes da factory precisavam garantir erro descritivo quando nenhuma strategy suportasse o site.
Isso exigiu introduzir `UnsupportedSiteException` no mesmo ciclo.

---

## Causa raiz

A regra arquitetural do ADR004 já previa essa exceção, mas ela ainda não existia no código.
Sem ela, a factory não conseguiria comunicar falha de resolução de forma explícita.

---

## Solução aplicada

- criado `JobScraperFactory` como interface mínima
- criada `DefaultJobScraperFactory` que:
  - recebe uma lista de `JobScraperStrategy<?>`
  - percorre em ordem de registro
  - retorna a primeira strategy compatível
  - lança `UnsupportedSiteException` com metadados do site quando não encontra suporte
- criada `UnsupportedSiteException` em `shared`

---

## Lições aprendidas

- a ordem de registro da factory é um detalhe importante e agora está protegida por teste
- a exceção descritiva evita falhas opacas quando um site ainda não tem strategy registrada
- a combinação de contract tests da 3.1 com resolution tests da 3.2 já estabiliza bem a camada
  de extração antes das implementações concretas

---

## Estado final

Resultado da suite unitária:

```text
Tests run: 136, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Comando executado:

```bash
./mvnw test -DexcludedGroups="integration"
```

Conclusão:

- `JobScraperFactory` implementada
- `UnsupportedSiteException` implementada
- testes unitários verdes
- story pronta para a 3.3 (`JobFetcher`)
