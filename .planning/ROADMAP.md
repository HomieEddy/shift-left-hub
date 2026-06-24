# Roadmap: Shift-Left Knowledge Hub

## Milestones

- ✅ **v1.0 Initial MVP** — Phases 1-8 (shipped 2026-06-08)
- ✅ **v2.0 Workspace Platform** — Phases 9-16 (shipped 2026-06-14)
- ✅ **v2.1 Deployment** — Phases 17-21 (shipped 2026-06-15)
- ✅ **v2.2 Post-Cleanup Polish** — Tier 12-18 (shipped 2026-06-23)
- 🚧 **v2.3 Deferred Items** — Phases 22-25 (in progress)

## Phases

<details>
<summary>✅ v1.0 Initial MVP (Phases 1-8) — SHIPPED 2026-06-08</summary>

- [x] Phase 1: Foundation (4 plans) — completed 2026-06-02
- [x] Phase 2: Knowledge Base (4 plans) — completed 2026-06-03
- [x] Phase 3: AI Self-Service Portal (4 plans) — completed 2026-06-04
- [x] Phase 4: Escalation & Ticketing (4 plans) — completed 2026-06-05
- [x] Phase 5: Agent Dashboard (4 plans) — completed 2026-06-06
- [x] Phase 6: KCS Auto-Drafting & Admin Review (3 plans) — completed 2026-06-07
- [x] Phase 7: Quality, Polish & DevOps (9 plans) — completed 2026-06-08
- [x] Phase 8: Testing & CI/CD (8 plans) — completed 2026-06-08

</details>

<details>
<summary>✅ v2.0 Workspace Platform (Phases 9-16) — SHIPPED 2026-06-14</summary>

- [x] Phase 9: Workspace Foundation (4 plans) — completed 2026-06-10
- [x] Phase 10: Document Ingestion + BYO LLM (4 plans) — completed 2026-06-10
- [x] Phase 11: Domain-Agnostic AI (6 plans) — completed 2026-06-12
- [x] Phase 12: Workspace Management UI (5 plans) — completed 2026-06-12
- [x] Phase 13: Frontend Cleanup (4 plans) — completed 2026-06-12
- [x] Phase 14: Seeding Revamp (3 plans) — completed 2026-06-13
- [x] Phase 15: File Upload Format Support (2 plans) — completed 2026-06-13
- [x] Phase 16: UI Neutralization (4 plans) — completed 2026-06-14

</details>

### 🚧 v2.1 Deployment (In Progress)

**Milestone Goal:** Harden and deploy the v2.0 Workspace Platform to production through codebase review, test tightening, security audit, and production deployment.

- [x] **Phase 17: Codebase Review** — Full codebase-wide review of frontend, backend, and database
- [x] **Phase 18: Unit Test Tightening** — Backend and frontend unit tests expanded with meaningful coverage
- [x] **Phase 19: E2E Test Coverage** — 8 Playwright e2e happy-path tests covering all user-facing features
- [x] **Phase 20: Security Audit & Hardening** — Full security audit of backend, frontend, and infrastructure
- [x] **Phase 21: Production Deployment** — Deploy frontend (Vercel), backend (Railway), and verify live

### 🚧 v2.3 Deferred Items (In Progress)

**Milestone Goal:** Address the four open follow-ups from v2.2 — OnPush change detection migration (42 components), TagService pagination, SpringDoc/OpenAPI documentation, and bundle size optimization.

- [ ] **Phase 22: OnPush Migration** — Migrate all 42 Angular components to `ChangeDetectionStrategy.OnPush` for performance
- [ ] **Phase 23: TagService Pagination** — Add backend pagination + frontend `<app-pagination>` to tag manager
- [ ] **Phase 24: SpringDoc/OpenAPI** — Add auto-generated OpenAPI docs at `/swagger-ui.html`
- [ ] **Phase 25: Bundle Size Optimization** — Reduce initial bundle from 560KB to under 500KB budget

## Phase Details

### Phase 17: Codebase Review
**Goal**: Codebase-wide review identifies and resolves quality issues across frontend TypeScript/Angular, backend Java/Spring Boot, and database schema
**Depends on**: Phase 16
**Requirements**: REV-01, REV-02, REV-03, REV-04
**Success Criteria** (what must be TRUE):
   1. Frontend TypeScript/Angular code has zero lint errors, no type safety issues, no dead code, and no unused imports
   2. Backend Java/Spring Boot code has zero lint errors, no type safety issues, no dead code, and no unused imports
   3. Database schema has no missing indexes, no unused migrations, and normalization is correct
   4. All issues found during review are either fixed or documented as tech debt with acceptance rationale
