# Wiki Schema (layer 3)

Governs the LLM wiki in `docs/`. Full rationale: [[llm-wiki-architecture]] (`docs/engineering/llm-wiki-architecture.md`).

## Layers
1. `docs/raw/` — human-owned sources. **Never modify.**
2. `docs/architecture/`, `decisions/`, `engineering/` — LLM-owned wiki pages.
3. This file — structure, naming, workflows.

## Structural files
| File | Rule |
|---|---|
| `docs/home.md` | Orientation. Edit only when project-level facts change. |
| `docs/INDEX.md` | Catalog of every page. **Update on every ingest.** |
| `docs/log.md` | **Append-only.** One entry per operation. |
| `docs/<section>/overview.md` | Index only: one line + `[[wikilink]]` per page. |
| `docs/lint-YYYY-MM-DD.md` | One new file per lint run. |

## Page conventions
- kebab-case filenames; ADRs are `ADR-NNN-<slug>.md`, one decision per file.
- Internal links are `[[wikilinks]]` only. External links are normal markdown.
- Frontmatter: `title, type, tags, sources, related, created, updated, confidence`.
- `sources:` lists the `docs/raw/` files (or code paths) every claim comes from.
- Never invent facts. If a source does not say it, the page does not say it.
- Supersede, don't delete. Changed decision = new ADR. Wrong claim = fix in place and bump `updated:`.

## Workflows

### Ingest ("ingest docs/raw/<file>")
1. Read `.claude/wiki-schema.md` and `docs/INDEX.md`.
2. Read the raw source fully.
3. For each concept in it: if a page exists, **read it and make a minimal diff** (add the source to `sources:`, add facts, bump `updated:`); otherwise create a page from the template.
4. Add `[[wikilinks]]` both ways between related pages.
5. Append the new pages to the section `overview.md`, then to `INDEX.md`.
6. Append an `INGEST` entry to `log.md`.

### Query ("query the wiki: <question>")
Read `INDEX.md`, then the relevant pages; answer with `[[wikilink]]` citations; say so when the wiki lacks the answer. Offer to save novel answers as a page (then follow Ingest steps 5–6).

### Lint ("lint the wiki")
Check: broken wikilinks, orphan pages, contradictions, stale claims, missing concept pages, frontmatter, ADR numbering. Write `docs/lint-YYYY-MM-DD.md`, append a `LINT` entry to `log.md`. Never auto-delete.

## Page template
```markdown
---
title: ""
type: architecture | decision | engineering
tags: []
sources: ["raw/<file>.md"]
related: ["[[...]]"]
created: YYYY-MM-DD
updated: YYYY-MM-DD
confidence: low | medium | high
---
# Title
```

## log.md entry format
`## [YYYY-MM-DD] INGEST | <source>` followed by bullets: pages created, pages updated, index files touched.
