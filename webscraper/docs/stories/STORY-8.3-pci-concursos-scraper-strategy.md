# STORY 8.3 — Implementar PciConcursosScraperStrategy

**Status:** ✅ Concluída
**Iteration:** 8 — Primeiro scraper HTML estático: PCI Concursos
**Data:** 2026-03-12
**Referência ADR:** ADR009 Story 8.3

---

## Objetivo

Implementar a primeira strategy concreta de scraping HTML estático do projeto:

- `PciConcursosScraperStrategy`
- paginação até o fim da listagem
- normalização para `PublicContestPostingEntity`

Essa story fecha o primeiro fluxo HTML real usando:

- `JobFetcher`
- `SelectorBundle`
- `jsoup`
- normalização de concursos públicos

---

## Ciclo TDD

### RED — testes de extração com fixture HTML primeiro

Foi criado `PciConcursosScraperStrategyTest` cobrindo:

- suporte por metadata explícita do `TargetSiteEntity`
- extração paginada usando duas fixtures HTML

O RED inicial falhou por compilação porque ainda não existiam:

- `PciConcursosScraperStrategy`
- `PciConcursosContestNormalizer`

### GREEN — implementação mínima

Foi implementado:

1. `PciConcursosContestNormalizer`
2. `PciConcursosScraperStrategy`
3. suporte a paginação no parser com `extractNextPageUrl(...)`
4. seletor `nextPage` no bundle `pci_concursos_v1`
5. correções pós-review para faixa salarial e órgãos federais
6. correções pós-review adicionais para:
   - preservar escolaridade desconhecida como `null`
   - normalizar acentos na heurística de `governmentLevel`
   - impedir loop de paginação em `nextPage` auto-referente
7. correção final do parser de salário para milhar sem centavos
8. correção final de paginação para `.next` sem `href`
9. correção final do parser de salário para valores de 4+ dígitos sem separador
10. correção final para ignorar números não monetários e `href="#"` na paginação
11. correção final para preservar `contestUrl` e derivar `publishedAt` da fonte
12. correção final para:
   - fechar concurso com base na data real do crawl
   - degradar com segurança quando o card não traz datas parseáveis
13. correção final para:
   - usar a URL do concurso como identidade estável
   - evitar `publishedAt` futuro inventado a partir de datas de inscrição
14. correção final para:
   - garantir `educationLevel` persistível mesmo quando o parser não reconhece a escolaridade

O fluxo executado pela strategy ficou:

1. `JobFetcher.fetch(...)`
2. `PciConcursosFixtureParser.parse(...)`
3. `PciConcursosContestNormalizer.normalize(...)`
4. seguir `nextPage` até não haver mais link
5. retornar `ScrapeResult.success(...)`

### REFACTOR

O parser continuou responsável pela extração HTML e a strategy ficou responsável por orquestração e
paginação. Isso mantém o mesmo desenho da arquitetura API-first já usada em Indeed e DOU.

---

## Arquivos criados / modificados

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `src/main/java/com/campos/webscraper/application/strategy/PciConcursosScraperStrategy.java` | Criado | Strategy HTML estática com paginação |
| `src/main/java/com/campos/webscraper/application/normalizer/PciConcursosContestNormalizer.java` | Criado | Normalização do preview PCI para `PublicContestPostingEntity` |
| `src/main/java/com/campos/webscraper/infrastructure/parser/PciConcursosFixtureParser.java` | Modificado | Extração de `nextPageUrl` |
| `src/main/java/com/campos/webscraper/infrastructure/parser/PciConcursosSelectorBundles.java` | Modificado | Inclusão do seletor `nextPage` |
| `src/test/java/com/campos/webscraper/application/strategy/PciConcursosScraperStrategyTest.java` | Criado | Testes RED/GREEN da strategy |
| `src/test/resources/fixtures/pci/pci-concursos-page-1.html` | Criado | Primeira página da listagem com link de próxima |
| `src/test/resources/fixtures/pci/pci-concursos-page-2.html` | Criado | Segunda página da listagem sem próxima |
| `docs/stories/README.md` | Modificado | Atualização do índice |
| `docs/stories/STORY-8.3-pci-concursos-scraper-strategy.md` | Criado | Registro final da story |

