# Shift-Left Knowledge Hub

## What This Is

A plug-and-play knowledge platform that turns any collection of documents into an intelligent, AI-powered assistant. Workspaces can bring their own knowledge base (markdown, plain text, PDF, HTML, XML, Word) and their own LLM (any OpenAI-compatible endpoint) — making it domain-agnostic. IT helpdesk, HR policy lookup, product documentation, legal research: the KB defines the domain.

v2.0 shipped: multi-tenant workspaces, document ingestion, BYO LLM, domain-agnostic assistant.
v2.0 Extended: file upload format support (HTML, XML, Word), UI neutralization to domain-agnostic branding.

## Core Value

Shift resolution as close to the user as possible by intercepting Level 0/1 issues before they reach the queue, while simultaneously eliminating the documentation burden on agents.

## Current Milestone: v2.3 Deferred Items

**Goal:** Address the four open follow-ups from v2.2 (Post-Cleanup Polish) — OnPush change detection migration across 42 components, TagService pagination, SpringDoc/OpenAPI documentation, and bundle size optimization to bring the production build under the 500KB warning budget.

**Target features:**
- Phase 22: Migrate all 42 Angular components to `ChangeDetectionStrategy.OnPush`
- Phase 23: Add Spring Data pagination to `TagService` and wire `<app-pagination>` into the tag manager UI
- Phase 24: Add `springdoc-openapi-starter-webmvc-ui:3.0.3` for auto-generated API docs at `/swagger-ui.html`
- Phase 25: Reduce the initial production bundle from 560KB to under 500KB through targeted eager-dependency fixes (lazy `ngx-markdown`, split translations, `@defer` workspace switcher, lazy `KcsDraftService`)

## Current State

**v2.2 Post-Cleanup Polish** — Shipped 2026-06-23

The application has been hardened and the v2.0 platform was deployed to production (Vercel + Railway) during v2.1. v2.2 added a final cleanup pass:

- **v2.2 PRs #126-#131** (6 PRs, 469 backend + 306 frontend tests, all green)
- **Tier 12**: DB/JPA hygiene — LAZY fetch + missing `@Index` + 2 unique constraints
- **Tier 13**: Domain exception hygiene — 3 new typed exceptions, 10 sites replaced
- **Tier 15**: 4 real frontend bugs fixed + auth signal hardening
- **Tier 16/17**: Routing (`canMatch`), modal a11y, commitlint, Prometheus
- **Tier 14/18**: Polish — `AiDefaults`, `SelfModificationException`, N+1 fix
- **Tier 17.5/17.6**: Infrastructure — `LICENSE`, `CONTRIBUTING.md`, `SECURITY.md`, editorconfig, PR template

**Open follow-ups from v2.2 → addressed in v2.3:**
- OnPush migration (42 components)
- TagService pagination
- SpringDoc/OpenAPI (blocker reason was stale)
- Bundle size optimization (560KB → <500KB)

## Requirements

### Validated

- ✓ AUTH-01–04: Full JWT auth with refresh rotation + RBAC — v1.0
- ✓ KB-01–05: Bilingual article CRUD + FTS + tags — v1.0
- ✓ AI-01–06: Conversational AI with hybrid search + SSE streaming — v1.0
- ✓ TKT-01–04: Escalation with context-preserving tickets — v1.0
- ✓ AGT-01–05: Agent dashboard with resolution + KCS flagging — v1.0
- ✓ KCS-01–04: Event-driven AI drafting with dedup — v1.0
- ✓ ADM-01–04: Admin user/tag/draft management — v1.0
- ✓ INF-01–04: Docker Compose stack + Ollama fallback — v1.0
- ✓ TST-01–05: Test coverage + CI/CD pipeline — v1.0
- ✓ WSF-01–06: Multi-tenant workspace isolation — v2.0
- ✓ DOC-01–05 + LLM-01–05: Document ingestion + BYO LLM — v2.0
- ✓ DOM-01–05: Domain-agnostic AI with taxonomy — v2.0
- ✓ WSM-01–05: Workspace management UI — v2.0
- ✓ FEC-01–04: Frontend Angular Style Guide compliance — v2.0
- ✓ SEED-01–06: Workspace-aware seeding — v2.0
- ✓ FUF-01–05: HTML/XML/Word file upload — v2.0
- ✓ UIN-01–05: Domain-agnostic UI/branding — v2.0
- ✓ DEP-01–05: Production deployment (Vercel + Railway) — v2.1
- ✓ TIER-12/13/14/15/16/17/18: Post-cleanup tiers — v2.2

### Active (v2.3 Deferred Items)

