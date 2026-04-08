# Story 13.3.13 — Itajubá híbrido: mapeamento das trilhas e ordem de execução

## Status

Concluida

## Objetivo

Mapear com evidência concreta as trilhas privada e pública de `Itajubá`, registrar os blockers
reais da cidade e decidir qual implementação sai primeiro.

## Contexto

- `Campinas` e `Santa Rita do Sapucaí` já foram fechadas como as duas primeiras cidades híbridas;
- `Itajubá` passa a ser a próxima cidade do backlog híbrido;
- a equipe decidiu adiar o script e2e oficial até fechar todas as cidades abertas, então a cidade
  precisa de uma priorização objetiva de fontes antes da próxima implementação.

## Evidência mapeada

### Trilha privada candidata

- empresa: `Helicopteros do Brasil S/A - Helibras` / `Airbus`
- origem: `Workday`
- entrada oficial observada:
  - `https://ag.wd3.myworkdayjobs.com/en-US/Airbus`
- evidência de aderência local:
  - vagas oficiais da Airbus com `location = Itajubá`
  - exemplo observado na busca pública: vaga da `Helibras` em `Itajubá / MG`

### Trilha pública candidata

- órgão: `Câmara Municipal de Itajubá`
- origem: portal institucional WordPress com páginas e anexos PDF oficiais
- páginas/fontes oficiais observadas:
  - `https://itajuba.cam.mg.gov.br/site/concurso-publico-cmi/`
  - `https://itajuba.cam.mg.gov.br/site/camara-municipal-de-itajuba-lanca-concurso-publico-para-preenchimento-de-cargos-efetivos/`
  - `https://itajuba.cam.mg.gov.br/site/homologacao-do-concurso-publico-da-camara-municipal-de-itajuba/`
  - `https://itajuba.cam.mg.gov.br/site/wp-content/uploads/2023/12/EDITAL_CMI_2023.pdf`
  - `https://itajuba.cam.mg.gov.br/site/wp-content/uploads/2024/12/HOMOLOGACAO-CONCURSO.pdf`

## Problemas encontrados

- a hipótese inicial de trilha pública pela `Prefeitura de Itajubá` não ficou boa para a próxima
  implementação:
  - o domínio institucional respondeu com desafio `Cloudflare` no acesso direto a `robots.txt`;
  - isso indica risco operacional maior para uma primeira implementação da cidade;
  - como já existe uma fonte pública oficial mais estável na Câmara, a prefeitura não é a melhor
    escolha agora.
- a trilha privada de `Airbus/Helibras` exige abrir uma família nova (`Workday`) ou algum parser
  específico ainda não consolidado no projeto.

## Decisão

- trilha pública oficial escolhida para a primeira implementação de `Itajubá`:
  - `camara_itajuba`
- trilha privada mapeada para uma fatia posterior da mesma cidade:
  - `airbus_helibras_workday`
- ordem da cidade:
  1. implementar primeiro a trilha pública oficial da Câmara Municipal de Itajubá
  2. depois atacar a trilha privada da `Helibras/Airbus` via `Workday`

## Solução aplicada

- o backlog da cidade foi refinado para refletir o mapeamento real das fontes;
- a prefeitura foi explicitamente rebaixada a hipótese descartada nesta etapa por risco
  operacional;
- a Câmara passou a ser a referência pública prioritária;
- `Airbus/Helibras` passou a ser a referência privada da cidade, mas não a primeira execução.

## Validação

- validação documental e de descoberta externa, sem mudança de código;
- evidências públicas verificadas:
  - página institucional da Câmara sobre o concurso;
  - PDF oficial do edital;
  - PDF oficial de homologação;
  - board oficial `Workday` da Airbus com vaga localizada em `Itajubá`.

## Lições aprendidas

- na expansão híbrida, a trilha pública oficial da cidade não precisa ser necessariamente a
  prefeitura se outro órgão oficial municipal oferecer fonte institucional mais estável;
- vale evitar introduzir uma nova família ATS antes de esgotar uma trilha pública oficial mais
  previsível.

## Estado final

- `Itajubá` fica com as duas trilhas mapeadas;
- a primeira implementação da cidade já está escolhida:
  - pública da Câmara primeiro;
  - privada `Airbus/Helibras` depois;
- a próxima fatia executável da cidade passa a ser a fonte pública oficial da Câmara Municipal de
  Itajubá.
