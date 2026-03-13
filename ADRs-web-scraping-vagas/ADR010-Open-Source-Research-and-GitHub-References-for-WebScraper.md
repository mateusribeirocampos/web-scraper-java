# ADR 010 — Pesquisa de Open Source e Projetos de Referência no GitHub

## Title

Levantamento de projetos open source relevantes no GitHub para scraping de vagas e concursos,
avaliação de reutilização e lições de design incorporadas à arquitetura desta plataforma.

## Status

Accepted

## Date

2026-03-09

## Context

Antes de implementar qualquer componente do zero, é obrigatório pesquisar se já existem soluções
open source reutilizáveis, adaptáveis ou que sirvam como referência de design. Esta pesquisa
serve a três propósitos:

1. **Reutilização direta:** componentes que podem ser incorporados ou adaptados ao projeto.
2. **Referência de design:** padrões, estruturas de dados e fluxos que informam nossa arquitetura.
3. **Análise de estrutura HTML:** projetos que já fizeram scraping dos mesmos sites revelam os
   seletores CSS e a estrutura DOM sem precisarmos inspecionar manualmente cada site.

---

## Decision

### 1. Projetos Java / Spring Boot de Referência

#### 1.1 `reljicd/spring-boot-web-scraper`

| Atributo | Valor |
|---|---|
| Linguagem | Java |
| Stack | Spring Boot + Thymeleaf + jsoup + Java 8 Lambdas & Streams |
| Link | https://github.com/reljicd/spring-boot-web-scraper |
| Licença | MIT |
| Status | Ativo (referência fundacional) |

**O que aprendemos deste projeto:**
- Confirma que a combinação Spring Boot + jsoup é idiomática e testável em Java.
- Demonstra o padrão de injetar o fetcher HTTP via Spring DI e usar jsoup para parsing.
- Mostra a separação entre a camada de fetch e a camada de parsing.
- Serve como template de estrutura de pacotes para o início do projeto.

**Decisão de uso:** Referência de arquitetura. Não importado como dependência.

---

#### 1.2 `mharisraza/WebLinkedInScraper`

| Atributo | Valor |
|---|---|
| Linguagem | Java |
| Stack | Java + Selenium + Spring Boot |
| Link | https://github.com/mharisraza/WebLinkedInScraper |
| Licença | A verificar |
| Status | Referência para padrão de browser automation no Spring Boot |

**O que aprendemos deste projeto:**
- Demonstra como integrar Selenium (ou analogamente Playwright) com Spring Boot como componente gerenciado.
- Confirma a necessidade de browser automation para LinkedIn (Tipo D).
- Reforça nossa decisão de **não fazer scraping do LinkedIn** diretamente — o projeto documenta
  explicitamente os desafios de anti-bot e autenticação do LinkedIn.

**Decisão de uso:** Referência de padrão de integração browser/Spring. Não importado.

---

#### 1.3 Botinok (GitHub Topics Java + LinkedIn + Spring Boot + Playwright)

| Atributo | Valor |
|---|---|
| Linguagem | Java |
| Stack | Spring Boot + Playwright + linkedin-scraper |
| Link | https://github.com/topics/linkedin?l=java |
| Status | Atualizado out/2024 |

**O que aprendemos deste projeto:**
- Demonstra integração nativa de Playwright com Spring Boot.
- Confirma que Playwright for Java é viável em produção dentro do ecossistema Spring.
- Mostra padrões de gerenciamento do ciclo de vida do browser como bean Spring.

**Decisão de uso:** Referência de integração Playwright/Spring Boot para ADR006 e ADR008.

---

### 2. Projetos Python de Alta Referência (benchmarks de design)

#### 2.1 `speedyapply/JobSpy`

| Atributo | Valor |
|---|---|
| Linguagem | Python |
| Stack | Python + aiohttp + BeautifulSoup + Playwright Python |
| Fontes suportadas | LinkedIn, Indeed, Glassdoor, Google Jobs, ZipRecruiter |
| Stars | 5.000+ |
| Licença | MIT |
| Link | https://github.com/speedyapply/JobSpy |

**O que aprendemos deste projeto:**

Este é o projeto de referência mais importante para o design de nosso scraper de vagas.

*Modelo de dados de vaga (campos normalizados pelo JobSpy):*
```python
# Campos que o JobSpy normaliza de TODAS as fontes
{
    "id": "...",
    "site": "indeed",
    "job_url": "https://...",
    "job_url_direct": "https://...",
    "title": "Software Engineer",
    "company": "Acme Corp",
    "location": "Remote",
    "date_posted": "2026-03-05",     # equivale ao nosso publishedAt
    "job_type": "fulltime",           # equivale ao nosso contractType
    "salary_source": "direct_data",
    "interval": "yearly",
    "min_amount": 80000,
    "max_amount": 120000,
    "currency": "USD",
    "is_remote": True,
    "description": "...",
    "skills_from_description": ["Java", "Spring Boot"],
    "company_url": "...",
    "emails": None
}
```

