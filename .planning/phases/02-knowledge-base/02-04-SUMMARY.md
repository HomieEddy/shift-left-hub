---
phase: 02-knowledge-base
plan: 04
subsystem: ui
tags: [angular, kb, full-text-search, ngx-markdown, bilingual, i18n, pagination]

# Dependency graph
requires:
  - phase: 02-02
    provides: Public article API endpoints (GET /api/articles, /api/articles/search, /api/articles/{id})
  - phase: 02-03
    provides: Shared UI components pattern (Card, SearchInput, Pagination), article models

provides:
  - Public article card grid with bilingual display at /articles
  - Debounced FTS search with highlighted snippets at /articles/search
  - Article reading view with markdown rendering at /articles/:id
  - Bilingual fallback notice when content unavailable in selected language

affects:
  - Phase 3 (Ticket System) — KB article references in ticket context
  - Future Phase (Analytics) — view count tracking

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Public KB components with TranslationService.currentLang() for bilingual content
    - ts_headline search results rendered via [innerHTML] with sanitize wrapper
    - Article viewer with ngx-markdown for markdown rendering
    - Offset pagination with Previous/Next buttons

key-files:
  created:
    - frontend/src/app/features/kb/services/public-article.service.ts
    - frontend/src/app/features/kb/public/article-list/article-list.component.ts
    - frontend/src/app/features/kb/public/article-list/article-list.component.html
    - frontend/src/app/features/kb/public/article-search/article-search.component.ts
    - frontend/src/app/features/kb/public/article-search/article-search.component.html
    - frontend/src/app/features/kb/public/article-viewer/article-viewer.component.ts
    - frontend/src/app/features/kb/public/article-viewer/article-viewer.component.html
  modified:
    - frontend/src/app/app.routes.ts
    - frontend/src/app/app.html
    - frontend/src/locale/messages.xlf
    - frontend/src/locale/messages.fr.xlf

key-decisions:
  - "D-14: Card grid layout (1/2/3 responsive columns) for article listing"
  - "D-15: 300ms debounce for live search with ts_headline highlighted snippets"
  - "D-16: Dedicated search results page at /articles/search?q=query"
  - "D-17: Offset pagination, 20 items per page"
  - "D-19: Article content matches user's language; fallback to other language with notice"
  - "D-20: Article listing shows articles regardless of language"
  - "D-22: ngx-markdown for article viewer markdown rendering"

patterns-established:
  - "Public KB components: standalone, inject(), signals, bilingual display via TranslationService.currentLang()"
  - "Search URL sync: debounced input updates URL query params via router.navigate"
  - "Bilingual fallback: displayContent getter falls back to contentEn when contentFr absent"

requirements-completed: [KB-02, KB-03, KB-05]

# Metrics
duration: 8min
completed: 2026-06-01
---

# Phase 2 Plan 04: Public Knowledge Base Interface Summary

**Card grid article browser with pagination, debounced full-text search with highlighted snippets, and a clean article reading view with bilingual fallback and markdown rendering**

## Performance

- **Duration:** 8 min
- **Started:** 2026-06-01T01:59:15Z
- **Completed:** 2026-06-01T02:02:00Z
- **Tasks:** 5
- **Files modified:** 11 (7 created, 4 modified)

## Accomplishments

- **PublicArticleService** — Three public endpoints: getArticles (paginated), search (with q param + paginated), getArticleById
- **ArticleListComponent** — Responsive 1/2/3 column card grid with tag chips, bilingual title/excerpt, loading skeleton, empty state, and Previous/Next pagination
- **ArticleSearchComponent** — Debounced (300ms) input that syncs with URL ?q= param, auto-searches on page load, renders ts_headline snippets via [innerHTML], shows loading spinner, empty results, and initial search hint states
- **ArticleViewerComponent** — Clean reading view with markdown via ngx-markdown, bilingual content display with blue fallback notice, author/published date/view count meta bar, color-coded tag chips
- **Routes, nav, i18n** — Three lazy-loaded public routes (/articles, /articles/search, /articles/:id), "Knowledge Base" nav link for authenticated users, 11 new translation units in both EN and FR XLF files

## Task Commits

Each task was committed atomically:

1. **Task 1: PublicArticleService** — `2af11d5` (feat(kb): create PublicArticleService for public KB endpoints)
2. **Task 2: ArticleListComponent** — `18f485c` (feat(kb): create ArticleListComponent with responsive card grid and pagination)
3. **Task 3: ArticleSearchComponent** — `d80aa9b` (feat(kb): create ArticleSearchComponent with debounced FTS search)
4. **Task 4: ArticleViewerComponent** — `a0d38c5` (feat(kb): create ArticleViewerComponent with markdown and bilingual fallback)
5. **Task 5: Routes, nav, i18n** — `179940b` (feat(kb): add public KB routes, nav link, and bilingual i18n translations)

## Files Created/Modified

- `frontend/src/app/features/kb/services/public-article.service.ts` — Public article API service (no withCredentials)
- `frontend/src/app/features/kb/public/article-list/article-list.component.ts` — Card grid with bilingual display and pagination
- `frontend/src/app/features/kb/public/article-list/article-list.component.html` — Responsive card grid template with tag chips
- `frontend/src/app/features/kb/public/article-search/article-search.component.ts` — Debounced FTS search with URL sync
- `frontend/src/app/features/kb/public/article-search/article-search.component.html` — Search page template with results and highlights
- `frontend/src/app/features/kb/public/article-viewer/article-viewer.component.ts` — Article viewer with bilingual fallback logic
- `frontend/src/app/features/kb/public/article-viewer/article-viewer.component.html` — Reading view template with markdown rendering
- `frontend/src/app/app.routes.ts` — Added 3 public KB routes (modified)
- `frontend/src/app/app.html` — Added Knowledge Base nav link for authenticated users (modified)
- `frontend/src/locale/messages.xlf` — 11 English translation units (modified)
- `frontend/src/locale/messages.fr.xlf` — 11 French translation units (modified)

## Decisions Made

- All decisions followed the plan as specified (D-14 through D-22 from CONTEXT.md)
- Three separate route paths (/articles, /articles/search, /articles/:id) — Angular router matches `/articles/search` before `/articles/:id` due to registration order
- Knowledge Base nav link shown for all authenticated users (not just admin), consistent with public KB access model
- Search component debounce implemented inline rather than using shared SearchInput component, to enable URL param sync within the same component

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

None — all builds passed on first attempt for each task.

## Threat Surface Scan

No new threat surface introduced beyond what was documented in the plan's threat model:
- T-02-13: ts_headline rendering via [innerHTML] — mitigated by server-side sanitization (only `<mark>` tags)
- T-02-14: ngx-markdown rendering — mitigated by ngx-markdown's default HTML sanitization
- T-02-15: Public article visibility — backend-only enforcement (PUBLISHED status filter)
- T-02-16: Bilingual fallback — viewer checks `contentFr` existence before showing fallback notice

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- All public KB browsing, search, and reading features complete
- Phase 3 (Ticket System) can reference KB articles using the article ID and reader route
- View count tracking on article viewer provides foundation for analytics

---

*Phase: 02-knowledge-base*
*Completed: 2026-06-01*
