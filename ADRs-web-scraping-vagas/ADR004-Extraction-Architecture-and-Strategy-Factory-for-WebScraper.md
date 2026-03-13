# ADR 004 — Arquitetura de Extração e Padrões Strategy/Factory para o WebScraper de Vagas

## Title

Design técnico da arquitetura de extração usando Strategy e Factory, adaptado ao domínio de
vagas de emprego (setor privado) e concursos públicos brasileiros.

## Status

Accepted

## Date

2026-03-09

## Context

Cada site-alvo pode exigir fluxos de requisição, seletores, regras de normalização e
comportamento de fallback distintos. Hard-codificar branches específicos por site em services
genéricos criaria um monolito frágil. O projeto precisa de uma arquitetura de extração extensível.

O domínio-alvo é agora concreto: `JobPosting` (vagas do setor privado) e `PublicContestPosting`
(concursos públicos). Exemplos de sites estão classificados no ADR002.

---

## Decision

### 1. Fluxo Arquitetural Central

```text
Scheduler / Manual Trigger
        ↓
CrawlJobOrchestrator
        ↓
JobScraperFactory
        ↓
JobScraperStrategy  (resolve por site metadata)
        ↓
JobFetcher  (HTTP client  OU  ApiClient  OU  Browser automation)
        ↓
JobParser / Extractor
        ↓
JobNormalizer
        ↓
JobPostingDeduplicator
        ↓
JobPostingPersistenceService
        ↓
ExecutionLog / Metrics / Events
```

**Nota:** Fontes API-first (Indeed via MCP Connector, DOU API, ATS públicos como Greenhouse,
Lever e Ashby) seguem o mesmo fluxo, mas `JobFetcher` é um `ApiClient` em vez de um HTTP scraper.
Isso mantém o pipeline uniforme e permite escalar a cobertura de PMEs sem multiplicar scrapers
HTML frágeis por empresa.

---

### 2. Contratos Principais

#### `JobScraperStrategy`

Responsável pela lógica de extração fim-a-fim para um site ou família de sites.

```java
public interface JobScraperStrategy {

    /**
     * Retorna true se esta strategy suporta o site fornecido.
     * A resolução deve ser baseada em metadados explícitos, não apenas URL.
     */
    boolean supports(TargetSite targetSite);

    /**
     * Executa a extração e retorna um ScrapeResult com os JobPostings brutos.
     * Nunca lança exceção para falhas de extração esperadas; encapsula em ScrapeResult.
     */
    ScrapeResult<RawJobRecord> scrape(ScrapeCommand command);
}
```

#### `JobScraperFactory`

Responsável por selecionar a implementação correta da strategy.

```java
public interface JobScraperFactory {

    /**
     * Resolve a strategy correta para o site fornecido.
     * Lança UnsupportedSiteException se nenhuma strategy suportar o site.
     */
    JobScraperStrategy resolve(TargetSite targetSite);
}
```

#### `JobFetcher`

Abstrai a recuperação de página via HTTP ou browser.

```java
public interface JobFetcher {
    FetchedPage fetch(FetchRequest request);
}
```

Implementações concretas previstas:
- `HttpJobFetcher` — para sites estáticos (Type A, B) via OkHttp / RestTemplate.
- `PlaywrightJobFetcher` — para sites dinâmicos (Type C) via Playwright for Java.
- `IndeedApiClient` — para Indeed via MCP Connector / REST API oficial.
- `DouApiClient` — para Diário Oficial da União via API gov.
- `GreenhouseJobBoardClient` — para boards públicos hospedados no Greenhouse.
- `LeverPostingsClient` — para boards públicos hospedados no Lever.
- `AshbyJobBoardClient` — para boards públicos hospedados no Ashby.

#### `JobNormalizer`

Mapeia fragmentos brutos extraídos para entidades canônicas do domínio.

```java
public interface JobNormalizer<T extends RawJobRecord> {

    /**
     * Converte um registro bruto no JobPosting ou PublicContestPosting canônico.
     */
    JobPosting normalize(T rawRecord);
}
```

---

### 3. Tipos de Strategy