*Padrões de design incorporados à nossa arquitetura a partir do JobSpy:*

1. **Campo `date_posted` mandatório** → nosso `publishedAt` é campo `@Column(nullable = false)`.
2. **Normalização de `job_type` para enum** → nosso `JobContractType` (CLT, PJ, INTERNSHIP, etc.).
3. **Campo `is_remote` como boolean explícito** → nosso `boolean remote` na entidade.
4. **`min_amount` / `max_amount` separados** → nosso `salaryRange` (texto livre) + consideração
   futura de `BigDecimal minSalary, maxSalary` para análises.
5. **Rate limiting explícito com documentação do erro 429** → reforça nosso ADR006 de rate-limiter
   obrigatório por site sem perfil "ilimitado".
6. **Estratégia por site como módulo isolado** → equivalente ao nosso `JobScraperStrategy` por site.

**Decisão de uso:** Benchmark de design para normalização de campos. Não importado como dependência.

---

#### 2.2 `benpmeredith/jobspy`

| Atributo | Valor |
|---|---|
| Linguagem | Python (FastAPI) |
| Stack | FastAPI + JobSpy |
| Fontes | LinkedIn, Indeed, ZipRecruiter |
| Link | https://github.com/benpmeredith/jobspy |

**O que aprendemos:**
- Demonstra o padrão de expor scrapers como API REST — exatamente o que nosso `interfaces` layer faz.
- Mostra endpoints como `POST /jobs/search` com filtros de `site_name`, `search_term`, `location`,
  `date_range` — referência para nosso endpoint de listagem de vagas.

**Decisão de uso:** Referência de API REST sobre scrapers para o ADR de interfaces (futuro).

---

### 3. Projetos Brasileiros de Referência (Concursos Públicos)

#### 3.1 `luiseduardobr1/PCIConcursos`

| Atributo | Valor |
|---|---|
| Linguagem | Python |
| Fontes | PCI Concursos (pciconcursos.com.br) |
| Licença | A verificar |
| Link | https://github.com/luiseduardobr1/PCIConcursos |

**O que aprendemos — estrutura HTML do PCI Concursos:**

Este projeto revela os campos disponíveis no PCI Concursos e seus seletores CSS, o que evita
inspeção manual do site e acelera a implementação do `PciConcursosScraperStrategy`.

*Campos extraídos pelo projeto:*
```
- nome do concurso
- número de vagas
- nível de escolaridade
- salário máximo
- período de inscrições (início e fim)
- link de detalhes do edital
```

*Mapeamento para nossa entidade `PublicContestPostingEntity`:*

| Campo PCI Concursos | Campo em PublicContestPostingEntity |
|---|---|
| nome do concurso | `contestName` |
| número de vagas | `numberOfVacancies` |
| nível de escolaridade | `educationLevel` |
| salário máximo | `baseSalary` / `salaryDescription` |
| período de inscrições | `registrationStartDate` / `registrationEndDate` |
| link de detalhes | `editalUrl` / `canonicalUrl` |

*Decisão de uso:* Referência de estrutura HTML do PCI Concursos. Os seletores CSS precisam ser
verificados e validados com fixtures HTML reais antes de uso em produção (per ADR007).

**Atualização de implementação (Story 8.1):**

- fixture HTML local salva em `webscraper/src/test/resources/fixtures/pci/pci-concursos-listing.html`
- output esperado do parse salvo em
  `webscraper/src/test/resources/fixtures/pci/pci-concursos-listing-expected.json`
- preview observável implementado em `PciConcursosParsePreview`
- seletores provisórios congelados na fixture:
  - `article.ca`
  - `.ca-link`
  - `.ca-orgao`
  - `.ca-cargo`
  - `.ca-vagas`
  - `.ca-escolaridade`
  - `.ca-salario`
  - `.ca-inscricoes`
  - `.ca-detalhes`

Esses seletores ainda não representam um `SelectorBundle` de produção. A extração formal para
bundle versionado fica na Story 8.2.

**Atualização de implementação (Story 8.2):**

- `SelectorBundle` implementado em
  `webscraper/src/main/java/com/campos/webscraper/infrastructure/parser/SelectorBundle.java`
- bundle `pci_concursos_v1` implementado em
  `webscraper/src/main/java/com/campos/webscraper/infrastructure/parser/PciConcursosSelectorBundles.java`
- o parser do PCI agora consome o bundle versionado em vez de seletores inline

**Atualização de implementação (Story 8.3):**

- `PciConcursosScraperStrategy` implementada com paginação por `nextPage`
- `PciConcursosContestNormalizer` implementado para transformar cards do PCI em
  `PublicContestPostingEntity`
- o bundle `pci_concursos_v1` passou a incluir também o seletor de navegação:
  - `nav.pagination .next`

