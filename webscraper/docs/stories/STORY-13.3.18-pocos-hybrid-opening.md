# Story 13.3.18 — Poços de Caldas híbrido: abertura e mapeamento

## Objetivo

Abrir `Poços de Caldas` como a próxima cidade do backlog híbrido e registrar as duas trilhas com
menor atrito técnico e melhor base oficial.

## Ciclo TDD

1. confirmar a cidade no backlog híbrido depois de `Itajubá`;
2. revisar a fonte pública oficial e a candidata privada;
3. escolher a ordem de implementação antes de abrir código novo.

## Arquivos criados / modificados

- `README.md`
- `webscraper/README.md`
- `ADRs-web-scraping-vagas/ADR009-XP-Delivery-Plan-and-Detailed-Tasks-for-WebScraper.md`
- `webscraper/docs/stories/README.md`

## Problemas encontrados

- a página HTML institucional da prefeitura respondeu `403` no acesso bruto;
- o ponto estável da trilha pública ficou no PDF oficial do edital, não na notícia HTML.

## Causa raiz

- o portal mistura publicação institucional com camada anti-bot na notícia;
- o anexo PDF permanece público e mais estável para ingestão.

## Solução aplicada

- trilha pública definida como `municipal_pocos_caldas` via edital PDF oficial;
- trilha privada definida como `alcoa_pocos_caldas_workday`;
- ordem oficial:
  - primeiro pública
  - depois privada

## Validação

- evidência pública revisada:
  - `https://pocosdecaldas.mg.gov.br/robots.txt`
  - `https://pocosdecaldas.mg.gov.br/wp-content/uploads/2025/09/EDITAL-DE-PROCESSO-SELETIVO-001-2025.pdf`
- evidência privada revisada:
  - `https://alcoa.wd5.myworkdayjobs.com/robots.txt`
  - `https://alcoa.wd5.myworkdayjobs.com/wday/cxs/alcoa/Careers/jobs`

## Lições aprendidas

- para portais municipais com camada anti-bot na notícia, o anexo oficial pode ser a fonte mais
  estável que a própria página HTML;
- `Workday` continua sendo a trilha privada mais previsível quando há facet de localização clara.

## Estado final

- `Poços de Caldas` aberta;
- trilhas pública e privada mapeadas;
- ordem de implementação definida.
