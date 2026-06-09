# Shift-Left Knowledge Hub

## What This Is

A modern IT Service Management (ITSM) web application that transforms corporate helpdesks from passive ticket systems into proactive resolution platforms. Provides an intelligent assistant that guides users through troubleshooting using company documentation, escalates with full context when needed, and auto-generates new knowledge base articles from resolved tickets — creating a continuous improvement loop.

v1.0 ships: conversational AI triage, contextual ticketing, agent dashboard with resolution workflow, and KCS auto-drafting — all gated by CI/CD.

## Core Value

Shift resolution as close to the user as possible by intercepting Level 0/1 issues before they reach the queue, while simultaneously eliminating the documentation burden on IT agents.

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

### Active

(No active requirements — v1.0 shipped. Next milestone: planning.)

### Out of Scope

- External search engines (Elasticsearch) — PostgreSQL FTS + pgvector handles all search needs
- NoSQL databases — Postgres-native schema with JSONB, arrays, and TSVECTOR
- Email notifications — not core to KCS loop
- SLA management — enterprise feature, no portfolio demo value
- Real-time agent chat — destroys async workflow benefits, requires staffing
- Mobile native app — responsive SPA is sufficient
- SSO/OAuth — setup complexity not justified for portfolio demo
- Multi-tenancy — single-tenant portfolio app

## Context

- **Domain:** ITSM / Knowledge-Centered Service (KCS)
- **Architecture:** Modular Monolith — Angular 21.2 SPA + Spring Boot 3.5 REST API + PostgreSQL 16 with pgvector
- **AI Pipeline:** RAG with hybrid search (FTS + pgvector + RRF); Ollama/OpenAI provider
- **Bilingual:** English/French with dynamic layout handling
- **Team:** Solo developer — all decisions prioritize demonstrable functionality
- **v1.0 shipped:** 8 phases, 60 plans, 41 requirements
- **Codebase:** ~8,700 frontend LOC + ~5,500 backend main LOC + ~2,200 test LOC
- **Coverage:** 112 backend integration tests (Testcontainers) + 104 frontend tests + Playwright E2E

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
*Last updated: 2026-06-08 after v1.0 milestone*
