# Story Logs — WebScraper de Vagas

Cada arquivo nesta pasta documenta a implementação de uma story do plano XP (ADR009),
capturando tentativas, erros encontrados, decisões tomadas e o resultado final.

## Convenção de arquivo

```
STORY-{iteration}.{numero}-{slug}.md
```

Exemplos:
- `STORY-1.2-enums-value-objects.md`
- `STORY-2.1-target-site-entity.md`

## Seções obrigatórias em cada story log

| Seção | Conteúdo |
|---|---|
| **Objetivo** | O que a story entrega e por que é necessária |
| **Ciclo TDD** | Sequência Red → Green → Refactor com detalhes |
| **Arquivos criados / modificados** | Lista com caminho relativo e responsabilidade |
| **Problemas encontrados** | Erros, breaking changes, comportamentos inesperados |
| **Causa raiz** | Por que o problema aconteceu |
| **Solução aplicada** | O que foi mudado para resolver |
| **Lições aprendidas** | O que este projeto agora sabe que não sabia antes |
| **Estado final** | Compilação, testes, banco — tudo verde? |

## Índice de stories

| Story | Arquivo | Status |
|---|---|---|
| 1.2 — Enums e Value Objects | [STORY-1.2-enums-value-objects.md](STORY-1.2-enums-value-objects.md) | ✅ Concluída |
| 2.1 — TargetSiteEntity + Migration V001 | [STORY-2.1-target-site-entity.md](STORY-2.1-target-site-entity.md) | ✅ Concluída |
| 2.2 — CrawlJobEntity + CrawlExecutionEntity + Migration V002 | [STORY-2.2-crawl-job-execution-entity.md](STORY-2.2-crawl-job-execution-entity.md) | ✅ Concluída |
| 2.3 — JobPostingEntity + Deduplicação + Migration V004 | [STORY-2.3-job-posting-entity.md](STORY-2.3-job-posting-entity.md) | ✅ Concluída |
| 2.4 — PublicContestPostingEntity + Deduplicação + Migration V005 | [STORY-2.4-public-contest-posting-entity.md](STORY-2.4-public-contest-posting-entity.md) | ✅ Concluída |
| 3.1 — Contrato JobScraperStrategy | [STORY-3.1-job-scraper-strategy-contract.md](STORY-3.1-job-scraper-strategy-contract.md) | ✅ Concluída |
| 3.2 — JobScraperFactory | [STORY-3.2-job-scraper-factory.md](STORY-3.2-job-scraper-factory.md) | ✅ Concluída |
| 3.3 — JobFetcher | [STORY-3.3-job-fetcher.md](STORY-3.3-job-fetcher.md) | ✅ Concluída |
| 4.1 — IndeedApiClient | [STORY-4.1-indeed-api-client.md](STORY-4.1-indeed-api-client.md) | ✅ Concluída |
| 4.2 — IndeedJobNormalizer | [STORY-4.2-indeed-job-normalizer.md](STORY-4.2-indeed-job-normalizer.md) | ✅ Concluída |
| 4.3 — IndeedApiJobScraperStrategy | [STORY-4.3-indeed-api-job-scraper-strategy.md](STORY-4.3-indeed-api-job-scraper-strategy.md) | ✅ Concluída |
| 4.4 — Persistir vagas do Indeed | [STORY-4.4-indeed-job-import-use-case.md](STORY-4.4-indeed-job-import-use-case.md) | ✅ Concluída |
| 5.1 — DouApiClient | [STORY-5.1-dou-api-client.md](STORY-5.1-dou-api-client.md) | ✅ Concluída |
| 5.2 — DouContestNormalizer | [STORY-5.2-dou-contest-normalizer.md](STORY-5.2-dou-contest-normalizer.md) | ✅ Concluída |
| 5.3 — DouApiContestScraperStrategy | [STORY-5.3-dou-api-contest-scraper-strategy.md](STORY-5.3-dou-api-contest-scraper-strategy.md) | ✅ Concluída |
| 5.4 — Persistir concursos do DOU | [STORY-5.4-dou-contest-import-use-case.md](STORY-5.4-dou-contest-import-use-case.md) | ✅ Concluída |
| 6.1 — Trigger por agendador | [STORY-6.1-crawl-job-scheduler.md](STORY-6.1-crawl-job-scheduler.md) | ✅ Concluída |
