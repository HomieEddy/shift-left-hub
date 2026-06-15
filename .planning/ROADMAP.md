# Roadmap: Shift-Left Knowledge Hub

## Milestones

- ✅ **v1.0 Initial MVP** — Phases 1-8 (shipped 2026-06-08)
- ✅ **v2.0 Workspace Platform** — Phases 9-16 (shipped 2026-06-14)
- 🚧 **v2.1 Deployment** — Phases 17-21 (in progress)

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
- [ ] **Phase 20: Security Audit & Hardening** — Full security audit of backend, frontend, and infrastructure
- [ ] **Phase 21: Production Deployment** — Deploy frontend (Vercel), backend (Railway), and verify live

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
- [ ] 20-01 — Backend Security Audit & Hardening (SEC-01, SEC-04) — OWASP Dependency-Check, SpotBugs find-sec-bugs, JWT/RBAC/injection/secrets audit, rate limiting, CR-01/CR-02 fixes, cookie hardening
- [ ] 20-02 — Frontend Security Audit & Hardening (SEC-02, SEC-04) — ESLint security plugin, XSS audit, DomSanitizer bypass fix with DOMPurify, template security audit
- [ ] 20-03 — Infrastructure Security Hardening (SEC-03, SEC-04) — Docker non-root USER, credential extraction, CSP meta tag, security headers, CI dependency scanning, .env.example

### Phase 21: Production Deployment
**Goal**: Application is deployed to production on Vercel (frontend) and Railway (backend/database) with correct Docker configuration and passing health checks
**Depends on**: Phase 20
**Requirements**: DEP-01, DEP-02, DEP-03, DEP-04, DEP-05
**Success Criteria** (what must be TRUE):
  1. Frontend Angular SPA is deployed and accessible on Vercel with custom domain
  2. Backend Spring Boot API + PostgreSQL database are deployed and accessible on Railway with custom domain
  3. Production Docker configuration uses multi-stage builds, minimal images, and excludes dev dependencies
  4. CI/CD pipeline verifies build, test, lint, and security steps before deploying
  5. Deployed application passes health checks and smoke tests (login → AI query → escalate → ticket)
**Plans**: TBD

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
| 20. Security Audit & Hardening | v2.1 | 0/3 | Planning (3 plans created) | - |
| 21. Production Deployment | v2.1 | 0/0 | Not started | - |

---

*Last updated: 2026-06-14 — v2.1 Deployment: Phase 19 complete*
