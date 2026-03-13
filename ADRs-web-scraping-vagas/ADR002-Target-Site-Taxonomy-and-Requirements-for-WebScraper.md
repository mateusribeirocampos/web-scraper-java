# ADR 002 — Taxonomia de Sites-Alvo, Análise Legal e Requisitos para o WebScraper de Vagas

## Title

Classificação dos sites brasileiros de vagas e concursos, análise de permissões (robots.txt / ToS / API), e definição de requisitos funcionais e não-funcionais.

## Status

Accepted

## Date

2026-03-09

## Context

Uma plataforma de scraping falha rapidamente quando trata todos os sites como se tivessem a mesma
estrutura, comportamento, e — mais importante — o mesmo status legal de acesso. Sites de vagas e
concursos públicos no Brasil têm políticas muito distintas entre si. O levantamento feito a seguir
é obrigatório antes de qualquer implementação.

**Princípio-guia:** API oficial sempre tem prioridade absoluta sobre scraping.
Se uma fonte oferece API documentada e legalmente utilizável, scraping é vedado para aquela fonte.

---

## Decision

### 1. Taxonomia Técnica de Sites

#### Tipo A — HTML estático com paginação

Características:
- HTML renderizado no servidor.
- Estrutura DOM estável.
- Paginação via query params ou links.
- Dados visíveis na resposta inicial.

Abordagem primária: HTTP client + jsoup.

#### Tipo B — Páginas semi-dinâmicas

Características:
- HTML entregue pelo servidor, mas alguns campos carregados de forma lazy.
- Parser estático ainda funciona na maioria dos casos com requisições adicionais.

Abordagem primária: HTTP client + jsoup, com chamadas de API descobertas quando disponíveis.

#### Tipo C — Páginas totalmente dinâmicas (JavaScript-rendered)

Características:
- Conteúdo relevante aparece apenas após execução de JS.
- DOM depende de chamadas XHR/fetch, hidratação ou renderização em tempo de execução.

Abordagem primária: Playwright for Java como fallback explícito, nunca como padrão.

#### Tipo D — Páginas autenticadas / com sessão

Características:
- Login obrigatório.
- Cookies de sessão ou tokens.
- Possíveis proteções MFA, anti-bot ou CSRF.

Abordagem: Apenas quando legalmente aprovado e com credencial em vault isolado.

#### Tipo E — API-backed (fonte real é JSON, não HTML)

Características:
- Dados expostos via endpoints JSON chamados pelo frontend.
- HTML é secundário; resposta da API é a fonte primária.

Abordagem: Preferir endpoints documentados ou descobertos quando permitidos. Inclui também
integrações via MCP connectors oficiais.

---

### 2. Catálogo de Sites Brasileiros — Análise Legal e Estratégia

#### 2.1 Vagas de Emprego — Setor Privado

| Site | Tipo | robots.txt | ToS | API Oficial | Decisão | Estratégia |
|---|---|---|---|---|---|---|
| **Indeed Brasil** (br.indeed.com) | E | ⚠️ Parcialmente restritivo para crawlers genéricos | ❌ Proíbe scraping direto | ✅ **Indeed MCP Connector** disponível | ✅ **Usar MCP Connector** | Tipo E via integração oficial. Nenhum scraping. |
| **LinkedIn** (linkedin.com) | D | ❌ `Disallow: /` para a maioria dos user-agents | ❌ Proíbe scraping explicitamente. Histórico judicial (hiQ Labs v. LinkedIn). | ⚠️ Partner API (acesso restrito / parceria) | ❌ **Não scrape.** | Solicitar acesso à LinkedIn Jobs API ou aceitar ausência desta fonte. |
| **Catho** (catho.com.br) | B | ⚠️ Parcialmente restritivo | ❌ ToS proíbe scraping automático | ❌ Não disponível publicamente | ❌ **Não recomendado** | Excluir da fase inicial sem aprovação explícita da Catho. |
| **InfoJobs** (br.infojobs.net) | B | ⚠️ | ❌ ToS proíbe | ⚠️ API com acesso restrito | ⚠️ **Avaliar** | Solicitar acesso à API antes de qualquer implementação. |
| **Vagas.com** (vagas.com.br) | A | ⚠️ A verificar | ⚠️ A verificar | ❌ | ⚠️ **Verificar antes de usar** | Revisar robots.txt e ToS antes de implementar. Candidato a Tipo A se permitido. |
| **Glassdoor** (glassdoor.com.br) | C | ❌ Restritivo | ❌ Proíbe scraping | ⚠️ Partner API | ❌ **Não recomendado** | Excluir da fase inicial. |
| **Gupy** (portal.gupy.io) | E | ✅ | ⚠️ API com Bearer Token (empresas-clientes) | ✅ API REST (`api.gupy.io/api/v1/jobs`) | ⚠️ **API requer auth corporativa** | O acesso público ao portal pode ser explorado via Tipo A para listagens públicas. A API oficial exige que a empresa seja cliente Gupy. Avaliar scraping do portal público separadamente. |
| **Programaticamente** (programaticamente.com.br) | A | ✅ A verificar | ⚠️ Verificar ToS | ❌ | ⚠️ **Candidato a Type A** | Site focado em vagas tech no Brasil. Verificar robots.txt e ToS. Alta relevância para Java Junior. |
| **Codante.io Jobs API** (docs.apis.codante.io/jobs-api) | E | ✅ | ✅ API pública para estudo/prototipação | ✅ REST sem autenticação | ✅ **Usar em dev/test** | Ideal para desenvolvimento local, testes de integração e prototipação do pipeline. Não é produção. |

