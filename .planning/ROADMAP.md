# Roadmap: Shift-Left Knowledge Hub

**8 phases** | **36 v1 + 5 v2 requirements** | All v1 covered ✓

## Phase Overview

| # | Phase | Goal | Requirements | Success Criteria |
|--|-------|------|--------------|------------------|
| 1 | Foundation | 4/4 | Complete  | 2026-05-31 |
| 2 | Knowledge Base | Article CRUD + FTS + tags + bilingual EN/FR | KB-01-05, ADM-04 | ✓ 4 |
| 3 | AI Self-Service Portal | Conversational chat + RAG + SSE streaming | AI-01-06, INF-04 | ✓ 4 |
| 4 | Escalation & Ticketing | Contextual ticket creation | TKT-01-04 | 3 |
| 5 | Agent Dashboard | ✓ Prioritized ticket queue + resolution + KCS flag | AGT-01-05 | 4/4 |
| 6 | KCS Auto-Drafting & Admin Review | Event-driven drafting + dedup + review queue | KCS-01-04, ADM-01-02 | 4 |
| 7 | Quality, Polish & DevOps | Static analysis, healthchecks, i18n verify, testing | (polish phase) | 7 |
| 8 | Testing & CI/CD | Test coverage pyramid + GitHub Actions CI/CD pipeline | TST-01-05 | 5 |

---

## Phase Details

### Phase 1: Foundation

**Goal:** Working authentication, Docker-based local dev environment, monorepo structure, user management

**Requirements:** AUTH-01, AUTH-02, AUTH-03, AUTH-04, ADM-03, INF-01, INF-02, INF-03

**Success criteria:**
1. ✅ Developer can run `docker compose up` and the full stack starts (PostgreSQL + pgAdmin)
2. ✅ User can sign up with email/password and log in
3. ✅ User session persists across browser refresh via refresh token rotation (HttpOnly cookies)
4. ✅ Admin can manage users and assign roles via the admin console

**Plans:** 4 plans in 3 waves (3/4 complete)

| Wave | Plans | Objective |
|------|-------|-----------|
| 1 | 01-01, 01-02 | Docker infra + Backend auth (parallel) |
| 2 | 01-03 | Admin API + Angular scaffold |
| 3 | 01-04 | Frontend auth UI + Admin console |

Plans:
- [x] 01-01 — Infrastructure: Docker Compose, backend Dockerfile, .gitignore, application config
- [x] 01-02 — Backend Auth: pom.xml, JWT auth (HttpOnly cookies + refresh rotation), User entity, RBAC, admin seeder
- [x] 01-03 — Admin API + Angular scaffold: AdminUserController, Angular CLI project, Tailwind, i18n, core services
- [x] 01-04 — Frontend Auth UI + Admin Console: Login/Register pages, user management table, language switcher, translations

---

### Phase 2: Knowledge Base

**Goal:** Full knowledge base CRUD with PostgreSQL Full-Text Search, tag management, and bilingual article support

**Requirements:** KB-01, KB-02, KB-03, KB-04, KB-05, ADM-04

**Success criteria:**
1. ✅ Admin can create, edit, and archive articles using a markdown editor
2. ✅ User can browse published articles with pagination and tag filtering
3. ✅ FTS search returns highlighted result snippets with fast response times
4. ✅ Articles display correctly in both English and French based on user preference

**Plans:**
- [x] 02-01 — Database schema: Article + Tag JPA entities, repositories, ArticleStatus enum, tsvector columns + GIN index + trigger
- [x] 02-02 — Backend services: ArticleService (CRUD with slug generation, lifecycle), TagService, admin REST APIs, public search endpoint
- [x] 02-03 — Angular admin UI: shared components (table, card, badge, modal), admin article editor, tag manager
- [x] 02-04 — Public KB: article listing, FTS search with snippets, bilingual viewer, tag filtering

---

### Phase 3: AI Self-Service Portal

**Goal:** Conversational AI interface that searches the knowledge base via hybrid search (FTS + vector + RRF) and streams step-by-step resolutions

**Requirements:** AI-01, AI-02, AI-03, AI-04, AI-05, AI-06, INF-04

**Success criteria:**
1. ✅ User can describe an IT issue in a chat interface and receive relevant step-by-step guides
2. ✅ AI responses stream in real-time with a typing indicator animation
3. ✅ System asks "Did this solve your problem?" after presenting a guide
4. ✅ When no relevant content found (below 0.65 threshold), system offers graceful fallback with escalation option

