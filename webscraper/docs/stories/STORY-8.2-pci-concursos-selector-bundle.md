# STORY 8.2 — Implementar SelectorBundle para PCI Concursos

**Status:** ✅ Concluída
**Iteration:** 8 — Primeiro scraper HTML estático: PCI Concursos
**Data:** 2026-03-12
**Referência ADR:** ADR009 Story 8.2

---

## Objetivo

Extrair os seletores inline do parser do PCI para um `SelectorBundle` versionado, com metadata
explícita de parser e bundle, mantendo o parse da fixture da 8.1 funcional.

O foco desta story é:

- criar `SelectorBundle`
- versionar o bundle do PCI como `pci_concursos_v1`
- fazer o parser consumir o bundle
- validar por testes que os seletores mapeados continuam produzindo o preview esperado

---

## Ciclo TDD

### RED — testes de mapeamento de seletores primeiro

Foram criados:

- `SelectorBundleTest`
- `PciConcursosSelectorBundleParserTest`

O RED inicial falhou por compilação, porque ainda não existiam:

- `SelectorBundle`
- `PciConcursosSelectorBundles`
- construtor do parser baseado em bundle

### GREEN — implementação mínima

Foi implementado:

1. `SelectorBundle` como record com metadata e mapa de seletores
2. `PciConcursosSelectorBundles.v1()` retornando `pci_concursos_v1`
3. refactor do `PciConcursosFixtureParser` para:
   - usar bundle default no construtor sem argumentos
   - aceitar bundle customizado no construtor injetável
   - substituir os seletores inline por `selectorBundle.selector(...)`
   - validar seletores obrigatórios logo na injeção do bundle

### REFACTOR

O parser permaneceu simples e sem Spring wiring. O objetivo foi apenas isolar os seletores em um
objeto versionado, preparando a Strategy da 8.3.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/infrastructure/parser/SelectorBundle.java` | Criado | Contrato versionado de bundle de seletores |
| `src/main/java/com/campos/webscraper/infrastructure/parser/PciConcursosSelectorBundles.java` | Criado | Factory do bundle `pci_concursos_v1` |
| `src/main/java/com/campos/webscraper/infrastructure/parser/PciConcursosFixtureParser.java` | Modificado | Parser passa a consumir o bundle |
| `src/test/java/com/campos/webscraper/infrastructure/parser/SelectorBundleTest.java` | Criado | Teste do metadata e mapeamento do bundle |
| `src/test/java/com/campos/webscraper/infrastructure/parser/PciConcursosSelectorBundleParserTest.java` | Criado | Teste de integração parser + bundle |
| `docs/stories/README.md` | Modificado | Atualização do índice |
| `docs/stories/STORY-8.2-pci-concursos-selector-bundle.md` | Criado | Registro final da story |

---

## Problemas encontrados

### Problema 1 — os seletores estavam embutidos no parser

A 8.1 entregou valor rápido para fixture e preview, mas ainda sem versionamento explícito dos
seletores.

### Problema 2 — era necessário não quebrar a observabilidade da 8.1

O parser já tinha vários testes de robustez. A extração para bundle não podia reintroduzir drift
ou regressões no preview.

### Problema 3 — bundle incompleto falhava tarde demais

No primeiro corte da 8.2, um bundle customizado sem algum campo obrigatório só quebrava durante o
parse, com erro opaco vindo do `jsoup`.

---

## Causa raiz

- a 8.1 intencionalmente usou seletores inline para não misturar escopo
- a 8.2 é a primeira story que formaliza esses seletores como contrato versionado
- a primeira versão do contrato ainda aceitava bundles incompletos sem fail-fast

---

## Solução aplicada

- criado `SelectorBundle` com:
  - `siteCode`
  - `strategyName`
  - `parserVersion`
  - `selectorBundleVersion`
  - `effectiveFrom`
  - `deprecatedAt`
  - `selectors`
- criado `PciConcursosSelectorBundles.v1()` com os campos:
  - `contestCard`
  - `contestName`
  - `organizer`
  - `positionTitle`
  - `numberOfVacancies`
  - `educationLevel`
  - `salaryRange`
  - `registrationDeadline`
  - `detailUrl`
- refatorado `PciConcursosFixtureParser` para usar o bundle default `pci_concursos_v1`
- adicionado `requireSelectors(...)` no `SelectorBundle`
- o parser agora rejeita bundle incompleto com `IllegalArgumentException` clara na construção

---

## Lições aprendidas

- bundle versionado deixa selector drift rastreável sem poluir a strategy
- o parser fica mais testável quando aceita bundle por construtor
- a 8.1 e a 8.2 juntas já deixam o PCI preparado para a 8.3 com baixo risco de regressão

---

## Estado final

Resultado validado:

```text
Tests run: 185, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Comando executado:

```bash
./mvnw test -DexcludedGroups=integration
```

Conclusão:

- `SelectorBundle` implementado
- `pci_concursos_v1` versionado
- parser do PCI usando bundle
- testes verdes
- story pronta para review