#### 2.1.1 Expansão segura para empresas pequenas e médias

Para ampliar a cobertura além de concursos e grandes boards generalistas, o projeto passa a
priorizar uma família adicional de fontes privadas com risco legal e técnico menor:

- boards públicos de ATS com endpoint oficial/documentado de vagas publicadas
- páginas de carreira mantidas pela própria empresa com `JobPosting` estruturado e acesso
  permitido por `robots.txt`
- APIs públicas ou feeds expostos para vagas publicadas, sem autenticação de candidato

Essas fontes são particularmente relevantes para empresas pequenas e médias, startups e software
houses que publicam vagas via plataformas padronizadas em vez de manter um portal próprio robusto.

| Fonte / Família | Tipo | Sinal de permissão | API/endpoint público | Decisão | Estratégia |
|---|---|---|---|---|---|
| **Greenhouse Job Board** | E | ✅ Job Board API pública para vagas publicadas | ✅ `boards-api.greenhouse.io/v1/boards/{board_token}/jobs` | ✅ **Priorizar após Indeed** | Integrar como provider API-first para boards de empresas com foco em Java/TI. |
| **Lever Postings** | E | ✅ Postings API pública para vagas publicadas | ✅ `api.lever.co/v0/postings/{site}` | ✅ **Priorizar após Greenhouse** | Integrar como provider API-first para empresas menores com job board Lever. |
| **Ashby Job Board API** | E | ✅ Posting API pública para vagas publicadas | ✅ `api.ashbyhq.com/posting-api/job-board/{job_board_name}` | ✅ **Priorizar após Lever** | Integrar como provider API-first para startups e PMEs que usam Ashby. |
| **Páginas de carreira próprias da empresa** | A/E | ⚠️ Requer revisão por domínio | ⚠️ Varia por empresa | ✅ **Permitido sob onboarding formal** | Usar apenas quando `robots.txt` permitir e houver sinais fortes como `schema.org/JobPosting` e sitemap acessível. |
| **Boards/agregadores sem API pública e sem permissão explícita** | A/B/C | ⚠️ Alto risco | ❌ | ❌ **Não priorizar** | Exigir revisão legal explícita antes de qualquer story de scraping. |

**Resumo de decisão para Setor Privado:**

- Indeed: integrar via MCP Connector (API oficial) — **prioridade máxima**.
- Greenhouse, Lever e Ashby: próximos candidatos prioritários para ampliar vagas de PMEs com
  abordagem API-first.
- Primeiro board Greenhouse selecionado em 2026-03-13: **Bitso** (`board_token = bitso`) —
  endpoint público validado e board com aderência atual ao foco Java/backend.
- Estado atual do provider Greenhouse em 2026-03-13: onboarding (`9.1`), client (`9.2`),
  normalizer (`9.3`) e strategy (`9.4`) implementados para `greenhouse_bitso`; a persistência
  ponta a ponta permanece planejada para a `9.5`.
- Páginas de carreira próprias: permitidas apenas com `robots.txt` revisado e sinais claros de
  publicação estruturada.
