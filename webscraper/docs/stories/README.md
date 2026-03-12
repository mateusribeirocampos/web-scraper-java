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
