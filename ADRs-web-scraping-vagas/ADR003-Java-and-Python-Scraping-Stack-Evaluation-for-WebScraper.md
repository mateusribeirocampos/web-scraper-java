# ADR 003 — Avaliação de Stack Java e Python para o WebScraper e Decisão Definitiva

## Title

Avaliação das bibliotecas Java e Python para scraping, rationale da escolha tecnológica
definitiva e levantamento de projetos open source de referência no GitHub.

## Status

Accepted

## Date

2026-03-09

## Context

O projeto precisava de uma decisão pragmática entre ecossistemas. Python é tradicionamente forte
em scraping e possui projetos open source maduros nessa área. Java oferece alinhamento mais forte
com a stack backend alvo e com o objetivo de carreira do time (Java Junior / Spring Boot).

A decisão não é trivial. Python vence em vários critérios isolados de scraping. Java vence
nos critérios que definem a natureza real do projeto. O raciocínio completo está nas seções abaixo.

Esta decisão é agora **definitiva e consolidada**: Java é a linguagem de produção. Python não
será usado em nenhum componente do sistema em produção. As razões são detalhadas e pontuadas abaixo.

---

## Decision

### 1. Análise Comparativa: Java vs Python por Dimensão Técnica

A tabela abaixo avalia cada linguagem em dimensões relevantes para **este projeto específico**,
com peso definido pelo impacto real em um sistema de backend de produção com scraping integrado.

> Escala: ✅ Vantagem clara · ⚖️ Equivalente / neutro · ⚠️ Desvantagem relativa
>
> Peso: 🔴 Alto impacto · 🟡 Médio impacto · 🟢 Baixo impacto

| Dimensão | Peso | Java | Python | Vencedor | Racional |
|---|---|---|---|---|---|
| **Ecossistema de scraping (bibliotecas)** | 🟡 Médio | ⚠️ jsoup, Playwright for Java — funcional mas menor comunidade de scraping | ✅ Scrapy, BeautifulSoup, Playwright, requests-html — ecossistema rico e maduro | Python | Python tem claramente mais ferramentas e exemplos focados em scraping. |
| **Velocidade de prototipação** | 🟡 Médio | ⚠️ Verbosidade Java, tipos explícitos, boilerplate de classes | ✅ Scripts rápidos, REPL interativo, Scrapy shell para debug | Python | Para entregar um scraper funcional rapidamente, Python é mais produtivo. |
| **Integração com backend Spring Boot** | 🔴 Alto | ✅ DI nativo, JPA, Actuator, Scheduling, Resilience4j, Flyway — tudo no mesmo runtime | ⚠️ Exige bridge, sidecar Python ou microserviço separado com overhead de comunicação | Java | Manter scraper e API no mesmo processo Spring elimina toda a complexidade de integração. |
| **Coerência do modelo de domínio** | 🔴 Alto | ✅ `JobPostingEntity`, repositórios, use cases, API — tudo em Java tipado | ⚠️ Domínio em Python + API em outro serviço = dois modelos de dados para sincronizar | Java | Duplicar o modelo de domínio entre duas linguagens é o maior risco arquitetural do projeto. |
| **Tipagem e segurança de refatoração** | 🔴 Alto | ✅ Tipagem estática forte. Compilador pega erros de contrato. Refatoração segura no IDE. | ⚠️ Tipagem dinâmica. Type hints ajudam, mas não são verificadas em runtime por padrão. | Java | Em um codebase de produção crescente, tipagem estática reduz regressões e facilita onboarding. |
| **Toolchain de TDD** | 🔴 Alto | ✅ JUnit 5, Mockito, Testcontainers, WireMock, AssertJ — ecossistema de testes de referência | ✅ pytest, responses, pytest-mock — igualmente excelente para TDD | ⚖️ Empate | Ambas as linguagens têm toolchains de teste de alta qualidade para TDD. |
| **Concorrência / requisições paralelas** | 🟡 Médio | ✅ Virtual Threads (Java 21) tornam I/O concorrente eficiente sem complexidade de async | ✅ asyncio + aiohttp (Python) — model event-loop maduro e eficiente para I/O | ⚖️ Empate | Java 21 equiparou Python em I/O-bound concurrency com Virtual Threads. |
| **Performance em I/O-bound workload** | 🟢 Baixo | ✅ JVM otimizada, JIT compilation, Virtual Threads | ✅ asyncio suficiente para scraping; GIL não é problema em I/O | ⚖️ Empate | Para scraping de sites reais limitado por rate limit externo, a diferença de performance é irrelevante. |
| **Deploy e operação** | 🟡 Médio | ✅ Único artefato JAR + container. Spring Actuator para health e métricas. | ⚠️ Runtime Python separado, dependências via pip/virtualenv/poetry, segunda camada de ops | Java | Um deployment reduz superfície operacional — logs, tracing, health checks em um único lugar. |
| **Alinhamento com objetivo de carreira** | 🔴 Alto | ✅ Java Júnior / Spring Boot é o objetivo declarado do time. Projeto vira portfolio real. | ⚠️ Construir em Python desvia o foco de desenvolvimento da competência central | Java | Cada hora investida neste projeto em Java é hora de aprendizado de Spring Boot em contexto real. |
| **Comunidade e recursos de scraping** | 🟢 Baixo | ⚠️ Menos tutoriais, menos exemplos de scraping em Java | ✅ Muito mais conteúdo de scraping em Python online | Python | Compensado pelo uso de open source Python como referência de design (sem dependência de produção). |
| **Manutenibilidade de longo prazo** | 🔴 Alto | ✅ Contratos explícitos, compilador como barreira de qualidade, IDE refactoring poderoso | ⚠️ Em bases de código maiores, ausência de tipos em runtime é fonte de bugs silenciosos | Java | Para um sistema que vai crescer incrementalmente por meses, a barreira de compilação é valiosa. |

