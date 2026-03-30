# STORY-13.2.3 — Inconfidentes PDF Enrichment Generalization

## Objetivo

Aprofundar o parsing de PDF de `municipal_inconfidentes` de forma que a solução sirva como base
reutilizável para futuras prefeituras `PUBLIC_CONTEST`, sem exigir mudança imediata no schema de
persistência.

## Regra de produto

- continuar tratando a fonte como `PUBLIC_CONTEST`;
- usar o PDF principal como enriquecimento oportunístico, sem bloquear o scrape HTML;
- preservar detalhes mais ricos do edital no `payloadJson`;
- manter compatibilidade com o modelo persistido atual.

## Ciclo TDD

### Red

- testes do parser exigindo:
  - preservação de múltiplos cargos detectados no PDF;
  - extração de referências de anexos do edital;
  - manutenção do comportamento atual para cargo único.
- testes do enricher exigindo:
  - propagação de listas de cargos e anexos para o preview enriquecido;
  - preservação do título HTML quando o edital cobre múltiplos cargos.
- testes do normalizer exigindo:
  - serialização desses detalhes no `payloadJson`.

### Green

- `InconfidentesEditalPdfMetadata` passou a carregar:
  - `positionTitles`;
  - `annexReferences`.
- `InconfidentesContestPreviewItem` passou a carregar:
  - `pdfPositionTitles`;
  - `pdfAnnexReferences`.
- `InconfidentesEditalPdfMetadataParser` passou a extrair:
  - lista de cargos do edital;
  - referências de anexos.
- `InconfidentesContestPdfEnricher` passou a preservar esses detalhes no preview enriquecido.
- `InconfidentesEditaisFixtureParser` passou a inicializar esses campos para o caminho HTML-only.

### Refactor

- a generalização ficou no nível de metadata/preview, não no schema da entidade persistida;
- o `PublicContestPostingEntity` continua estável;
- o detalhe adicional segue no `payloadJson`, pronto para reaproveitamento em outras prefeituras
  e para futuras evoluções de anexos/cargos múltiplos.

## Arquivos principais

- `src/main/java/com/campos/webscraper/application/enrichment/InconfidentesEditalPdfMetadata.java`
- `src/main/java/com/campos/webscraper/application/enrichment/InconfidentesEditalPdfMetadataParser.java`
- `src/main/java/com/campos/webscraper/application/enrichment/InconfidentesContestPdfEnricher.java`
- `src/main/java/com/campos/webscraper/infrastructure/parser/InconfidentesContestPreviewItem.java`
- `src/main/java/com/campos/webscraper/infrastructure/parser/InconfidentesEditaisFixtureParser.java`

## Estado final

- `Inconfidentes` deixa de ser só a primeira fonte municipal operacional;
- passa também a ser a referência de enrichment PDF para o backlog municipal;
- o padrão novo permite abrir `Pouso Alegre`, `Munhoz` e demais prefeituras com menos heurística
  descartável e mais reaproveitamento do pipeline de edital.