- **PERF-01**: All 42 Angular components use `ChangeDetectionStrategy.OnPush`
- **PERF-02**: Components with imperative `.subscribe()` patterns converted to `signal()` or use `markForCheck()`
- **TAG-01**: `TagService.getAllTags()` returns `Page<TagResponse>` (Spring Data pagination)
- **TAG-02**: `TagManagerComponent` displays pagination controls via `<app-pagination>`
- **DOC-01**: `springdoc-openapi-starter-webmvc-ui:3.0.3` added as a dependency
- **DOC-02**: `/swagger-ui.html` and `/v3/api-docs` accessible without authentication
- **PERF-03**: `ngx-markdown` is lazy-loaded (no longer in main bundle)
- **PERF-04**: `translations.ts` is split — FR translations lazy-loaded
- **PERF-05**: Initial production bundle < 500KB (currently 560KB)

### Out of Scope

- External search engines (Elasticsearch) — PostgreSQL FTS + pgvector handles all search needs
- NoSQL databases — Postgres-native schema with JSONB, arrays, and TSVECTOR
- Email notifications — not core to KCS loop
- SLA management — enterprise feature, no portfolio demo value
- Real-time agent chat — destroys async workflow benefits, requires staffing
- Mobile native app — responsive SPA is sufficient
- SSO/OAuth — setup complexity not justified for portfolio demo
- Schema-per-tenant isolation — breaks Flyway, Testcontainers, Spring AI pgvector; row-level workspace_id sufficient
- OCR for scanned PDFs — requires Tesseract; text-based PDFs cover 80%+ of use cases
- Multi-provider model routing — users route via OpenRouter/Portkey if needed
- Nested sub-workspaces — flat workspace model with labels sufficient
- Public auto-join workspaces — invite-only for security

## Context

- **Domain:** Knowledge Platform / Multi-Tenant AI Assistant
- **Architecture:** Modular Monolith — Angular 21.2 SPA + Spring Boot 3.5 REST API + PostgreSQL 16 with pgvector
- **AI Pipeline:** RAG with hybrid search (FTS + pgvector + RRF); configurable OpenAI-compatible provider
- **Bilingual:** English/French with dynamic layout handling
- **Team:** Solo developer — all decisions prioritize demonstrable functionality
- **v1.0 shipped:** 8 phases, 60 plans, 41 requirements
- **v2.0 shipped:** 8 phases, 34 plans, 46 requirements
- **Codebase:** ~9,500 frontend LOC + ~6,200 backend main LOC + ~2,600 test LOC
- **Coverage:** 112 backend integration tests (Testcontainers) + 127 frontend tests + Playwright E2E

## Constraints

- **Stack**: Angular + Spring Boot 3.x + PostgreSQL 16 — fixed per architecture design
- **Auth**: JWT stateless authentication with RBAC (ROLE_USER, ROLE_AGENT, ROLE_ADMIN)
- **AI**: Spring AI with OpenAI API (Ollama fallback for demo)
- **i18n**: English + French required from the start
- **Containerization**: Docker Compose for local development
- **Monorepo**: `backend/` and `frontend/` directories with separate Dockerfiles

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Modular Monolith over microservices | Solo developer, rapid iteration, no distributed complexity | ✓ Shipped v1.0 |
| PostgreSQL FTS over Elasticsearch | Eliminates sidecar dependency, sufficient for doc corpus | ✓ Shipped v1.0 |
| Angular over React | Portfolio diversity (Spring + Angular pairing is enterprise standard) | ✓ Shipped v1.0 |
| JWT stateless auth with HttpOnly cookies | Simple, no session store, security best practice | ✓ Shipped v1.0 |
| Spring AI | Native Spring ecosystem integration for RAG workflows | ✓ Shipped v1.0 |
| Bilingual EN/FR from day one | Rare differentiator, costly to retrofit | ✓ Shipped v1.0 |
| Sequential TKT-NNNN numbering | Human-readable ticket IDs vs raw UUIDs | ✓ Shipped v1.0 |
| JSONB shift_left_context | Flexible schema for AI chat transcript storage | ✓ Shipped v1.0 |
| Testcontainers over H2 | Real PostgreSQL for integration tests (JSONB/TSVECTOR) | ✓ Shipped v1.0 |
| JaCoCo + SonarQube Cloud | Automated quality gating in CI pipeline | ✓ Shipped v1.0 |
| Row-level workspace_id isolation (not schema-per-tenant) | Preserves Flyway, Testcontainers, Spring AI pgvector integration | ✓ Shipped v2.0 |
| Hibernate @Filter + AOP for data isolation | Defense-in-depth against cross-tenant leakage | ✓ Shipped v2.0 ⚠️ Known gap: native SQL bypasses filter |
| WorkspaceChatModelRegistry | Per-workspace LLM routing with caching | ✓ Shipped v2.0 |
| Per-workspace seeders (4) + master seeder | Clean separation, idempotent via slug/email checks | ✓ Shipped v2.0 |
| Warm slate/charcoal palette with amber accent | Domain-agnostic neutral branding | ✓ Shipped v2.0 |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---

*Last updated: 2026-06-14 after milestone v2.1 Deployment*
