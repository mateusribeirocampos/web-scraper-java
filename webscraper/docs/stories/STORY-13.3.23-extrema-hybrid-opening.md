# Story 13.3.23 — Extrema híbrido: abertura e mapeamento

## Objetivo

Abrir `Extrema` como a última cidade do backlog híbrido, definindo uma trilha pública oficial e uma
trilha privada executável.

## Ciclo TDD

1. validar as fontes vivas da cidade antes de abrir código;
2. decidir a ordem de implementação por menor risco operacional;
3. registrar a cidade e as duas trilhas no plano/documentação.

## Arquivos criados / modificados

- `README.md`
- `webscraper/README.md`
- `ADRs-web-scraping-vagas/ADR009-XP-Delivery-Plan-and-Detailed-Tasks-for-WebScraper.md`
- `webscraper/docs/stories/README.md`
- `webscraper/docs/stories/STORY-13.3.10-hybrid-cities-closure-plan.md`

## Problemas encontrados

- o candidato privado inicial não se sustentou com evidência viva;
- a cidade exigia uma trilha privada que realmente devolvesse vagas em `Extrema`.

## Causa raiz

- a expansão municipal/híbrida anterior já tinha consumido os candidatos mais óbvios;
- para a última cidade, o mapeamento precisava ser mais conservador.

## Solução aplicada

- trilha pública escolhida: portal da Secretaria de Educação da Prefeitura de `Extrema`;
- trilha privada escolhida: board `Gupy` da `Special Dog Company` com vagas em `Extrema`;
- ordem de execução registrada:
  - primeiro a trilha pública oficial;
  - depois a trilha privada.

## Validação

- evidências revisadas:
  - `https://www.extrema.mg.gov.br/robots.txt`
  - `https://www.extrema.mg.gov.br/secretarias/educacao`
  - `https://specialdogcompany.gupy.io/robots.txt`
  - `https://www.specialdog.com.br/privacidade`

## Lições aprendidas

- no fechamento do backlog híbrido, a qualidade do mapeamento vale mais do que insistir em um
  candidato privado frágil;
- `Gupy` continua sendo a rota mais rápida para fechar a trilha privada quando existe board oficial
  e evidência viva por cidade.

## Estado final

- `Extrema` aberta como a última cidade do backlog híbrido;
- ordem de implementação da cidade definida.