---

## Problemas encontrados

### Problema 1 — a paginação ainda não tinha contrato explícito

As stories 8.1 e 8.2 tratavam só a extração do card e o bundle base. Ainda faltava o caminho para
seguir a listagem até o fim.

### Problema 2 — não existia normalizer específico do PCI

Para devolver `PublicContestPostingEntity`, era necessário transformar o preview extraído em uma
forma de domínio consistente com o restante do projeto.

### Problema 3 — faixa salarial e órgãos federais precisaram de refinamento

O review identificou duas lacunas práticas:

- faixas como `R$ 6.200,50 a R$ 8.000,00` quebravam o parse do salário
- organizadores federais como universidade federal, instituto federal e ministério estavam sendo
  classificados incorretamente

### Problema 4 — o fluxo ainda precisava ser mais conservador com dados desconhecidos

O review seguinte identificou:

- escolaridade desconhecida estava sendo promovida indevidamente para `SUPERIOR`
- textos com acento podiam falhar na heurística de governo
- paginação podia entrar em loop quando o próximo link apontava para a própria página

### Problema 5 — salário com milhar sem centavos ainda podia ser corrompido

O review final identificou que formatos comuns como `R$ 8.500` ou `R$ 1.000 a R$ 2.000` ainda
podiam ser interpretados como `8` ou `1`.

### Problema 6 — `.next` sem href ainda podia abortar o scrape

Em marcações de última página onde o elemento `.next` existe sem link utilizável, a strategy ainda
recebia URL vazia e podia abortar em vez de encerrar a paginação.

### Problema 7 — salários como `1500,00` e `1000` ainda podiam ficar 10x menores

O último review identificou que a regex monetária ainda capturava só os três primeiros dígitos em
valores sem separador de milhar.

### Problema 8 — números não monetários e `href="#"` ainda geravam ruído

O review seguinte identificou dois casos finais:

- textos como `40h semanais, R$ 2.500,00` ainda podiam capturar `40` como salário
- paginação com `.next href="#"` ainda causava fetch redundante

### Problema 9 — URL canônica e data de publicação ainda estavam imprecisas

O último review identificou que:

- `canonicalUrl` estava reaproveitando a URL do edital e perdendo a página principal do concurso
- `publishedAt` ainda não refletia uma data derivada da fonte

### Problema 10 — status e datas ausentes ainda podiam distorcer o resultado do scrape

O review seguinte identificou que:

- concursos expirados ainda podiam sair como `OPEN` porque o fechamento estava sendo comparado com
  `publishedAt`, e não com a data do crawl
- cards sem datas parseáveis ainda podiam abortar o scrape inteiro em vez de degradar para um
  registro válido

### Problema 11 — identidade e `publishedAt` ainda podiam degradar a qualidade persistida

O review seguinte identificou que:

- `externalId` ainda usava a URL do edital, o que podia gerar duplicatas quando o edital mudasse
  mas o concurso continuasse o mesmo
- `publishedAt` ainda podia ser derivado de datas de inscrição futuras, produzindo concursos com
  data de publicação à frente da própria coleta

### Problema 12 — escolaridade desconhecida ainda quebrava a persistência

O review final identificou que:

- o parser continua corretamente expondo `educationLevel = null` quando a fonte não informa ou muda
  o texto de escolaridade
- mas a entidade persistida exige `education_level not null`, então o normalizer ainda podia gerar
  inserts inválidos para concursos reais do PCI

---

## Causa raiz

- a 8.1 e a 8.2 prepararam fixture e bundle, mas ainda sem orchestration
- o PCI é a primeira fonte HTML estática do projeto, então era esperado surgir o primeiro fluxo
  de paginação e normalização fora do mundo API-first
