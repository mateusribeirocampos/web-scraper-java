# Story 13.3.5 — Santa Rita do Sapucaí Hybrid Opening

## Status

Opened and documented

## Objective

Abrir `Santa Rita do Sapucaí` como a próxima cidade do backlog híbrido, identificando uma trilha
privada real de tecnologia e uma trilha pública oficial de concursos/processos seletivos antes de
qualquer implementação.

## TDD Cycle

- Esta story é de abertura e priorização, não de implementação.
- O ciclo aplicado aqui foi:
  1. revisar o backlog híbrido e a ordem pós-`Campinas`;
  2. mapear fontes com sinal técnico real;
  3. registrar a entrada da cidade com referências oficiais/públicas reproduzíveis;
  4. deixar a escolha da primeira implementação como decisão explícita da próxima fatia.

## Files Created / Modified

- `ADRs-web-scraping-vagas/ADR009-XP-Delivery-Plan-and-Detailed-Tasks-for-WebScraper.md`
- `README.md`
- `webscraper/README.md`
- `webscraper/docs/stories/README.md`
- `webscraper/docs/stories/STORY-13.3.5-santa-rita-hybrid-opening.md`

## Sources Mapped

Trilha privada candidata:

- `WatchGuard Technologies` via `Lever`
- board público: `https://jobs.lever.co/watchguard`
- evidência atual:
  - vagas associadas a `Santa Rita Do Sapucai, Brazil`
  - presença de vagas de tecnologia, incluindo `Senior Software Engineer` e `Senior Data Base Engineer`

Trilha pública candidata:

- Câmara Municipal de Santa Rita do Sapucaí
- página oficial: `https://www.santaritadosapucai.mg.leg.br/transparencia/processos-seletivos-2025`
- evidência atual:
  - página oficial de `Processos Seletivos 2025`
  - edital publicado no domínio oficial da Câmara

## Decision

`Santa Rita do Sapucaí` entra formalmente como a próxima cidade híbrida do backlog.

As duas trilhas iniciais ficam definidas assim:

1. privada:
   - `WatchGuard Technologies` via `Lever`
2. pública:
   - Câmara Municipal de Santa Rita do Sapucaí, pela área oficial de processos seletivos

## Why These Sources

- `WatchGuard` oferece um board ATS público, estruturado e com sinal direto da cidade no payload da vaga.
- A Câmara Municipal oferece uma fonte oficial pública com página estruturada e editais publicados
  no próprio domínio governamental.
- Essa combinação mantém o objetivo do backlog híbrido:
  - mercado privado real do polo tech;
  - fonte pública oficial do mesmo município.

## Validation

Validação de descoberta feita com fontes públicas:

- board público `Lever` da `WatchGuard` com resultados para `Santa Rita Do Sapucai, Brazil`
- página oficial `Processos Seletivos 2025` da Câmara Municipal acessível publicamente

## Next Decision

A ordem da próxima implementação foi definida assim:

1. abrir primeiro a trilha privada `Lever` da `WatchGuard`;
2. deixar a trilha pública oficial da Câmara para a story seguinte.

## Final State

- `Santa Rita do Sapucaí` foi aberta documentalmente no backlog híbrido
- as duas rotas iniciais já têm referência concreta
- a story seguinte saiu primeiro pela trilha privada via `WatchGuard` no `Lever`
- a trilha pública oficial da Câmara passou a seguir logo depois, modelada como HTML estático com
  anexos PDF oficiais