**Plans:** 4 plans in 3 waves

| Wave | Plans | Objective |
|------|-------|-----------|
| 1 | 03-01 | Backend AI Infrastructure (pom.xml, AiConfig, EmbeddingService, admin API) |
| 2 | 03-02, 03-03 | Backend Chat + RAG Pipeline (parallel with Frontend Admin Settings) |
| 3 | 03-04 | Frontend Chat UI (bubble layout, SSE, typing indicator, feedback) |

Plans:
- [x] 03-01 — Backend AI Infrastructure: Spring AI 2.0.0-M8 deps, AiConfig entity with encrypted API key, EmbeddingService (pgvector + auto-embed on publish), AiConfigController (CRUD, test connection, re-embed), security wiring
- [x] 03-02 — Backend Chat & RAG Pipeline: SSE streaming SseEmitter endpoint, hybrid search (FTS+pgvector+RRF), similarity threshold >0.65, context construction, LLM streaming, fallback flow
- [x] 03-03 — Admin LLM Settings UI: /admin/settings/llm route, provider dropdown (Ollama/OpenAI), test connection button, re-embed button, EN/FR translations
- [x] 03-04 — Chat UI: /chat route, bubble layout, typing indicator, SSE streaming, markdown rendering (ngx-markdown), "Did this solve?" feedback, fallback + escalation placeholder, inline error + retry

---

### Phase 4: Escalation & Ticketing

**Goal:** Seamless escalation from AI chat to human agent with full context preservation

**Requirements:** TKT-01, TKT-02, TKT-03, TKT-04

**Success criteria:**
1. User can escalate an unresolved issue with one click from the chat interface
2. Ticket is pre-filled with the user's issue description and full AI chat transcript
3. User selects category (NETWORK, HARDWARE, SOFTWARE, ACCESS, PERIPHERALS) and urgency
4. Ticket status correctly tracks through NEW → IN_PROGRESS → RESOLVED

**Plans:**
1. Implement ticket database schema with JSONB shift_left_context
2. Build Spring Boot ticket module: CRUD API, category/urgency enums, status workflow
3. Build Angular escalation UI: morph transition from chat to ticket form, context pre-fill
4. Implement ticket list view for users (my tickets)

---

### Phase 5: Agent Dashboard

**Goal:** Clean, prioritized workspace for IT agents to view, filter, and resolve tickets

**Requirements:** AGT-01, AGT-02, AGT-03, AGT-04, AGT-05

**Success criteria:**
1. Agent can view all tickets sorted by urgency in a clean dashboard
2. Agent can filter tickets by status, category, and urgency
3. Agent can open a ticket and see full shift-left context (what the AI tried)
4. Agent can add resolution notes, flag as Knowledge Gap, and close the ticket

**Plans:**
1. ✅ Build Angular Agent Dashboard: prioritized ticket list with filtering, server-side search
2. ✅ Build ticket detail view with shift-left context display, work notes timeline
3. ✅ Implement resolution form with Knowledge Gap checkbox
4. ✅ Implement ticket resolution API endpoint

---

### Phase 6: KCS Auto-Drafting & Admin Review

**Goal:** Close the KCS loop — automatically draft KB articles from resolved tickets flagged as Knowledge Gaps, with admin review workflow

**Requirements:** KCS-01, KCS-02, KCS-03, KCS-04, ADM-01, ADM-02

**Success criteria:**
1. Flagged tickets automatically trigger AI article drafting within seconds of resolution
2. AI-drafted articles contain meaningful content (title, problem description, solution, tags)
3. Duplicate detection prevents creating articles that already exist in the KB
4. Admin can view article drafts, edit content, and approve or reject them

**Plans:** 3 plans in 3 waves

| Wave | Plans | Objective |
|------|-------|-----------|
| 1 | 06-01 | Backend Foundation — sourceTicketId, @EnableAsync, TicketResolvedEvent, event wiring |
| 2 | 06-02 | KCS Drafting Engine + Admin API — AI synthesis, dedup, async listener, controller |
| 3 | 06-03 | Frontend Admin KCS UI — draft queue table, approve/reject, nav badge, routes |

