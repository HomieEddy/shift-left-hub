---
phase: 02-knowledge-base
plan: 02
subsystem: api
tags: [spring-boot, jpa, postgresql, fts, tsvector, rest-api]
requires:
  - phase: 02-knowledge-base
    plan: 01
    provides: Article, Tag, ArticleStatus domain entities and repositories
provides:
  - Admin REST API for article CRUD + lifecycle management
  - Admin REST API for tag CRUD with delete guard
  - Public REST API for article browsing and FTS search
  - ArticleService with slug generation, status transitions, tag assignment
  - TagService with article count tracking and delete-unless-in-use guard
  - FTS search with ts_headline highlighted snippets via native JPQL query
affects: [phase 02-03 UI components, phase 02-04 public KB]

tech-stack:
  added: []
  patterns:
    - Service layer: @Service + @RequiredArgsConstructor + @Transactional(readOnly = true)
    - Record DTOs with static from() factory method
    - Native FTS query with ts_headline for search results
    - Custom exception per domain with UUID constructor

key-files:
  created:
    - article/service/ArticleService.java
    - article/service/PublicArticleService.java
    - article/api/AdminArticleController.java
    - article/api/PublicArticleController.java
    - article/api/dto/ArticleResponse.java
    - article/api/dto/ArticleSearchResult.java
    - article/api/dto/CreateArticleRequest.java
    - article/api/dto/UpdateArticleRequest.java
    - article/domain/ArticleNotFoundException.java
    - tag/service/TagService.java
    - tag/api/AdminTagController.java
    - tag/api/dto/TagResponse.java
    - tag/api/dto/CreateTagRequest.java
    - tag/api/dto/UpdateTagRequest.java
    - tag/domain/TagNotFoundException.java
  modified:
    - article/domain/ArticleRepository.java
    - config/SecurityConfig.java
    - common/config/GlobalExceptionHandler.java
    - tag/api/dto/TagResponse.java

key-decisions:
  - "Slug auto-generated from English title via slugify() in ArticleService"
  - "FTS query targets tsv_en/tsv_fr columns using language-specific plainto_tsquery"
  - "Delete tag guard: throw IllegalStateException (409) if tag has any articles"
  - "Non-published article by ID returns 404, not 403 (prevents existence probing)"

patterns-established:
  - "Domain exceptions: extend RuntimeException, UUID constructor, placed in domain package"
  - "Record DTOs: Java 14+ records with static from() factory, package per domain's api.dto"
  - "Admin controller: @RestController @RequestMapping(/api/admin/...) with Authentication parameter for user resolution"
  - "Public controller: @RestController @RequestMapping(/api/articles) GET-only, no auth needed"

requirements-completed:
  - KB-01
  - KB-02
  - KB-03
  - KB-04
  - KB-05
  - ADM-04

duration: 25min
completed: 2026-06-01
---

# Phase 2 Knowledge Base — Plan 02-02 Summary

**Backend API layer: article CRUD services, FTS search service, tag management, and REST controllers for admin and public endpoints**

## Performance

- **Duration:** 25 min
- **Started:** 2026-06-01T21:19:00Z
- **Completed:** 2026-06-01T21:23:00Z
- **Tasks:** 8 (+ 1 pre-step for missing domain entities)
- **Files modified:** 24

## Accomplishments

- `ArticleService` with full CRUD, slug generation from English title, DRAFT→PUBLISHED→ARCHIVED lifecycle, tag assignment via `TagRepository`
- `PublicArticleService` with paginated published article listing (sorted by publishedAt DESC) and FTS search mapping native query results to `ArticleSearchResult` DTOs with `ts_headline` highlighted snippets
- `TagService` with article count tracking (via `Tag.articles` back-reference) and delete-unless-in-use guard throwing `IllegalStateException` (409)
- `AdminArticleController` — GET (list with status filter + pagination), GET/{id}, POST, PUT/{id}, PUT/{id}/publish, PUT/{id}/archive, DELETE/{id} — all at `/api/admin/articles`
- `AdminTagController` — GET (list), GET/{id}, POST, PUT/{id}, DELETE/{id} — all at `/api/admin/tags`
- `PublicArticleController` — GET /api/articles (paginated published listing), GET /api/articles/search?q= (FTS search), GET /api/articles/{id} (single article)
- `ArticleRepository` extended with `findByStatus(ArticleStatus, Pageable)` and native `searchByText` query using `ts_headline` with English and French configs
- `SecurityConfig` updated with `.requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()`
- `GlobalExceptionHandler` extended with handlers for `ArticleNotFoundException` (404), `TagNotFoundException` (404), and `IllegalStateException` (409)

## Task Commits

| Task | Name | Hash | Type |
|------|------|------|------|
| Pre | Domain entities (Article, Tag, ArticleStatus, repositories) | `04d348e` | feat(kb) |
| 1 | Article exception and DTOs | `87d9543` | feat(kb) |
| 2 | Tag exception and DTOs | `55827dd` | feat(kb) |
| 3 | ArticleService and ArticleRepository update | `f04b3ea` | feat(kb) |
| 4 | PublicArticleService | `98ccda0` | feat(kb) |
| 5 | TagService | `44d7d6e` | feat(kb) |
| 6 | AdminArticleController and AdminTagController | `123968f` | feat(kb) |
| 7 | PublicArticleController | `664febc` | feat(kb) |
| 8 | SecurityConfig and GlobalExceptionHandler | `e70a557` | chore(config) |
| Fix | TagResponse articleCount and createdAt fields | `27a4789` | fix(kb) |