- a primeira heurística de normalização ainda estava simplificada demais para salary range e
  órgãos federais
- o loop de paginação ainda assumia que o HTML de navegação sempre avançaria corretamente
- a regex monetária ainda não distinguia bem milhar sem centavos
- o parser de navegação ainda não tratava `href` ausente como fim normal da listagem
- a regex monetária ainda precisava priorizar valores inteiros longos sem separador
- o parser de salário ainda precisava dar preferência a tokens precedidos por `R$`
- o preview ainda precisava carregar a URL principal do concurso separada do edital
- a regra de status ainda confundia data da fonte com data da coleta
- a normalização ainda estava rígida demais para cards com datas ausentes
- a identidade do concurso ainda estava acoplada a uma URL mais volátil do que a página principal
- a ausência de data real de publicação ainda estava sendo preenchida com uma heurística agressiva
- o contrato do parser e o contrato JPA da entidade final têm exigências diferentes para
  `educationLevel`

---

## Solução aplicada

- criada `PciConcursosScraperStrategy`
- criado `PciConcursosContestNormalizer`
- adicionado `nextPage` em `pci_concursos_v1`
- implementada paginação por loop até ausência de próximo link
- normalização mínima entregue com:
  - `contestName`
  - `organizer`
  - `positionTitle`
  - `governmentLevel`
  - `educationLevel`
  - `numberOfVacancies`
  - `baseSalary`
  - `salaryDescription`
  - `editalUrl`
  - `publishedAt`
  - `registrationStartDate`
  - `registrationEndDate`
  - `contestStatus`
  - `payloadJson`
- ajustes pós-review:
  - `baseSalary` agora deriva o primeiro valor monetário quando a fonte traz faixa salarial
  - `governmentLevel` agora reconhece marcadores federais explícitos antes do fallback
  - `educationLevel` desconhecido é preservado como `null`
  - heurística de governo normaliza acentos antes das comparações
  - a strategy interrompe paginação em URL repetida ou auto-referente
  - `baseSalary` agora interpreta corretamente valores com milhar mesmo sem centavos
  - `extractNextPageUrl()` agora devolve `null` quando `.next` não tem `href` utilizável
  - `baseSalary` agora também interpreta corretamente `1500,00` e `1000`
  - `baseSalary` agora ignora números não monetários anteriores ao valor em reais
  - `extractNextPageUrl()` agora trata `href="#"` como fim da paginação
  - `contestUrl` passou a ser preservada separadamente de `editalUrl`
  - `publishedAt` agora deriva da primeira data estável disponível da própria fonte
  - `contestStatus` agora usa a data real do crawl para decidir entre `OPEN` e `REGISTRATION_CLOSED`
  - quando as datas não são parseáveis, a normalização continua com fallback seguro para a data do
    crawl, sem abortar a página inteira
  - `externalId` agora usa a URL canônica do concurso, preservando identidade estável entre crawls
  - `publishedAt` agora usa a data do crawl como fallback conservador, evitando datas futuras
    artificiais
  - o parser continua observável com `educationLevel = null`, mas o normalizer aplica fallback para
    `SUPERIOR` antes de materializar `PublicContestPostingEntity`, preservando compatibilidade com o
    schema atual

---

## Lições aprendidas

- o pipeline HTML do projeto já consegue sair de fixture paginada e chegar em entidade de domínio
- `SelectorBundle` precisou crescer para contemplar navegação, não só campos do card
- manter parser e strategy separados evita acoplamento entre DOM e controle de fluxo

---

## Estado final

Resultado validado:

```text
Tests run: 200, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Comando executado:

```bash
./mvnw test -DexcludedGroups=integration
```

Conclusão:

- `PciConcursosScraperStrategy` implementada
- paginação implementada
- normalização para `PublicContestPostingEntity` implementada
- story pronta para review
