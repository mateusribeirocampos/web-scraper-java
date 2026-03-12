# STORY 8.1 — Capturar fixture HTML do PCI Concursos

**Status:** ✅ Concluída
**Iteration:** 8 — Primeiro scraper HTML estático: PCI Concursos
**Data:** 2026-03-12
**Referência ADR:** ADR009 Story 8.1

---

## Objetivo

Criar a base testável do scraper HTML do PCI Concursos antes da strategy real:

- salvar uma fixture HTML representativa da listagem
- salvar o output esperado do parse em formato estruturado
- implementar um parser preview mínimo em `jsoup`
- produzir um retorno observável para sabermos se o parse está funcionando

O retorno observável desta story é `PciConcursosParsePreview`, que informa:

- `sourceUrl`
- `selectorBundleVersion`
- `itemsFound`
- lista normalizada de itens extraídos

---

## Ciclo TDD

### RED — teste de fixture antes da implementação

Foi criado `PciConcursosFixtureParserTest` lendo:

- `fixtures/pci/pci-concursos-listing.html`
- `fixtures/pci/pci-concursos-listing-expected.json`

O teste exige que o parser:

- encontre 2 cards
- mantenha `selectorBundleVersion = pci_concursos_v1`
- gere um preview estruturado idêntico ao JSON esperado

### GREEN — implementação mínima

Foi implementado `PciConcursosFixtureParser` com seletores inline provisórios e parse via `jsoup`.

Também foram criados:

- `PciConcursosParsePreview`
- `PciConcursosPreviewItem`

O parser extrai da fixture:

- `contestName`
- `organizer`
- `positionTitle`
- `numberOfVacancies`
- `educationLevel`
- `salaryDescription`
- `registrationStartDate`
- `registrationEndDate`
- `detailUrl`

### REFACTOR

O parser foi mantido propositalmente simples. Os seletores continuam inline nesta story para
permitir que a Story 8.2 extraia isso para um `SelectorBundle` versionado sem misturar escopos.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/infrastructure/parser/PciConcursosFixtureParser.java` | Criado | Parser mínimo da fixture HTML via `jsoup` |
| `src/main/java/com/campos/webscraper/infrastructure/parser/PciConcursosParsePreview.java` | Criado | Retorno observável do parse |
| `src/main/java/com/campos/webscraper/infrastructure/parser/PciConcursosPreviewItem.java` | Criado | Item normalizado do preview |
| `src/test/java/com/campos/webscraper/infrastructure/parser/PciConcursosFixtureParserTest.java` | Criado | Teste RED/GREEN do parse por fixture |
| `src/test/resources/fixtures/pci/pci-concursos-listing.html` | Criado | Fixture HTML representativa do PCI |
| `src/test/resources/fixtures/pci/pci-concursos-listing-expected.json` | Criado | Output esperado do parse |
| `docs/stories/README.md` | Modificado | Atualização do índice |
| `docs/stories/STORY-8.1-pci-concursos-html-fixture.md` | Criado | Registro final da story |

---

## Problemas encontrados

### Problema 1 — não havia parser HTML anterior no projeto

Até aqui o projeto só tinha integrações API-first. Não existia pacote de parser HTML nem fixture de
DOM para concursos.

### Problema 2 — a story precisava de um retorno verificável

Sem strategy completa ainda, seria fácil concluir a fixture sem um sinal claro de funcionamento.

### Problema 3 — o parser preview mascarava drift e podia quebrar com seletor ausente

Na primeira versão, escolaridade desconhecida era normalizada para `SUPERIOR` e o link de detalhe
assumia presença obrigatória. O review identificou que isso enfraquecia o valor do preview como
sinal observável de drift.

---

## Causa raiz

- a Iteration 8 é o primeiro ponto em que a arquitetura sai de JSON/API e entra em parsing HTML
- o plano do ADR009 separa corretamente fixture, bundle de seletores e strategy em stories distintas
- a primeira implementação ainda tinha fallback otimista demais para campos incompletos

---

## Solução aplicada

- criada fixture HTML estática local para a página de listagem
- criado JSON com o output esperado do parse
- implementado parser preview mínimo com `jsoup`
- definido `PciConcursosParsePreview` como retorno observável da story
- corrigido o parser após review para:
  - retornar `null` quando a escolaridade não puder ser inferida
  - não lançar `NullPointerException` quando o link de detalhe estiver ausente
  - resolver links relativos usando a `sourceUrl` real da página parseada
  - aceitar rótulos de escolaridade com acentuação normal em português
  - mapear requisitos de `pós-graduação` e `especialização` para `POS_GRADUACAO`
  - interpretar contagens de vagas com separador de milhar
- adicionados testes cobrindo os dois cenários de robustez acima
  e as variações de URL relativa, acentuação e contagem numérica

Seletores CSS congelados nesta story:

- card: `article.ca`
- nome do concurso: `.ca-link`
- órgão: `.ca-orgao`
- cargo: `.ca-cargo`
- vagas: `.ca-vagas`
- escolaridade: `.ca-escolaridade`
- salário: `.ca-salario`
- inscrições: `.ca-inscricoes`
- link de detalhe: `.ca-detalhes`

Esses seletores são provisórios e representam a fixture validada localmente. A formalização em
`SelectorBundle` fica para a Story 8.2.

---

## Lições aprendidas

- para scraper HTML, a primeira entrega útil não é a strategy completa; é a fixture confiável
- um preview estruturado evita ambiguidade sobre “está funcionando ou não”
- separar fixture de bundle reduz o risco de misturar regra de parse com wiring de produção cedo demais

---

## Estado final

Resultado validado:

```text
Tests run: 182, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Comando executado:

```bash
./mvnw test -DexcludedGroups=integration
```

Conclusão:

- fixture HTML do PCI Concursos criada
- output esperado do parse salvo
- retorno observável implementado
- base pronta para a Story 8.2 (`SelectorBundle`)