| Strategy | Tipo de Site | Fonte | Descrição |
|---|---|---|---|
| `IndeedApiJobScraperStrategy` | Tipo E | Indeed MCP/API | Consome API oficial do Indeed. Primeira strategy a ser implementada. |
| `DouApiContestScraperStrategy` | Tipo E | DOU API Gov | Consome API do Diário Oficial para editais de concurso. |
| `GreenhouseJobScraperStrategy` | Tipo E | Greenhouse Job Board API | Integra boards públicos de empresas pequenas e médias via provider padronizado. Implementada para `greenhouse_bitso`. |
| `LeverPostingsStrategy` | Tipo E | Lever Postings API | Integra boards públicos de empresas com ATS Lever. |
| `AshbyJobBoardStrategy` | Tipo E | Ashby Public Job Posting API | Integra boards públicos de startups e PMEs com ATS Ashby. |
| `StaticListingJobScraperStrategy` | Tipo A | PCI Concursos, Vagas.com | Parsing HTML estático via jsoup de páginas de listagem. |
| `StaticDetailJobScraperStrategy` | Tipo A/B | Gupy portal público | Parsing HTML de página de detalhe de vaga. |
| `DynamicBrowserJobScraperStrategy` | Tipo C | Sites JS-heavy | Playwright como fallback — apenas para sites comprovadamente Tipo C. |
| `AuthenticatedPortalJobScraperStrategy` | Tipo D | Sites com login | Somente após aprovação legal e integração com credential vault. |

**Regra adicional:** para o setor privado, uma strategy por provider ATS deve ser preferida a uma
strategy por empresa sempre que o provider disponibilizar job board público estável. A strategy por
empresa só entra quando a empresa expõe portal próprio sem provider reutilizável e com onboarding
legal aprovado.

---

### 4. Regra de Resolução da Factory

A factory deve resolver por metadados explícitos do site, não por cadeias frágeis de
`if/else` baseadas apenas em URL.

Metadados usados na resolução:

```java
public record TargetSite(
    String code,           // ex.: "INDEED_BR", "DOU_API", "PCI_CONCURSOS"
    String domain,
    SiteType siteType,     // STATIC, SEMI_DYNAMIC, DYNAMIC, AUTHENTICATED, API_BACKED
    JobCategory category,  // PRIVATE_SECTOR, PUBLIC_CONTEST
    boolean requiresAuth,
    ExtractionMode allowedMode,  // API_FIRST, STATIC_HTML, BROWSER
    String parserVersion,
    String rateLimitProfile,
    LegalStatus legalStatus  // APPROVED, PENDING_REVIEW, PROHIBITED
) {}
```

---

### 5. Versionamento de Seletores e Parsers

Cada strategy deve carregar regras de extração versionadas para que o selector drift
seja rastreável operacionalmente.

Estrutura sugerida para o bundle de seletores:

```java
public record SelectorBundle(
    String siteCode,
    String strategyName,
    String parserVersion,
    String selectorBundleVersion,
    LocalDate effectiveFrom,
    LocalDate deprecatedAt,
    Map<String, String> selectors  // campo -> seletor CSS ou path JSON
) {}
```

Exemplo para PCI Concursos:

```java
Map.of(
    "contestName",    ".concurso-titulo",
    "organizer",      ".orgao span",
    "numberOfVacancies", ".vagas strong",
    "salaryRange",    ".salario",
    "registrationDeadline", ".prazo-inscricao",
    "detailUrl",      "a.ver-edital"
)
```

---

### 6. Exemplo de Implementação: IndeedApiJobScraperStrategy