---

### 2. Matriz de Decisão Ponderada

| Categoria de Critério | Peso relativo | Java | Python |
|---|---|---|---|
| Integração de backend + modelo de domínio | 35% | ✅ Forte | ⚠️ Fraco |
| TDD + manutenibilidade + tipagem | 30% | ✅ Forte | ⚖️ Neutro |
| Alinhamento de carreira e aprendizado | 20% | ✅ Forte | ⚠️ Fraco |
| Ecossistema de scraping + velocidade | 15% | ⚠️ Fraco | ✅ Forte |

**Resultado:** Java vence em 85% do peso dos critérios. Python vence nos 15% restantes (ecossistema
de scraping e prototipação), que são compensados pelo uso de projetos open source Python como
referência de design sem custo de produção.

---

### 3. O Argumento Crítico: este projeto é 30% scraper, 70% plataforma backend

A pergunta correta não é "qual linguagem é melhor para scraping?", mas sim:

> **"Qual linguagem é melhor para construir uma plataforma backend com scheduling, persistência,
> resiliência, API REST, filas e scraping integrados como um componente?"**

Se o projeto fosse **apenas** um scraper de linha de comando que gera um CSV, Python venceria
facilmente. Mas este projeto inclui:

- API REST para listagem de vagas por data e filtros.
- Modelo de domínio persistido com JPA/PostgreSQL.
- Scheduler de crawl com cron.
- Resiliência com Retry, RateLimiter, CircuitBreaker.
- Queue para processamento assíncrono.
- Observabilidade com Spring Actuator + métricas.
- TDD em cada camada com Testcontainers.

Adicionar Python ao projeto para cobrir apenas os ~30% de scraping exigiria:
- Um segundo runtime, dependências e toolchain de CI.
- Um segundo modelo de dados (ou serialização entre linguagens).
- Dois deployments coordenados.
- Dois contextos de observabilidade e logging.
- Uma fronteira de integração adicional para testar.

O custo total de Python como sidecar supera em muito o benefício de scraping mais ergonômico.

---

### 4. Decisão Final: Java como única linguagem de produção

**Java 21 + Spring Boot 3.x** é a stack de produção. Não há componente Python, não há
sidecar Python, não há microserviço Python.

Razões consolidadas (priorizadas por impacto):

1. **Coerência arquitetural** — domain model, persistence, API e scraper no mesmo runtime elimina
   a maior fonte de complexidade acidental de sistemas multi-linguagem.
2. **Tipagem forte e refatoração segura** — o compilador Java é uma ferramenta de qualidade de
   design, não apenas de execução.
3. **Spring Boot nativo** — DI, scheduling, JPA, Actuator, Resilience4j são componentes de
   primeira classe, não integrações externas.
4. **Alinhamento de carreira** — cada feature implementada é experiência real em Spring Boot que
   vai direto para o portfólio.
5. **Virtual Threads (Java 21)** — equiparam Java a Python/asyncio para I/O-bound concurrency,
   eliminando a última vantagem técnica de Python neste contexto.
6. **Open source Python como referência de design** — o custo de prototipação mais lento em Java
   é compensado pelo benchmark dos projetos Python existentes (JobSpy, PCIConcursos) que revelam
   modelos de dados e estruturas HTML sem escrever código Python em produção.

---

### 2. Bibliotecas Java Avaliadas