---

#### 3.2 `Vinimartinsc/concursosPublicosAPI`

| Atributo | Valor |
|---|---|
| Linguagem | — |
| Descrição | API que retorna concursos no Brasil via ConcursosNoBrasil.com.br. Propósito educacional. |
| Aviso | Mudanças no site fonte podem tornar a API indisponível. Não recomendado para produção. |
| Link | https://github.com/Vinimartinsc/concursosPublicosAPI |

**O que aprendemos:**
- Demonstra que há demanda por uma API consolidada de concursos — validação de mercado para
  o nosso projeto.
- Reforça que fontes não-oficiais de agregação de concursos são frágeis (selector drift).
- Reforça a decisão de priorizar a DOU API oficial como fonte primária para concursos federais.

**Decisão de uso:** Validação de domínio e aviso de risco. Não importado.

---

### 4. Recursos Curados de Referência

| Recurso | Descrição | Link |
|---|---|---|
| **awesome-linkedin-scrapers** | Coleção curada de scrapers LinkedIn com status de manutenção. Referência para identificar abordagens técnicas atuais. | [GitHub](https://github.com/The-Web-Scraping-Playbook/awesome-linkedin-scrapers) |
| **Web Scraping in Java and Spring Boot: Guide 2025** | Guia completo de scraping Java com Spring Boot + jsoup + Crawlbase Java SDK. | [GitHub](https://github.com/premiefbeme/HostDare/blob/main/overcome/Web%20Scraping%20in%20Java%20and%20Spring%20Boot:%20Comprehensive%20Guide%20for%202025.md) |
| **Codante.io Jobs API** | API pública de vagas para desenvolvimento e testes. Sem autenticação. Resposta JSON padronizada. | [Documentação](https://docs.apis.codante.io/jobs-api) |

---

### 5. Resumo de Decisões de Reutilização

| Projeto | Reutilização | Tipo de uso |
|---|---|---|
| `reljicd/spring-boot-web-scraper` | Template de arquitetura Spring Boot + jsoup | Referência estrutural |
| `speedyapply/JobSpy` | Modelo de dados de vaga normalizado; padrões de rate limit | Benchmark de design |
| `luiseduardobr1/PCIConcursos` | Estrutura HTML e campos do PCI Concursos | Referência de seletores |
| `benpmeredith/jobspy` | Padrão de API REST sobre scrapers | Referência de interface |
| `Codante.io Jobs API` | Dados de teste em dev/staging | Fixture dinâmica |
| Demais projetos | Padrões de integração browser/Spring | Referência técnica |

---

### 6. O que NÃO importar como dependência direta

Os projetos acima são referências de design e benchmark. **Nenhum deles deve ser adicionado como
dependência direta de produção**, pelas seguintes razões:

- Projetos Python são incompatíveis com nossa stack Java.
- Scrapers open source focados em LinkedIn violam os ToS do LinkedIn e podem gerar risco legal.
- APIs não-oficiais de concursos (ex.: `concursosPublicosAPI`) são instáveis por natureza.
- A plataforma que estamos construindo deve ter ownership total do código de extração para
  poder versionar seletores, aplicar rate limits e auditar cada site individualmente.

---

## Consequences

### Benefícios

- Acelera a fase de design sem reinventar a roda.
- Valida campos de domínio contra soluções existentes e bem mantidas.
- Revela a estrutura HTML de sites-alvo sem inspeção manual.
- Confirma viabilidade técnica de cada abordagem antes de implementar.

### Desafios

- Projetos de referência têm licenças variadas — verificar antes de copiar código diretamente.
- Seletores CSS de projetos de referência podem estar desatualizados; sempre validar com fixture.
- Soluções Python precisam de tradução cuidadosa para Java — os padrões são portáveis, não o código.

## Next Steps

1. Usar campos do `speedyapply/JobSpy` como checklist de completude do `JobPostingEntity`.
2. Usar `luiseduardobr1/PCIConcursos` para identificar seletores iniciais do PCI Concursos.
3. Salvar fixture HTML capturada do PCI Concursos para testes antes de implementar o scraper.
4. Verificar licença do `reljicd/spring-boot-web-scraper` antes de adaptar qualquer trecho de código.

## References

- ADR001 — Direção tecnológica
- ADR002 — Taxonomia de sites e análise legal
- ADR003 — Decisão Java vs Python
- ADR004 — Arquitetura de extração
- ADR005 — Modelo de domínio
- [speedyapply/JobSpy](https://github.com/speedyapply/JobSpy)
- [reljicd/spring-boot-web-scraper](https://github.com/reljicd/spring-boot-web-scraper)
- [luiseduardobr1/PCIConcursos](https://github.com/luiseduardobr1/PCIConcursos)
- [Codante.io Jobs API](https://docs.apis.codante.io/jobs-api)