**Plans**: 3 plans

Plans:
- [x] 17-01 — Frontend Codebase Review ✓ `pnpm lint clean, build clean, 127 tests pass`
- [x] 17-02 — Backend Codebase Review ✓ `Checkstyle 0, SpotBugs 0, 246 tests pass`
- [x] 17-03 — Database Schema Review ✓ `V8 migration, REV-04 audit complete`

**Completed:** 2026-06-14

### Phase 18: Unit Test Tightening
**Goal**: Backend and frontend unit tests are tightened with meaningful, business-logic-focused coverage
**Depends on**: Phase 17
**Requirements**: TST-06, TST-07
**Success Criteria** (what must be TRUE):
  1. Backend service-layer unit tests are expanded beyond 9 with meaningful test cases covering core business logic
  2. Frontend smart component and service unit tests are expanded with meaningful coverage (not trivial/trivial tests)
  3. `mvn test` passes — all backend unit + integration tests pass consistently
  4. `npm run test -- --watch=false` passes — all frontend unit tests pass consistently
**Plans**: 5 plans

Plans:
- [x] 18-01 — Backend test deepening: 12 files deepened with 33 new edge-case tests ✓ `mvn test: 339 pass`
- [x] 18-02 — Backend new test files: 6 new test files with 60 tests ✓ `mvn test: 339 pass`
- [x] 18-03 — Frontend agent + landing tests: 3 spec files, 36 tests ✓ `ng test: 229 pass`
- [x] 18-04 — Frontend remaining tests: 8 spec files, 65 tests ✓ `ng test: 229 pass`
- [x] 18-05 — JaCoCo thresholds 40→60%/30→50% + verification ✓ `mvn verify: BUILD SUCCESS`

### Phase 19: E2E Test Coverage
**Goal**: All 8 user-facing features are covered by Playwright e2e happy-path tests
**Depends on**: Phase 18
**Requirements**: TST-08, TST-09, TST-10, TST-11, TST-12, TST-13, TST-14, TST-15, TST-16
**Success Criteria** (what must be TRUE):
   1. Playwright e2e covers auth happy path: register, login, logout, protected route redirect
   2. Playwright e2e covers knowledge base, AI self-service, escalation, agent dashboard, workspace management, admin, and document ingestion happy paths
   3. All 8 Playwright e2e tests pass consistently across multiple runs
   4. All three test suites pass consistently: backend (`mvn test`), frontend (`npm run test -- --watch=false`), and e2e (Playwright)
**Plans**: 6 plans

Plans:
- [x] 19-01 — Infrastructure: 3 new page objects + e2e/fixtures ✓ `8 files, 231 insertions`
- [x] 19-02 — Auth (TST-08) + KB (TST-09) specs + template testids ✓ `5 files, 118 insertions`
- [x] 19-03 — AI Chat (TST-10) + Escalation (TST-11) specs + chat testids ✓ `3 files, 115 insertions`
- [x] 19-04 — Agent Dashboard (TST-12) + Workspace Mgmt (TST-13) specs + switcher testids ✓ `3 files, 73 insertions`
- [x] 19-05 — Admin (TST-14) + Document Ingestion (TST-15) specs + admin template testids ✓ `7 files, 190 insertions`
- [x] 19-06 — Cleanup + Verification (TST-16): delete golden-path ✓ `142 deletions`

**Completed:** 2026-06-14

### Phase 20: Security Audit & Hardening
**Goal**: Application is audited for security vulnerabilities across backend, frontend, and infrastructure, with all findings resolved
**Depends on**: Phase 19
**Requirements**: SEC-01, SEC-02, SEC-03, SEC-04
**Success Criteria** (what must be TRUE):
   1. Backend security audit covers JWT authentication, RBAC authorization, injection vulnerabilities (SQL, NoSQL, command), secrets exposure, and dependency vulnerabilities
   2. Frontend security audit covers XSS, CSRF, secure cookie handling, and dependency vulnerabilities
   3. Infrastructure security audit covers CORS headers, CSP headers, HTTPS enforcement, and environment variable management
   4. All discovered vulnerabilities are fixed or documented with risk acceptance rationale
**Plans**: 3 plans

Plans:
- [x] 20-01 — Backend Security Audit ✓ `OWASP + SpotBugs findsecbugs, cookie hardening, rate limiting, CR-02 fix, env var salt`
- [x] 20-02 — Frontend Security Audit ✓ `ESLint security plugin, DOMPurify XSS fix, 0 lint errors`
- [x] 20-03 — Infrastructure Hardening ✓ `Docker non-root, CSP meta tag, CI dep scanning, .env.example`