- LinkedIn: aguardar acesso a Partner API ou excluir da fase 1.
- Catho, Glassdoor: excluídos da fase 1 sem permissão explícita.
- Vagas.com, Programaticamente: revisar antes de implementar.
- Gupy portal público: avaliar como Tipo A após revisão legal.
- Codante Jobs API: usar apenas em dev/test como fixture dinâmica.

---

#### 2.2 Concursos Públicos

| Site | Tipo | robots.txt | ToS / Status Legal | API Oficial | Decisão | Estratégia |
|---|---|---|---|---|---|---|
| **Diário Oficial da União** (in.gov.br) | E | ✅ Dados governamentais públicos | ✅ Lei de Acesso à Informação (LAI). Dados públicos por lei. | ✅ **API de busca do DOU** (in.gov.br/dados-abertos) | ✅ **Usar API oficial** | Tipo E via API gov. Buscar editais com palavras-chave como "Analista de TI" ou "Desenvolvedor". |
| **Portal da Transparência** (transparencia.gov.br) | E | ✅ | ✅ Dados públicos / API aberta | ✅ API pública (transparencia.gov.br/api-de-dados) | ✅ **Usar API quando aplicável** | Para concursos federais vinculados ao governo federal. |
| **PCI Concursos** (pciconcursos.com.br) | A | ✅ Verificado em 2026-03-13: `Allow: /` para `User-agent: *` com `Disallow` pontuais para admin/PDF/PHP | ⚠️ Footer público expõe política de privacidade/cancelamento, mas não foi identificada página pública dedicada de ToS permissivo para scraping | ❌ | ⚠️ **Implementado tecnicamente, mas ainda pendente para produção** | Scraper HTML concluído até a 8.3. Em 8.4 o site permaneceu `PENDING_REVIEW` para produção até fechamento formal do checklist legal completo. |
| **ConcursosNoBrasil** (concursosnobrasil.com.br) | A | ⚠️ A verificar | ⚠️ A verificar | ❌ | ⚠️ **Avaliar após PCI** | Alternativa ao PCI Concursos. |
| **IBGE** (ibge.gov.br/concursos) | A | ✅ | ✅ Dados governamentais | ❌ | ✅ **Tipo A permitido** | Concursos do IBGE são dados públicos. Site estático. |
| **ESAF / Cebraspe / FGV / IBFC** (bancas) | A | ⚠️ Cada banca tem seu próprio site | ⚠️ Verificar por banca | ❌ | ⚠️ **Verificar por banca** | Cada banca (ESAF, Cebraspe, FGV, IBFC, VUNESP, etc.) tem estrutura própria. Priorizar scraping de sites de divulgação (PCI, DOU) em vez de sites das bancas. |

**Resumo de decisão para Concursos Públicos:**

- DOU API: usar API oficial — **prioridade máxima** para concursos federais.
- Portal da Transparência API: usar API oficial quando aplicável.
- PCI Concursos: candidato a Tipo A após revisão formal do robots.txt.
- Bancas individuais: avaliar case-by-case, preferir fontes de divulgação consolidadas.

---

### 3. Processo Mandatório de Onboarding de Site

Antes de ativar qualquer site em produção, os seguintes campos devem ser preenchidos na entidade
`TargetSite` e revisados por um responsável do projeto:

```
□ URL do robots.txt revisada e resultado documentado
□ ToS revisado e status documentado (PERMITIDO / PROIBIDO / AVALIANDO)
□ API oficial verificada e, se disponível, preferida ao scraping
□ Endpoint oficial da API/documentação pública registrado quando a categoria for `API_OFICIAL`
□ Strategy/factory registrada para o `siteCode` antes de qualquer ativação em produção
□ Justificativa de negócio registrada
□ Perfil de rate-limit definido (jamais "sem limite")
□ Categoria legal: DADOS_PUBLICOS / API_OFICIAL / SCRAPING_PERMITIDO / SCRAPING_PROIBIDO
□ Contato/equipe responsável registrado
□ Status de autenticação requerida documentado
□ Existência de `schema.org/JobPosting`, sitemap ou endpoint público documentada quando aplicável
```

Nenhum scraper pode ser ativado em produção sem este checklist completo.

---

### 4. Requisitos Funcionais

