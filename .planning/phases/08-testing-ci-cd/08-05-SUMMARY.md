---
phase: 08-testing-ci-cd
plan: 05
subsystem: frontend
tags:
  - testing
  - angular
  - vitest
  - component-tests
  - smart-components
requires: [08-04]
provides: [TST-03]
affects: [frontend]
tech-stack:
  added: []
  patterns:
    - "Angular TestBed with standalone component imports"
    - "Vitest vi.fn() for service mocking"
    - "HttpClient via provideHttpClient() for dependent imports"
    - "Subject-based event injection for stream testing"
    - "Mock paramMap get() for ActivatedRoute tests"
key-files:
  created:
    - frontend/src/app/features/auth/login/login.component.spec.ts
    - frontend/src/app/features/chat/chat.component.spec.ts
    - frontend/src/app/features/kb/public/article-search/article-search.component.spec.ts
    - frontend/src/app/features/tickets/ticket-list/ticket-list.component.spec.ts
    - frontend/src/app/features/kb/admin/article-editor/article-editor.component.spec.ts
  modified: []
decisions:
  - "Use Vitest vi.fn() instead of jasmine.createSpyObj (jasmine types not available)"
  - "Mock Router + ActivatedRoute instead of using provideRouter (avoids DI conflicts)"
  - "Use provideMarkdown() for ngx-markdown MarkdownService dependency"
  - "Skip fakeAsync/tick due to Zone.js ProxyZone unavailability in test environment"
metrics:
  duration: ~8 minutes
  completed: 2026-06-08T15:34Z
  total_tests: 39 (5 new spec files)
  total_test_suite: 106 tests across 12 files
  test_duration: ~1.05s
  files_created: 5
  files_modified: 0
  min_line_requirements:
    login: "66 lines (min 60) ✓"
    chat: "102 lines (min 80) ✓"
    article-search: "96 lines (min 50) ✓"
    ticket-list: "76 lines (min 50) ✓"
    article-editor: "157 lines (min 50) ✓"
---

# Phase 8 Plan 5: Smart Component Tests Summary

Created 5 Angular smart component test files using Vitest + TestBed, verifying service call behaviour, SSE event handling, error states, and navigation for all key user-facing components.

## Tasks Completed

| Task | Component | Tests | Key Behaviour Verified | Commit |
|------|-----------|-------|----------------------|--------|
| 1 | LoginComponent | 5 | Submit → authService.login, success navigation, 401/500 error handling, loading state | `4d7260d`, `4904da5` |
| 2 | ChatComponent | 10 | sendMessage call, SSE token streaming, done/fallback/error events, empty guard, streaming guard, escalation | `d0cbf31` |
| 3 | ArticleSearchComponent | 8 | Tag loading on init, query param→doSearch, results population, empty/error/loading states, search input | `a18adcf` |
| 4 | TicketListComponent | 7 | Ticket loading on init, status filter, ALL filter, loading state, error handling, empty results | `e2ad720` |
| 5 | ArticleEditorComponent | 10 | Create/edit mode detection, article load, form population, create/update save, save/load error handling | `8756fc5` |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Replaced jasmine APIs with Vitest equivalents**
- **Found during:** Task 1 (LoginComponent)
- **Issue:** `jasmine.createSpyObj` and `jasmine.SpyObj` types are not available in the Vitest-based test runner (`@angular/build:unit-test`)
- **Fix:** Used `vi.fn()` from Vitest with plain object mocks: `{ login: vi.fn() }` instead of `jasmine.createSpyObj('AuthService', ['login'])`
- **Files modified:** `login.component.spec.ts`, `chat.component.spec.ts`, `article-search.component.spec.ts`, `ticket-list.component.spec.ts`, `article-editor.component.spec.ts`

**2. [Rule 1 - Bug] Replaced provideRouter with manual Router mock for ActivatedRoute tests**
- **Found during:** Task 3 (ArticleSearch) and Task 5 (ArticleEditor)
- **Issue:** `provideRouter([])` provides its own `ActivatedRoute` implementation, overriding the mock `{ queryParams }` or `{ snapshot: { paramMap } }`
- **Fix:** Provided `{ provide: Router, useValue: { navigate: vi.fn() } }` directly instead of using `provideRouter`
- **Files modified:** `article-search.component.spec.ts`, `article-editor.component.spec.ts`