| Biblioteca / Ferramenta | Pontos Fortes | Limitações | Decisão |
|---|---|---|---|
| **jsoup** | API simples, parsing HTML robusto, seletores CSS, manipulação de DOM. Amplamente adotado para scraping Java. | Não é um browser real; limitado para JS pesado | **Adotado como biblioteca primária de extração estática** |
| **Playwright for Java** | Automação de browser cross-browser (Chromium/WebKit/Firefox), útil para alvos JS-heavy. API síncrona e assíncrona. | Custo de execução maior, overhead de infraestrutura | **Adotado como fallback controlado (somente Tipo C)** |
| **OkHttp / Spring WebClient** | Clientes HTTP eficientes para consumir APIs JSON (Indeed MCP, DOU API, Codante) | Não fazem parsing HTML | **Adotados para integrações API-first** |
| **Selenium** | Ecossistema maduro, automação ampla de browser | Custo de manutenção maior para este caso de uso vs Playwright | Não adotado |
| **HtmlUnit** | Simulação headless de browser dentro da JVM | Fidelidade menor que browser real para JS moderno | Não adotado |

---

### 3. Bibliotecas Python — Referência Conceitual (não produção)

| Biblioteca / Ferramenta | Pontos Fortes | Por que não adotada |
|---|---|---|
| **Scrapy** | Framework maduro para crawling; modelo spider; execução async/event-driven. Excelente como benchmark de design. | Segunda linguagem/runtime na plataforma |
| **BeautifulSoup** | API de parsing simples, ampla curva de aprendizado | Fraca como choice de arquitetura fim-a-fim |
| **Playwright Python** | Mesmo modelo robusto de automação disponível em Python | Multi-linguagem + custo de browser |
| **Requests + lxml** | Leve e produtivo para muitos casos | Requer mais montagem de arquitetura customizada |

Scrapy em particular continua sendo um benchmark excelente para:
- design de lifecycle de spider,
- padrões de orquestração de crawl,
- conceitos de extração incremental,
- debugging ergonômico via shell interativo.

Esses conceitos informam o design Java, mas não há produção Python.

---

### 4. Projetos Open Source de Referência no GitHub

A pesquisa de open source teve dois objetivos: (a) identificar soluções reutilizáveis ou
adaptáveis e (b) aprender padrões de design de scraping de vagas antes de implementar do zero.

#### 4.1 Projetos Java / Spring Boot relevantes