- Registrar metadados do site-alvo e configuração de extração.
- Suportar crawling de páginas de listagem e extração de páginas de detalhe.
- Normalizar dados extraídos em registros canônicos de `JobPosting`.
- Detectar duplicatas por chaves de negócio estáveis ou fingerprints de conteúdo.
- Persistir snapshots brutos de HTML/JSON para debugging quando permitido.
- Armazenar resultado de extração, motivo de falha e timestamps de execução.
- Suportar trigger manual e jobs agendados.
- Suportar atualizações de parser/versão por site.
- Disponibilizar vagas filtradas por data de publicação (`publishedAt`).
- Integrar fontes via API oficial (Indeed MCP, DOU API) como adaptadores de primeira classe.
- Permitir verificação manual de aceite por pesquisa/intenção do usuário antes de promover uma
  nova família de fonte.

### 5. Requisitos Não-Funcionais

- **Manutenibilidade:** onboarding de novo site via nova strategy/factory, sem reescrever o pipeline.
- **Resiliência:** falha em um site não deve parar todos os jobs.
- **Escalabilidade:** múltiplos jobs processados de forma assíncrona.
- **Educação:** rate limits e delays de crawl por site são obrigatórios.
- **Observabilidade:** todo job é rastreável do agendamento à persistência.
- **Auditabilidade:** versões de seletores e decisões de parser devem ser historicizadas.
- **Testabilidade:** regras de extração validadas via fixtures antes do deploy.
- **Conformidade legal:** robots.txt, ToS e status de aprovação registrados mesmo quando o scraping é tecnicamente viável.
- **Aceitação funcional:** cada família de fonte deve ter um cenário manual reproduzível de busca
  do usuário, mesmo quando a query ainda estiver embutida em um `CrawlJob` configurado e não
  exposta como input livre na API pública.

### 6. Fora de Escopo da Fase Inicial

- CAPTCHA solving.
- Bypass de MFA.
- Crawling distribuído em grande escala.
- Geração autônoma de seletores por LLM em produção.
- Scrapers de sites classificados como SCRAPING_PROIBIDO.

## Consequences

### Benefícios

- Planejamento realista por família de fontes.
- Critérios claros para jsoup vs Playwright vs API client.
- Fatia de backlog mais limpa e controle de risco legal.
- Proteção reputacional e legal do projeto.
- Prioridade ao Indeed MCP já disponível no ambiente reduz esforço imediato.
- ATSs públicos reduzem o custo de onboarding para dezenas de PMEs sem precisar criar um scraper
  específico por empresa logo no início.

### Desafios

- Alguns sites podem mudar de Tipo A para Tipo C sem aviso.
- Selector drift exige manutenção contínua.
- APIs oficiais podem ter limites de acesso ou requerer aprovação formal.
- Mesmo em páginas públicas da empresa, `robots.txt` e ToS continuam mandatórios antes de ativar
  qualquer coleta.

## Next Steps

1. Registrar os primeiros sites candidatos sob esta taxonomia com robots.txt verificado.
2. Implementar adaptador Indeed MCP como primeira integração API-first.
3. Implementar adaptador DOU API como segunda integração para concursos federais.
4. Criar testes com fixtures por classe de site antes de codificar a extração em produção.
5. Preencher o checklist de onboarding para cada site antes de ativar em produção.
6. Executar teste manual de aceite por fonte usando uma pesquisa representativa do usuário, por
   exemplo:
   - vagas privadas: `desenvolvedor de software em java spring boot`
   - concursos: `concurso analista de ti` ou `concurso desenvolvedor java`

## References

- ADR001 — Direção arquitetural geral
- ADR003 — Stack Java e projetos open source de referência
- ADR005 — Modelo de domínio JobPosting
- ADR010 — Open source e projetos GitHub de referência
- [Indeed MCP Connector](https://to.indeed.com) — integração oficial disponível no ambiente
- [Diário Oficial da União — Dados Abertos](https://www.in.gov.br/dados-abertos)
- [Portal da Transparência — API](https://transparencia.gov.br/api-de-dados)
- [Codante.io Jobs API](https://docs.apis.codante.io/jobs-api) — para desenvolvimento e testes
- [Greenhouse Job Board API](https://developers.greenhouse.io/job-board.html)
- [Lever Postings API](https://github.com/lever/postings-api)
- [Ashby Public Job Posting API](https://developers.ashbyhq.com/docs/public-job-posting-api)
- [Google Search Central — JobPosting structured data](https://developers.google.com/search/docs/appearance/structured-data/job-posting)
- [Google Search Central — robots.txt](https://developers.google.com/search/docs/crawling-indexing/robots/intro)
