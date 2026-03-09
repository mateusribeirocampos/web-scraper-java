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

**Resumo de decisão para Setor Privado:**

- Indeed: integrar via MCP Connector (API oficial) — **prioridade máxima**.
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
| **PCI Concursos** (pciconcursos.com.br) | A | ⚠️ A verificar formalmente | ⚠️ Dados de editais públicos; ToS menciona uso pessoal | ❌ | ⚠️ **Candidato a Tipo A com cautela** | Há precedente de projetos open source que fazem scraping deste site (ver ADR010). Revisar robots.txt antes de produção. Campos disponíveis: nome do concurso, nº de vagas, escolaridade, salário, prazo de inscrição, link. |
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
□ Justificativa de negócio registrada
□ Perfil de rate-limit definido (jamais "sem limite")
□ Categoria legal: DADOS_PUBLICOS / API_OFICIAL / SCRAPING_PERMITIDO / SCRAPING_PROIBIDO
□ Contato/equipe responsável registrado
□ Status de autenticação requerida documentado
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

### 5. Requisitos Não-Funcionais

- **Manutenibilidade:** onboarding de novo site via nova strategy/factory, sem reescrever o pipeline.
- **Resiliência:** falha em um site não deve parar todos os jobs.
- **Escalabilidade:** múltiplos jobs processados de forma assíncrona.
- **Educação:** rate limits e delays de crawl por site são obrigatórios.
- **Observabilidade:** todo job é rastreável do agendamento à persistência.
- **Auditabilidade:** versões de seletores e decisões de parser devem ser historicizadas.
- **Testabilidade:** regras de extração validadas via fixtures antes do deploy.
- **Conformidade legal:** robots.txt, ToS e status de aprovação registrados mesmo quando o scraping é tecnicamente viável.

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

### Desafios

- Alguns sites podem mudar de Tipo A para Tipo C sem aviso.
- Selector drift exige manutenção contínua.
- APIs oficiais podem ter limites de acesso ou requerer aprovação formal.

## Next Steps

1. Registrar os primeiros sites candidatos sob esta taxonomia com robots.txt verificado.
2. Implementar adaptador Indeed MCP como primeira integração API-first.
3. Implementar adaptador DOU API como segunda integração para concursos federais.
4. Criar testes com fixtures por classe de site antes de codificar a extração em produção.
5. Preencher o checklist de onboarding para cada site antes de ativar em produção.

## References

- ADR001 — Direção arquitetural geral
- ADR003 — Stack Java e projetos open source de referência
- ADR005 — Modelo de domínio JobPosting
- ADR010 — Open source e projetos GitHub de referência
- [Indeed MCP Connector](https://to.indeed.com) — integração oficial disponível no ambiente
- [Diário Oficial da União — Dados Abertos](https://www.in.gov.br/dados-abertos)
- [Portal da Transparência — API](https://transparencia.gov.br/api-de-dados)
- [Codante.io Jobs API](https://docs.apis.codante.io/jobs-api) — para desenvolvimento e testes
