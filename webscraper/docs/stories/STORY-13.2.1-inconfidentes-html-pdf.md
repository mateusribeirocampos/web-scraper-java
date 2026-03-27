# STORY-13.2.1 — Inconfidentes HTML + PDF

## Objetivo

Fechar a primeira fonte municipal operacional da família `PUBLIC_CONTEST` usando a página oficial
de editais da Prefeitura de Inconfidentes, com captura de anexos e modelagem voltada a cargo,
formação e escolaridade.

## Regra de produto

- tratar a fonte como `PUBLIC_CONTEST`;
- usar `HTML + PDF` oficial, não senioridade;
- preservar anexos/evidências no `payloadJson`;
- filtrar avisos não operacionais para não misturar transporte, chamadas culturais e concursos.

## Ciclo TDD

### Red

- testes do parser exigindo:
  - extração do bloco de contratação de professor;
  - captura dos links de edital/anexos;
  - exclusão de blocos não operacionais como transporte universitário e chamamento cultural.
- teste do normalizer exigindo mapeamento para `PublicContestPostingEntity`.
- teste da strategy exigindo suporte e scrape da fonte municipal.
- teste do runner exigindo despacho para `municipal_inconfidentes`.
- teste do catálogo exigindo exposição do perfil curado operacional.

### Green

- parser `InconfidentesEditaisFixtureParser` implementado.
- normalizer `InconfidentesContestNormalizer` implementado.
- strategy `InconfidentesContestScraperStrategy` implementada.
- import use case `InconfidentesContestImportUseCase` implementado.
- `ImportingCrawlJobExecutionRunner` passou a suportar `municipal_inconfidentes`.
- `CoreSourceOnboardingProfiles` e `TargetSiteOnboardingProfileCatalog` passaram a expor o perfil
  operacional de Inconfidentes.
- o checklist curado do perfil ficou propositalmente conservador em `robots/terms/API`, evitando
  vender ativação pronta antes do fechamento formal das evidências legais.

### Refactor

- a fatia ficou limitada a um domínio municipal real para evitar generalização prematura;
- `Pouso Alegre` e `Munhoz` continuam fora do runtime até validação técnica própria;
- anexos seguem preservados no `payloadJson` para futuras features de leitura de edital e filtro
  mais fino por formação/cargo.

## Arquivos principais

- `src/main/java/com/campos/webscraper/infrastructure/parser/InconfidentesEditaisFixtureParser.java`
- `src/main/java/com/campos/webscraper/application/normalizer/InconfidentesContestNormalizer.java`
- `src/main/java/com/campos/webscraper/application/strategy/InconfidentesContestScraperStrategy.java`
- `src/main/java/com/campos/webscraper/application/usecase/InconfidentesContestImportUseCase.java`
- `src/main/java/com/campos/webscraper/application/onboarding/CoreSourceOnboardingProfiles.java`
- `src/main/java/com/campos/webscraper/application/onboarding/TargetSiteOnboardingProfileCatalog.java`

## Estado final

- `municipal_inconfidentes` entra no runtime operacional;
- a fonte executa pelo mesmo pipeline de concursos já usado por `dou-api` e `pci_concursos`;
- o inventário municipal dos ADRs passa a distinguir:
  - `Inconfidentes`: implementado por `HTML + PDF`;
  - `Pouso Alegre` e `Munhoz`: ainda em validação técnica.
