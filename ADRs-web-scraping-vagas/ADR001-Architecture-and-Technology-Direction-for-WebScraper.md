# ADR 001 — Direção Arquitetural e de Tecnologia para o WebScraper de Vagas

## Title

Decisão arquitetural para a plataforma WebScraper de vagas de emprego e concursos públicos com foco em Java Júnior e Spring Boot.

## Status

Accepted

## Date

2026-03-09

## Context

O projeto tem como objetivo coletar, normalizar e disponibilizar vagas de emprego e concursos públicos
brasileiros, filtradas especificamente para a stack Java Júnior / Spring Boot. As fontes de dados
incluem portais comerciais de vagas (setor privado) e fontes oficiais de concursos públicos.

A plataforma deve distinguir claramente entre duas estratégias de aquisição de dados:

1. **API-first** — quando o site ou serviço disponibiliza uma API oficial, documentada e
   legalmente utilizável (ex.: Indeed via MCP connector, Diário Oficial via API gov), essa via é
   obrigatoriamente preferida ao scraping de HTML.
2. **Scraping controlado** — aplicado apenas a fontes classificadas como permitidas após revisão
   explícita de `robots.txt`, Termos de Serviço e levantamento de risco legal, conforme
   catalogado no ADR002.

A escolha da linguagem de implementação é **Java**, alinhada ao objetivo de aprendizagem e
desenvolvimento de carreira do time em Java / Spring Boot. Python é descartado como linguagem
de produção, mas permanece como referência de benchmark conceitual (ver ADR003).

O projeto deve suportar:

- Páginas HTML estáticas com paginação.
- Páginas com renderização JavaScript quando comprovadamente necessário.
- Lógica de extração reutilizável entre famílias de sites.
- Persistência de registros normalizados, payloads brutos e histórico de execução.
- Retries controlados, rate limiting e processamento assíncrono.
- Disponibilização das vagas ordenadas e filtradas por data de publicação.
- Implementação 100% orientada a TDD.

## Decision

### 1. Tecnologias Selecionadas

| Componente | Tecnologia | Justificativa |
|---|---|---|
| Linguagem | Java 21 | Tipagem forte, ecossistema maduro, alinhamento com stack Spring Boot |
| Framework backend | Spring Boot 3.x | Injeção de dependência, agendamento, observabilidade, acesso a dados |
| Build | Maven | Estrutura padronizada e amplo suporte no ecossistema Java |
| Parsing HTML estático | jsoup | API conveniente, seletores CSS, parsing HTML5 |
| Automação de browser | Playwright for Java | Fallback controlado para páginas JS-heavy; suporta Chromium, WebKit, Firefox |
| Persistência | PostgreSQL | Armazenamento relacional confiável para vagas, execuções e auditoria |
| ORM | Spring Data JPA / Hibernate | Persistência produtiva com contratos de repositório claros |
| Cache / fila / coordenação | Redis | Estado de job, chaves de deduplicação, contadores de rate-limiter |
| Resiliência | Resilience4j | Retry, RateLimiter, Bulkhead e CircuitBreaker nativos no Spring Boot |
| Execução assíncrona | Spring Scheduling + @Async | Desacoplar trigger de crawl da execução do parser e da persistência |
| Testes | JUnit 5 + Mockito + Testcontainers + WireMock | TDD completo: unitário, integração e adaptadores |
| Integrações API oficiais | Indeed MCP Connector / REST clients | Consumir fontes com API oficial sem necessidade de scraping |

### 2. Estilo Arquitetural

- **Arquitetura em camadas** para clareza da aplicação.
- **Strategy pattern** para comportamento de extração por página/site.
- **Factory pattern** para seleção da implementação correta do scraper.
- **Orquestração por use-cases** para execução, normalização e persistência de crawls.
- **Ports and adapters** para clientes HTTP, automação de browser, fila e persistência.
- **API-first** como critério de prioridade obrigatória antes de qualquer implementação de scraping.

### 3. Domínio-Alvo

O domínio central é **vagas de emprego** para a stack Java Júnior / Spring Boot, divididas em:

- `PRIVATE_SECTOR` — vagas publicadas por empresas privadas em portais comerciais.
- `PUBLIC_CONTEST` — concursos públicos com edital, prazo de inscrição e número de vagas.

A entidade principal é `JobPosting`, que contém `publishedAt` como campo mandatório para
suportar ordenação e filtragem cronológica (ver ADR005).

### 4. Política de Entrega

> **TDD é mandatório.**
>
> Nenhuma feature pode começar pela implementação em produção.
> O primeiro artefato de qualquer feature deve ser um teste automatizado falhando.

## Consequences

### Benefícios

- Consistência total com o ecossistema Java/Spring.
- Reuso fácil de cross-cutting concerns: logging, retry, persistência.
- Extensão para novos sites sem reescrever o pipeline central.
- Melhor manutenibilidade de longo prazo via contratos tipados e TDD.
- Alinhamento direto com o objetivo de carreira em backend Java.
- Uso de API oficial (Indeed MCP, DOU API) reduz risco legal e manutenção de seletores.

### Desafios

- Automação de browser é mais pesada e lenta que parsing estático.
- Seletores por site são frágeis e devem ser versionados.
- Políticas anti-bot e restrições legais exigem governança, não apenas código.
- APIs oficiais podem ter limites de taxa e custo de acesso.

## Implementation Strategy

1. Definir o modelo de domínio e os contratos de scraper.
2. Implementar integrações API-first (Indeed MCP, DOU API) como primeiros adaptadores.
3. Adicionar pipeline estático com jsoup para sites classificados como permitidos.
4. Adicionar fallback dinâmico com Playwright apenas para casos comprovados.
5. Introduzir orquestração resiliente, filas e observabilidade.
6. Expandir famílias de sites incrementalmente sob TDD.

## Next Steps

1. Catalogar sites-alvo com análise legal (ADR002).
2. Formalizar seleção de bibliotecas e pesquisa de open source (ADR003, ADR010).
3. Criar primeiro vertical slice com testes: fetch → parse → normalize → persist.

## References

- ADR002 — Taxonomia e análise legal de sites brasileiros
- ADR003 — Avaliação Java vs Python e benchmarks
- ADR005 — Modelo de domínio JobPosting
- ADR009 — Plano de entregas XP
- ADR010 — Open source e projetos de referência no GitHub