**Completed:** 2026-06-15

### Phase 21: Production Deployment
**Goal**: Application is deployed to production on Vercel (frontend) and Railway (backend/database) with correct Docker configuration and passing health checks
**Depends on**: Phase 20
**Requirements**: DEP-01, DEP-02, DEP-03, DEP-04, DEP-05
**Success Criteria** (what must be TRUE):
   1. Frontend Angular SPA is deployed and accessible on Vercel with default domain
   2. Backend Spring Boot API + PostgreSQL database are deployed and accessible on Railway with default domain
   3. Production Docker configuration uses multi-stage builds, minimal images, and excludes dev dependencies
   4. CI/CD pipeline verifies build, test, lint, and security steps before deploying
   5. Deployed application passes health checks and smoke tests (login → AI query → escalate → ticket)
**Plans**: 3 plans

Plans:
- [x] 21-01 — Pre-deploy code changes: env.js runtime config, base URL interceptor, Dockerfile/CI verification, pre-deploy checklist
- [x] 21-02 — Pre-deploy setup: create Railway project + PostgreSQL + environment variables, create Vercel project
- [x] 21-03 — Deploy & smoke test: set production Railway URL, update demo walkthrough, push to main, manual smoke test

**Completed:** 2026-06-15

### Phase 22: OnPush Migration
**Goal**: Migrate all 42 Angular components from `Default` to `ChangeDetectionStrategy.OnPush` to reduce change detection cycles and improve runtime performance.
**Depends on**: Phase 21
**Requirements**: PERF-01, PERF-02
**Success Criteria** (what must be TRUE):
   1. All 42 component files declare `changeDetection: ChangeDetectionStrategy.OnPush` in their `@Component` decorator
   2. Components with imperative `.subscribe()` assignments convert to `signal()` patterns or use `markForCheck()` to remain OnPush-compatible
   3. `pnpm test -- --watch=false` passes — all 306 frontend tests still green
   4. `pnpm build` produces a clean production build with no template/type errors
   5. All 8 Playwright e2e tests pass against the OnPush-migrated build
**Plans**: 5 plans

Plans:
- [ ] 22-01 — Dumb + shared UI components (14 components: button, icon-button, card, badge, table, pagination, icon-picker, skeleton×3, search-input, confirmation-dialog, toast-container, modal)
- [ ] 22-02 — Auth, Landing, Workspace Switcher, Chat, App (7 components: login, register, landing, workspace-switcher, invitation-badge, chat, app)
- [ ] 22-03 — Tickets + Agent + KB (12 components: ticket-list, ticket-detail, escalation-form, agent-ticket-list, agent-ticket-detail, article-list×2, article-search, article-viewer, article-editor, tag-manager, kb-admin-article-list)
- [ ] 22-04 — Admin (9 components: user-list, kcs-draft-list, document-list, taxonomy-tree, taxonomy-bulk, llm-settings, workspace-list, workspace-detail, workspace-settings, workspace-members)
- [ ] 22-05 — Verification: run all 306 frontend tests + Playwright E2E smoke test

### Phase 23: TagService Pagination
**Goal**: Add Spring Data pagination to the tag backend and wire the existing `<app-pagination>` component into the tag manager UI so the tag list scales beyond a single page.
**Depends on**: Phase 22
**Requirements**: TAG-01, TAG-02
**Success Criteria** (what must be TRUE):
   1. `GET /api/admin/tags?page=N&size=M` returns a Spring `Page<TagResponse>` (no more `List<TagResponse>`)
   2. `TagService.getAllTags(page, size)` returns `Page<TagResponse>` with article counts preserved
   3. `TagManagerComponent` displays pagination controls when `totalPages > 1` and fetches the next page on click
   4. Backend integration test verifies pagination parameters are honored
   5. `mvn test` and `pnpm test -- --watch=false` both pass cleanly
**Plans**: 3 plans

Plans:
- [ ] 23-01 — Backend pagination: `TagRepository.findAll(Pageable)`, `TagService.getAllTags(page, size) → Page<TagResponse>`, `AdminTagController` accepts `@RequestParam page, size`
- [ ] 23-02 — Frontend pagination: `PaginatedResponse<TagDto>`, `tag.service.getTags(page, size)`, `TagManagerComponent` with `currentPage`/`totalPages` signals and `<app-pagination>`
- [ ] 23-03 — Verify: 2 backend tests + 1 frontend spec added, full test suite green, manual smoke test with 25+ tags

