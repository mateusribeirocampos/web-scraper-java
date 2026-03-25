# STORY-13.1 — Expansão municipal `PUBLIC_CONTEST` (primeira fatia)

## Objetivo

Abrir a expansão para prefeituras próximas com o menor risco possível, começando por perfis curados
de onboarding para municípios com melhor sinal de API/dados abertos.

## Regra de produto

- tratar a família municipal como `PUBLIC_CONTEST`;
- filtrar por cargo, escolaridade, formação exigida e evidência em edital;
- não reutilizar `junior/pleno/senior` como eixo principal dessa família.

## Ciclo TDD

### Red

- Ajustados testes do catálogo para exigir os novos perfis:
  - `municipal_inconfidentes`
  - `municipal_pouso_alegre`
  - `municipal_munhoz`
- Ajustado teste do controller para validar a exposição de um perfil municipal detalhado.

### Green

- O inventário municipal foi consolidado nos ADRs para:
  - `Inconfidentes`
  - `Pouso Alegre`
  - `Munhoz`
- A direção de produto/runtime ficou explícita:
  - família `PUBLIC_CONTEST`
  - filtro por cargo, escolaridade e formação
  - nada de expor source municipal no catálogo operacional antes do importador real

### Refactor

- A fatia não promete scraper municipal pronto.
- Ela evita expor perfis “ativáveis” sem executor correspondente.
- O inventário detalhado fica nos ADRs até a próxima fatia do runtime municipal.

## Arquivos criados / modificados

- `../../ADRs-web-scraping-vagas/ADR002-Target-Site-Taxonomy-and-Requirements-for-WebScraper.md`
- `../../ADRs-web-scraping-vagas/ADR009-XP-Delivery-Plan-and-Detailed-Tasks-for-WebScraper.md`

## Estado final

- documentação sincronizada
- runtime protegido contra exposição prematura de fonte municipal

## Próximo passo

- validar tecnicamente `Inconfidentes`, `Pouso Alegre` e `Munhoz` como candidatos reais de API;
- se a API não cobrir concursos diretamente, descer para `HTML + PDF` em nova fatia com onboarding
  formal por domínio.