Plans:
- [x] 06-01 — Backend Foundation: Article entity sourceTicketId field, AsyncConfig, TicketResolvedEvent event record, AgentTicketService event publishing
- [x] 06-02 — KCS Core Backend: KcsDraftingService (AI synthesis + dedup + article creation), KcsEventListener (async listener with 3x retry), AdminKcsController (list/approve/reject/pending-count), work note auto-generation
- [x] 06-03 — Frontend Admin UI: KcsDraftListComponent with table + approve/reject actions, KcsDraftService, nav link with pending count badge, route wiring, EN/FR translations

---

### Phase 7: Quality, Polish & DevOps

**Goal:** Production-ready polish — healthchecks, SPA routing, comprehensive testing, edge case handling

**Requirements:** (polish — no new requirements)

**Success criteria:**
1. Docker Compose healthchecks ensure correct startup ordering (PostgreSQL ready → Spring Boot ready → frontend serves)
2. SPA deep-linking works correctly (browser refresh on any route returns the same page)
3. All text renders correctly in both English and French with no layout overflow
4. Core user flows work end-to-end with proper error handling

**Plans:**
1. Docker Compose healthcheck configuration with proper depends_on conditions
2. SPA deep-linking fix: Spring Boot forwards all non-API routes to index.html
3. Bilingual layout audit: verify French text expansion doesn't break layouts
4. Error boundaries and loading states for all API calls
5. E2E testing with Playwright for core flows: login → search article → chat → escalate → resolve
6. Final demo walkthrough script

---

### Phase 8: Testing & CI/CD

**Goal:** Production-quality test coverage + automated CI/CD pipeline that gates every merge.

**Requirements:** TST-01, TST-02, TST-03, TST-04, TST-05

**Success criteria:**
1. All backend service tests pass with `mvn test` (unit + integration)
2. All frontend tests pass with `npm test -- --watch=false`
3. Playwright E2E golden path runs against the live Docker stack
4. GitHub Actions CI runs lint → test → build on every PR to master
5. Railway/Vercel deploy gates on CI passing

**Plans:** 8 plans in 3 waves

| Wave | Plans | Objective |
|------|-------|-----------|
| 1 | 08-01, 08-02, 08-03 | Backend: Testcontainers base, Service layer unit tests, Integration tests |
| 2 | 08-04, 08-05 | Frontend: Service tests, Smart component tests |
| 3 | 08-06, 08-07, 08-08 | E2E Playwright, GitHub Actions CI, Pre-commit hooks |

Plans:
- [ ] 08-01 — Test Dependencies & Base Configuration: Testcontainers, `application-test.properties`, `AbstractIntegrationTest`
- [ ] 08-02 — Backend Service-Layer Unit Tests: AuthService, TicketService, AgentTicketService, ArticleService, KcsDraftingService, TagService
- [ ] 08-03 — Backend Integration Tests (Testcontainers): Auth flow, Ticket CRUD, KB search, Agent resolve, KCS draft
- [ ] 08-04 — Frontend Service & RxJS Tests: AuthService, TicketService, ChatService, ArticleService, KcsDraftService, TranslationService
- [ ] 08-05 — Frontend Smart Component Tests: LoginComponent, ChatComponent, ArticleSearchComponent, TicketListComponent, ArticleEditorComponent
- [ ] 08-06 — Playwright E2E Golden Path: login → AI query → escalate → agent resolves → flag KCS gap
- [ ] 08-07 — GitHub Actions CI: `ci.yml` with backend, frontend, E2E jobs; Railway/Vercel deploy gating
- [ ] 08-08 — Pre-commit Hooks: Husky + lint-staged for Prettier + ESLint on staged files

---

## Phase Dependency Graph

```
Phase 1 (Foundation)
    └──► Phase 2 (Knowledge Base)
            └──► Phase 3 (AI Portal)
                    └──► Phase 4 (Escalation)
                            └──► Phase 5 (Agent Dashboard)
                                    └──► Phase 6 (KCS Drafting)
                                            └──► Phase 7 (Polish)
                                                    └──► Phase 8 (Testing & CI/CD)
```

Each phase depends on all previous phases. Strict sequential execution.

## Coverage Validation

- v1 requirements: 36 total
- v2 requirements: 5 (TST-01 through TST-05)
- Mapped to phases: 41 (36 v1 + 5 v2)
- Unmapped: 0 ✓

---

*Created: 2026-05-31*
*Last updated: 2026-06-08 — Phase 8 added*
*Next: `/gsd-plan-phase 8`*