### Phase 24: SpringDoc/OpenAPI
**Goal**: Add `springdoc-openapi-starter-webmvc-ui:3.0.3` to auto-generate OpenAPI documentation accessible at `/swagger-ui.html` for the 17 backend controllers.
**Depends on**: Phase 23
**Requirements**: DOC-01, DOC-02
**Success Criteria** (what must be TRUE):
   1. `springdoc-openapi-starter-webmvc-ui:3.0.3` is added to `pom.xml`
   2. `/swagger-ui.html` and `/v3/api-docs` are accessible without authentication (permitted in SecurityConfig)
   3. All 17 controllers appear in the OpenAPI spec with HTTP methods, paths, request/response schemas
   4. `mvn test` passes
   5. `mvn compile` clean (no Spring Boot 4.x compatibility issues)
**Plans**: 1 plan

Plans:
- [ ] 24-01 — Single plan: add dependency, configure application.properties, update SecurityConfig, verify

### Phase 25: Bundle Size Optimization
**Goal**: Reduce the production initial bundle from 560KB to under the 500KB warning budget through targeted eager-dependency fixes.
**Depends on**: Phase 24
**Requirements**: PERF-03, PERF-04, PERF-05
**Success Criteria** (what must be TRUE):
   1. `ngx-markdown` is no longer in the main bundle — only loaded for article viewer/editor routes
   2. `translations.ts` (61KB) is split — FR translations lazy-loaded separately
   3. `WorkspaceSwitcherComponent`, `InvitationBadgeComponent`, and sidebar Lucide icons are wrapped in `@defer` blocks (render after auth check)
   4. `KcsDraftService` is conditionally injected only for admin users
   5. `pnpm build` produces an initial bundle under 500KB (the warning budget)
**Plans**: 3 plans

Plans:
- [ ] 25-01 — Lazy `ngx-markdown` + split translations
- [ ] 25-02 — `@defer` workspace components + sidebar icons
- [ ] 25-03 — Lazy `KcsDraftService` + verify bundle under 500KB

## Progress

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Foundation | v1.0 | 4/4 | Complete | 2026-05-31 |
| 2. Knowledge Base | v1.0 | 4/4 | Complete | 2026-06-01 |
| 3. AI Self-Service Portal | v1.0 | 4/4 | Complete | 2026-06-02 |
| 4. Escalation & Ticketing | v1.0 | 4/4 | Complete | 2026-06-03 |
| 5. Agent Dashboard | v1.0 | 4/4 | Complete | 2026-06-04 |
| 6. KCS Auto-Drafting & Admin Review | v1.0 | 3/3 | Complete | 2026-06-05 |
| 7. Quality, Polish & DevOps | v1.0 | 9/9 | Complete | 2026-06-06 |
| 8. Testing & CI/CD | v1.0 | 8/8 | Complete | 2026-06-08 |
| 9. Workspace Foundation | v2.0 | 4/4 | Complete | 2026-06-10 |
| 10. Document Ingestion + BYO LLM | v2.0 | 4/4 | Complete | 2026-06-10 |
| 11. Domain-Agnostic AI | v2.0 | 6/6 | Complete | 2026-06-12 |
| 12. Workspace Management UI | v2.0 | 5/5 | Complete | 2026-06-12 |
| 13. Frontend Cleanup | v2.0 | 4/4 | Complete | 2026-06-12 |
| 14. Seeding Revamp | v2.0 | 3/3 | Complete | 2026-06-13 |
| 15. File Upload Format Support | v2.0 | 2/2 | Complete | 2026-06-13 |
| 16. UI Neutralization | v2.0 | 4/4 | Complete | 2026-06-14 |
| 17. Codebase Review | v2.1 | 3/3 | Complete | 2026-06-14 |
| 18. Unit Test Tightening | v2.1 | 5/5 | Complete | 2026-06-14 |
| 19. E2E Test Coverage | v2.1 | 6/6 | Complete | 2026-06-14 |
| 20. Security Audit & Hardening | v2.1 | 3/3 | Complete | 2026-06-15 |
| 21. Production Deployment | v2.1 | 3/3 | Complete | 2026-06-15 |
| 22. OnPush Migration | v2.3 | 0/5 | Planned | - |
| 23. TagService Pagination | v2.3 | 0/3 | Planned | - |
| 24. SpringDoc/OpenAPI | v2.3 | 0/1 | Planned | - |
| 25. Bundle Size Optimization | v2.3 | 0/3 | Planned | - |

---

*Last updated: 2026-06-23 — v2.3 Deferred Items: Phases 22-25 planned*
