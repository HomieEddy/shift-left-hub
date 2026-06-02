# Roadmap: Shift-Left Knowledge Hub

**7 phases** | **36 requirements** | All v1 covered ✓

## Phase Overview

| # | Phase | Goal | Requirements | Success Criteria |
|--|-------|------|--------------|------------------|
| 1 | Foundation | 3/4 | Executing  | 2026-05-31 |
| 2 | Knowledge Base | Article CRUD + FTS + tags + bilingual EN/FR | KB-01-05, ADM-04 | 4 |
| 3 | AI Self-Service Portal | Conversational chat + RAG + SSE streaming | AI-01-06, INF-04 | 4 |
| 4 | Escalation & Ticketing | Contextual ticket creation | TKT-01-04 | 3 |
| 5 | Agent Dashboard | Prioritized ticket queue + resolution + KCS flag | AGT-01-05 | 4 |
| 6 | KCS Auto-Drafting & Admin Review | Event-driven drafting + dedup + review queue | KCS-01-04, ADM-01-02 | 4 |
| 7 | Quality, Polish & DevOps | Healthchecks + i18n verify + testing | (polish phase) | 3 |

---

## Phase Details

### Phase 1: Foundation

**Goal:** Working authentication, Docker-based local dev environment, monorepo structure, user management

**Requirements:** AUTH-01, AUTH-02, AUTH-03, AUTH-04, ADM-03, INF-01, INF-02, INF-03

**Success criteria:**
1. Developer can run `docker compose up` and the full stack starts (PostgreSQL + pgAdmin)
2. User can sign up with email/password and log in
3. User session persists across browser refresh via refresh token rotation (HttpOnly cookies)
4. Admin can manage users and assign roles via the admin console

**Plans:** 4 plans in 3 waves (3/4 complete)

| Wave | Plans | Objective |
|------|-------|-----------|
| 1 | 01-01, 01-02 | Docker infra + Backend auth (parallel) |
| 2 | 01-03 | Admin API + Angular scaffold |
| 3 | 01-04 | Frontend auth UI + Admin console |

Plans:
- [ ] 01-01 — Infrastructure: Docker Compose, backend Dockerfile, .gitignore, application config
- [ ] 01-02 — Backend Auth: pom.xml, JWT auth (HttpOnly cookies + refresh rotation), User entity, RBAC, admin seeder
- [x] 01-03 — Admin API + Angular scaffold: AdminUserController, Angular CLI project, Tailwind, i18n, core services
- [x] 01-04 — Frontend Auth UI + Admin Console: Login/Register pages, user management table, language switcher, translations

---

### Phase 2: Knowledge Base

**Goal:** Full knowledge base CRUD with PostgreSQL Full-Text Search, tag management, and bilingual article support

**Requirements:** KB-01, KB-02, KB-03, KB-04, KB-05, ADM-04

**Success criteria:**
1. Admin can create, edit, and archive articles using a markdown editor
2. User can browse published articles with pagination and tag filtering
3. FTS search returns highlighted result snippets with fast response times
4. Articles display correctly in both English and French based on user preference

**Plans:**
- [x] 02-01 — Database schema: Article + Tag JPA entities, repositories, ArticleStatus enum, tsvector columns + GIN index + trigger
- [x] 02-02 — Backend services: ArticleService (CRUD with slug generation, lifecycle), TagService, admin REST APIs, public search endpoint
- [x] 02-03 — Angular admin UI: shared components (table, card, badge, modal), admin article editor, tag manager
- [ ] 02-04 — Public KB: article listing, FTS search with snippets, bilingual viewer, tag filtering

---

### Phase 3: AI Self-Service Portal

**Goal:** Conversational AI interface that searches the knowledge base via hybrid search (FTS + vector + RRF) and streams step-by-step resolutions

**Requirements:** AI-01, AI-02, AI-03, AI-04, AI-05, AI-06, INF-04

**Success criteria:**
1. User can describe an IT issue in a chat interface and receive relevant step-by-step guides
2. AI responses stream in real-time with a typing indicator animation
3. System asks "Did this solve your problem?" after presenting a guide
4. When no relevant content found (below 0.65 threshold), system offers graceful fallback with escalation option

**Plans:**
1. Implement Spring AI RAG pipeline: FTS + pgvector hybrid search, context construction, similarity threshold
2. Build streaming SSE endpoint with Spring Boot
3. Build Angular chat UI: message list, input, typing indicator, markdown rendering
4. Integrate Ollama for local LLM fallback (no API key required for demo)
5. Handle "Did this solve?" flow and graceful fallback UI

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
1. Build Angular Agent Dashboard: prioritized ticket list with filtering, list animation reordering
2. Build ticket detail view with shift-left context display
3. Implement resolution form with Knowledge Gap checkbox
4. Implement ticket resolution API endpoint

---

### Phase 6: KCS Auto-Drafting & Admin Review

**Goal:** Close the KCS loop — automatically draft KB articles from resolved tickets flagged as Knowledge Gaps, with admin review workflow

**Requirements:** KCS-01, KCS-02, KCS-03, KCS-04, ADM-01, ADM-02

**Success criteria:**
1. Flagged tickets automatically trigger AI article drafting within seconds of resolution
2. AI-drafted articles contain meaningful content (title, problem description, solution, tags)
3. Duplicate detection prevents creating articles that already exist in the KB
4. Admin can view article drafts, edit content, and approve or reject them

**Plans:**
1. Implement event-driven KCS pipeline with Spring ApplicationEventPublisher
2. Build AI synthesis service: ticket timeline → LLM prompt → markdown draft → dedup check
3. Build Angular admin draft queue: pending drafts list with source ticket link
4. Build article editor UI for reviewing/editing/publishing drafts

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

## Phase Dependency Graph

```
Phase 1 (Foundation)
    └──► Phase 2 (Knowledge Base)
            └──► Phase 3 (AI Portal)
                    └──► Phase 4 (Escalation)
                            └──► Phase 5 (Agent Dashboard)
                                    └──► Phase 6 (KCS Drafting)
                                            └──► Phase 7 (Polish)
```

Each phase depends on all previous phases. Strict sequential execution.

## Coverage Validation

- v1 requirements: 36 total
- Mapped to phases: 36
- Unmapped: 0 ✓

---
*Created: 2026-05-31*
*Next: `/gsd-plan-phase 1`*