| Projeto | Linguagem | Descrição | Relevância | Link |
|---|---|---|---|---|
| **reljicd/spring-boot-web-scraper** | Java | Spring Boot + Thymeleaf + jsoup + Java 8 Lambdas & Streams. Template fundacional para projetos de scraping. | ✅ Alta — base de referência de arquitetura | [GitHub](https://github.com/reljicd/spring-boot-web-scraper) |
| **mharisraza/WebLinkedInScraper** | Java | Scraper LinkedIn com Selenium + Spring Boot. Exemplo de integração browser automation no Spring Boot. | ✅ Média — referência de padrão de integração Selenium/Spring | [GitHub](https://github.com/mharisraza/WebLinkedInScraper) |
| **Botinok** (GitHub Topics Java LinkedIn) | Java | Spring Boot + Playwright + linkedin-scraper. Automatiza interações no LinkedIn com Playwright. | ✅ Média — referência de integração Playwright/Spring Boot | [GitHub Topics Java LinkedIn](https://github.com/topics/linkedin?l=java) |

#### 4.2 Projetos Python de Alta Referência

| Projeto | Linguagem | Descrição | Relevância como benchmark | Link |
|---|---|---|---|---|
| **speedyapply/JobSpy** | Python | Scraper multi-plataforma: LinkedIn, Indeed, Glassdoor, Google, ZipRecruiter. 5k+ stars. | ✅ Alta — benchmark completo de scraping de vagas, padrões de normalização, tratamento de 429 | [GitHub](https://github.com/speedyapply/JobSpy) |
| **benpmeredith/jobspy** | Python (FastAPI) | FastAPI job scraper para LinkedIn, Indeed, ZipRecruiter. | ✅ Média — referência de API REST sobre scrapers | [GitHub](https://github.com/benpmeredith/jobspy) |

#### 4.3 Projetos Brasileiros de Referência (Concursos)

| Projeto | Linguagem | Descrição | Relevância | Link |
|---|---|---|---|---|
| **luiseduardobr1/PCIConcursos** | Python | Extrai concursos do PCI Concursos em nível nacional e estadual. Salva CSV com nome, vagas, escolaridade, salário, prazo, link. | ✅ Alta — demonstra estrutura HTML do PCI Concursos e campos extraíveis | [GitHub](https://github.com/luiseduardobr1/PCIConcursos) |
| **Vinimartinsc/concursosPublicosAPI** | — | API que retorna concursos no Brasil (via ConcursosNoBrasil). Propósito educacional. | ✅ Média — referência de domínio de concursos | [GitHub](https://github.com/Vinimartinsc/concursosPublicosAPI) |

#### 4.4 Recursos de Documentação

| Recurso | Descrição | Link |
|---|---|---|
| **awesome-linkedin-scrapers** | Coleção curada de scrapers LinkedIn: Python, Node.js, Playwright, Puppeteer, Selenium. Status de manutenção atualizado. | [GitHub](https://github.com/The-Web-Scraping-Playbook/awesome-linkedin-scrapers) |
| **Web Scraping in Java and Spring Boot: Guide 2025** | Guia completo de scraping Java com Spring Boot, jsoup e Crawlbase Java SDK. | [GitHub](https://github.com/premiefbeme/HostDare/blob/main/overcome/Web%20Scraping%20in%20Java%20and%20Spring%20Boot:%20Comprehensive%20Guide%20for%202025.md) |

---

### 5. Quando Python seria a escolha certa (e por que não é aqui)

Para fins de transparência e revisão futura, os cenários em que Python seria a escolha superior:

| Cenário | Python venceria? | Por quê |
|---|---|---|
| Scraper de linha de comando que gera CSV | ✅ Sim | Sem backend, sem API, sem persistência. Scrapy faz isso em horas. |
| Prova de conceito rápida para validar seletores | ✅ Sim | Scrapy shell + BeautifulSoup é imbatível para prototipar seletores. |
| Pipeline de dados / ETL com pandas | ✅ Sim | Ecossistema de dados Python é superior. |
| Microserviço de scraping isolado sem portfólio Java | ✅ Sim | Se o objetivo não for aprender Java, Python entrega mais rápido. |
| **Plataforma backend com API, JPA, Scheduling, Resilience** | ❌ Não | Java/Spring Boot é nativo; Python exige integração externa custosa. |
| **Projeto de portfólio para Java Junior backend developer** | ❌ Não | Python desvia o investimento de aprendizado do objetivo central. |

---

### 6. Lições Aprendidas dos Projetos Open Source

Com base na análise dos projetos acima, os seguintes padrões de design foram incorporados nas
decisões arquiteturais desta plataforma:

1. **Rate limiting per site é crítico** — JobSpy documenta explicitamente o erro 429 como
   consequência de excesso de requisições; nosso ADR006 define rate-limiter obrigatório por site.

2. **Estrutura HTML do PCI Concursos** — o projeto `luiseduardobr1/PCIConcursos` confirma que
   o site expõe: nome do concurso, número de vagas, nível de escolaridade, salário máximo, prazo
   de inscrição e link de detalhes — todos campos mapeados no nosso `PublicContestPosting` (ADR005).

3. **Spring Boot + jsoup é viável e testável** — `reljicd/spring-boot-web-scraper` confirma
   a combinação como arquiteturalmente sólida e idiomática em Java.

4. **Playwright é necessário para LinkedIn** — projetos como Botinok e WebLinkedInScraper
   demonstram que LinkedIn requer browser automation por ser Type D/C. Isso reforça nossa
   decisão de excluir LinkedIn do escopo de scraping direto.

5. **API-first reduz drasticamente a manutenção** — JobSpy documenta que scrapers de HTML são
   frágeis por natureza; nosso ADR002 prioriza API oficial (Indeed MCP, DOU API) exatamente
   para eliminar esse custo operacional nas fontes mais importantes.

---

## Consequences

### Benefícios

- Alinhamento total com portfólio e estratégia Java/Spring do time.
- Overhead cognitivo e de deployment reduzido pela ausência de dual-language core.
- Boa separação entre extração estática primária e fallback dinâmico explícito.
- Open source de referência acelera a fase de design sem comprometer a decisão de stack.

### Desafios

- Exemplos e conteúdo da comunidade de scraping são mais abundantes em Python.
- Time familiar com Scrapy pode inicialmente perceber Java como mais lento para prototipação.

## Next Steps

1. Criar os contratos centrais de extrator com jsoup.
2. Reservar adaptadores Playwright para sites classificados como Tipo C.
3. Estudar `luiseduardobr1/PCIConcursos` como referência de estrutura HTML do PCI Concursos.
4. Estudar `speedyapply/JobSpy` como benchmark de normalização de campos de vagas.
5. Documentar critérios explícitos para quando um site pode escalar de parsing estático para
   automação de browser.

## References

- ADR001 — Direção tecnológica
- ADR002 — Análise legal de sites
- ADR010 — Open source e projetos GitHub (aprofundamento)
- [speedyapply/JobSpy](https://github.com/speedyapply/JobSpy) — benchmark de scraping de vagas
- [reljicd/spring-boot-web-scraper](https://github.com/reljicd/spring-boot-web-scraper) — template Java
- [luiseduardobr1/PCIConcursos](https://github.com/luiseduardobr1/PCIConcursos) — estrutura PCI Concursos