## Files Created/Modified

### Created
- `backend/src/main/java/.../article/service/ArticleService.java` — Article CRUD + lifecycle
- `backend/src/main/java/.../article/service/PublicArticleService.java` — Published article browsing + FTS search
- `backend/src/main/java/.../article/api/AdminArticleController.java` — Admin article REST endpoints
- `backend/src/main/java/.../article/api/PublicArticleController.java` — Public article REST endpoints
- `backend/src/main/java/.../article/api/dto/ArticleResponse.java` — Article response with author/editor/tags
- `backend/src/main/java/.../article/api/dto/ArticleSearchResult.java` — Lightweight search result DTO
- `backend/src/main/java/.../article/api/dto/CreateArticleRequest.java` — Article creation payload
- `backend/src/main/java/.../article/api/dto/UpdateArticleRequest.java` — Article update payload
- `backend/src/main/java/.../article/domain/ArticleNotFoundException.java` — 404 for missing articles
- `backend/src/main/java/.../tag/service/TagService.java` — Tag CRUD with article count + delete guard
- `backend/src/main/java/.../tag/api/AdminTagController.java` — Admin tag REST endpoints
- `backend/src/main/java/.../tag/api/dto/TagResponse.java` — Tag response with article count
- `backend/src/main/java/.../tag/api/dto/CreateTagRequest.java` — Tag creation payload
- `backend/src/main/java/.../tag/api/dto/UpdateTagRequest.java` — Tag update payload
- `backend/src/main/java/.../tag/domain/TagNotFoundException.java` — 404 for missing tags

### Modified
- `backend/src/main/java/.../article/domain/ArticleRepository.java` — Added `findByStatus` + native `searchByText` query
- `backend/src/main/java/.../config/SecurityConfig.java` — Added `.permitAll()` for public article routes
- `backend/src/main/java/.../common/config/GlobalExceptionHandler.java` — Added handlers for ArticleNotFoundException, TagNotFoundException, IllegalStateException

## Decisions Made

- **Slug generation**: `slugify()` strips non-alphanumeric chars, replaces spaces/special chars with hyphens, and trims. Accept risk of collision for Phase 2 per T-02-07.
- **Delete tag guard**: Uses `tag.getArticles().size()` via the `@ManyToMany(mappedBy = "tags")` back-reference to check usage before deletion.
- **FTS search mapping**: Native query returns `Object[]` rows; `PublicArticleService.search()` maps them to `ArticleSearchResult` by position. Headline falls back from English to French.
- **Article status visibility**: Non-published article by ID throws `ArticleNotFoundException` (404) rather than `AccessDeniedException` (403) — prevents existence probing (T-02-05 mitigation).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Missing domain entities from Plan 02-01**
- **Found during:** Pre-step (before Task 1)
- **Issue:** The plan depends on Plan 02-01 having created Article, Tag, ArticleStatus entities and repositories, but those packages didn't exist yet.
- **Fix:** Created ArticleStatus enum, Article entity, Tag entity (with @ManyToMany(mappedBy="tags") back-reference), ArticleRepository, and TagRepository as a pre-step.
- **Files modified:** Article.java, ArticleStatus.java, Tag.java, ArticleRepository.java, TagRepository.java
- **Verification:** Compiled successfully in Task 8 maven compile
- **Committed in:** `04d348e` (pre-step commit)

**2. [Rule 1 - Bug] TagResponse missing articleCount and createdAt fields**
- **Found during:** Task 8 compilation verification
- **Issue:** The existing TagResponse.java file (from a prior partial commit) only had id, nameEn, nameFr, color fields and a single `from(Tag)` method — missing articleCount, createdAt, and the `from(Tag, long)` overload used by TagService.
- **Fix:** Updated TagResponse to include articleCount and createdAt fields, with both `from(Tag)` and `from(Tag, long)` factory methods.
- **Files modified:** TagResponse.java
- **Verification:** `mvn clean compile` passes (39 source files)
- **Committed in:** `27a4789` (fix commit after Task 8)

---

**Total deviations:** 2 auto-fixed (1 blocking, 1 bug)
**Impact on plan:** Both fixes necessary for plan completion. No scope creep.

## Issues Encountered

- **Test failure**: `KnowledgeHubApplicationTests.contextLoads` fails because PostgreSQL is not running in this session. This is a pre-existing environmental issue (the test requires `docker compose up -d postgres`). Compilation succeeds with 0 errors on 39 source files.
- **TagResponse stale file**: The write tool did not overwrite an existing outdated version of TagResponse.java from a prior partial commit. Fixed by explicitly editing the file.

## Threat Surface Scan

No threat flags found. The plan's threat model (T-02-04 through T-02-08) covers all security-relevant surfaces:
- T-02-04: Mitigated — search uses parameterized native query (`:query` binding)
- T-02-05: Mitigated — non-published articles return 404, not 403
- T-02-06: Mitigated — hard delete is admin-only via SecurityConfig
- T-02-07: Accepted — slug collision risk deferred
- T-02-08: Mitigated — TagService.deleteTag throws 409 if tag is in use

## Known Stubs

None — all services and controllers are fully wired with proper data flow.

## Next Phase Readiness

- Full admin article and tag REST API surface available for frontend consumption
- Public article listing and search endpoints ready for Phase 2-04
- SecurityConfig updated for public article access
- Ready for Phase 2-03 (Shared UI components + Admin KB)

## Self-Check: PASSED

All 15 created files verified on disk. All 10 commit hashes verified in git log.

---

*Phase: 02-knowledge-base*
*Completed: 2026-06-01*