```java
@Component
public class IndeedApiJobScraperStrategy implements JobScraperStrategy {

    private final IndeedApiClient indeedApiClient;
    private final IndeedJobNormalizer normalizer;

    public IndeedApiJobScraperStrategy(IndeedApiClient indeedApiClient,
                                       IndeedJobNormalizer normalizer) {
        this.indeedApiClient = indeedApiClient;
        this.normalizer = normalizer;
    }

    @Override
    public boolean supports(TargetSite targetSite) {
        return "INDEED_BR".equals(targetSite.code())
            && targetSite.allowedMode() == ExtractionMode.API_FIRST;
    }

    @Override
    public ScrapeResult<RawJobRecord> scrape(ScrapeCommand command) {
        IndeedJobSearchResponse response = indeedApiClient
            .searchJobs(command.keywords(), command.location());

        List<JobPosting> postings = response.jobs().stream()
            .map(rawJob -> RawIndeedJobRecord.builder()
                .jobId(rawJob.jobId())
                .title(rawJob.title())
                .company(rawJob.company())
                .location(rawJob.location())
                .publishedAt(rawJob.postedAt())
                .applyUrl(rawJob.applyUrl())
                .build())
            .map(normalizer::normalize)
            .toList();

        return ScrapeResult.success(postings);
    }
}
```

---

### 7. Exemplo de Implementação: StaticListingJobScraperStrategy (PCI Concursos)

```java
@Component
public class PciConcursosScraperStrategy implements JobScraperStrategy {

    private final JobFetcher httpFetcher;
    private final PciConcursosNormalizer normalizer;
    private final SelectorBundle selectorBundle;

    @Override
    public boolean supports(TargetSite targetSite) {
        return "PCI_CONCURSOS".equals(targetSite.code())
            && targetSite.allowedMode() == ExtractionMode.STATIC_HTML;
    }

    @Override
    public ScrapeResult<RawJobRecord> scrape(ScrapeCommand command) {
        FetchedPage page = httpFetcher.fetch(new FetchRequest(command.url()));
        Document doc = Jsoup.parse(page.content());

        List<JobPosting> postings = doc
            .select(selectorBundle.selectors().get("contestCard"))
            .stream()
            .map(el -> RawContestRecord.builder()
                .contestName(el.select(selectorBundle.selectors().get("contestName")).text())
                .organizer(el.select(selectorBundle.selectors().get("organizer")).text())
                .numberOfVacancies(el.select(selectorBundle.selectors().get("numberOfVacancies")).text())
                .salaryRange(el.select(selectorBundle.selectors().get("salaryRange")).text())
                .registrationDeadline(el.select(selectorBundle.selectors().get("registrationDeadline")).text())
                .detailUrl(el.select(selectorBundle.selectors().get("detailUrl")).attr("abs:href"))
                .build())
            .map(normalizer::normalize)
            .toList();

        return ScrapeResult.success(postings);
    }
}
```

---

### 8. Regra TDD Mandatória para Componentes de Arquitetura

Antes de implementar qualquer strategy concreta, a seguinte sequência de testes é obrigatória:

1. Teste de resolução da factory falhando.
2. Teste de extração de fixture de página falhando.
3. Teste de normalização falhando (campos de vaga esperados).
4. Teste de integração de persistência falhando.
5. Somente então: código de produção.

---

## Consequences

### Benefícios

- Novos sites se tornam mudanças aditivas, não invasivas.
- Separação clara entre fetch, parse, normalize e persist.
- Testes mais fáceis com fixtures e adaptadores fake.
- API-first e scraping HTML seguem o mesmo pipeline — uniformidade.
- Suporte explícito aos dois domínios: vagas privadas e concursos públicos.

### Desafios

- Mais interfaces e abstrações no início.
- Requer disciplina para evitar atalhos de "caso especial".

## Next Steps

1. Implementar testes de contrato para resolução da factory.
2. Criar a primeira strategy API-first (IndeedApiJobScraperStrategy) a partir de fixture de resposta JSON.
3. Repetir o padrão API-first em novos providers reutilizáveis (`DouApiContestScraperStrategy`,
   `GreenhouseJobScraperStrategy`).
4. Criar a primeira strategy estática (PciConcursosScraperStrategy) a partir de fixture HTML.
5. Adicionar strategy de browser somente após cenário falhando provar necessidade.

## References

- ADR001 — Direção tecnológica
- ADR002 — Taxonomia de sites e análise legal
- ADR005 — Modelo de domínio JobPosting e PublicContestPosting
- ADR007 — TDD e quality gates