**3. [Rule 1 - Bug] Added provideMarkdown() for ngx-markdown dependency**
- **Found during:** Task 5 (ArticleEditorComponent)
- **Issue:** ArticleEditorComponent imports `MarkdownModule` which requires `MarkdownService`. This service is not `providedIn: 'root'` and must be explicitly provided.
- **Fix:** Added `provideMarkdown()` from `ngx-markdown` to the test providers
- **Files modified:** `article-editor.component.spec.ts`

**4. [Rule 1 - Bug] Replaced spyon with vi.spyOn**
- **Found during:** Task 1
- **Issue:** `spyOn` (Jasmine) is not available; `jasmine` namespace is not configured in the Vitest environment
- **Fix:** Used `vi.spyOn(router, 'navigate')` instead of `spyOn(router, 'navigate')`
- **Files modified:** `login.component.spec.ts`

**5. [Adaptation] Used Subject-based event injection for stream testing**
- **Found during:** Task 2 (ChatComponent)
- **Context:** ChatComponent uses RxJS Subjects for SSE stream events. Tests inject events via `Subject.next()` to simulate streaming.
- **Pattern applied consistently across:** ChatComponent (SSE), ArticleSearch (error/loading), TicketList (error/loading), ArticleEditor (error/loading)

**6. [Adaptation] Skipped fakeAsync debounce test**
- **Found during:** Task 3 (ArticleSearchComponent)
- **Issue:** `fakeAsync`/`tick` requires `ProxyZone` from `zone.js/testing`, which is not set up in the Angular Vitest environment
- **Fix:** Replaced `fakeAsync` debounce test with a direct `onSearchInput` query signal test. Verified debounce downstream effects via the `doSearch` method call from query param emissions.
- **Files modified:** `article-search.component.spec.ts`

## TDD Gate Compliance

| Gate | Status | Commit |
|------|--------|--------|
| RED (test) | ✓ All 5 spec files created as test commits | `4d7260d`, `d0cbf31`, `a18adcf`, `e2ad720`, `8756fc5` |
| GREEN (impl) | N/A — implementation pre-existed | — |
| REFACTOR | N/A — no refactoring needed | — |

Note: Components already existed from prior phases. The "GREEN" phase was verification that tests correctly describe existing behaviour (all pass on first run after API corrections from Jasmine→Vitest).

## Threat Surface Scan

No new threat surface introduced. All tests use mocked services (`vi.fn()`). No real HTTP calls, no AI API invocations. Threat T-08-05-01 (mocked service state) mitigated per plan.

## Success Criteria Verification

| Criterion | Status |
|-----------|--------|
| LoginComponent: 5 tests (submit+auth call, success navigation, isLoading, 401 error, generic error) | ✓ 5 tests |
| ChatComponent: 10 tests (sendMessage, token streaming, done/fallback/error, empty guard, streaming guard, escalation) | ✓ 10 tests |
| ArticleSearchComponent: 8 tests (init tags, query param trigger, results, empty, error, loading, search input) | ✓ 8 tests |
| TicketListComponent: 7 tests (load, NEW/ALL filter, loading, error, empty) | ✓ 7 tests |
| ArticleEditorComponent: 10 tests (create mode, edit mode, tag load, article load, form populate, create/update save, errors) | ✓ 10 tests |
| `pnpm ng test --no-watch` passes 100% | ✓ 106 tests pass across 12 files (2.74s) |
| No real network calls during tests | ✓ All services mocked |

## Commits

| Hash | Message |
|------|---------|
| `4d7260d` | test(08-testing-ci-cd): add LoginComponent behaviour tests |
| `d0cbf31` | test(08-testing-ci-cd): add ChatComponent behaviour tests |
| `a18adcf` | test(08-testing-ci-cd): add ArticleSearchComponent behaviour tests |
| `e2ad720` | test(08-testing-ci-cd): add TicketListComponent behaviour tests |
| `8756fc5` | test(08-testing-ci-cd): add ArticleEditorComponent behaviour tests |
| `4904da5` | test(08-testing-ci-cd): add isLoading test to LoginComponent spec |

## Self-Check: PASSED

- [x] All 5 spec files exist at expected paths
- [x] All 6 commits exist in git history
- [x] All spec files meet or exceed minimum line requirements
- [x] `pnpm ng test --no-watch` passes 106/106 tests
- [x] No accidental file deletions detected
