# Shift-Left Knowledge Hub

> **Shift resolution as close to the user as possible.** An AI-powered, multi-tenant knowledge platform that intercepts Level 0/1 issues before they hit the queue — and learns from every ticket it can't.

---

## The Problem

Knowledge work has a documentation paradox: the teams best positioned to write knowledge articles are the same teams drowning in tickets. The result is a knowledge base that's always six months out of date, agents answering the same five questions a day, and users waiting in a queue for answers that already exist somewhere.

Traditional helpdesk platforms treat AI as a layer on top of a ticketing system. **Shift-Left Knowledge Hub inverts that** — the knowledge base *is* the product, the AI is the front door, and tickets are a fallback that capture the gaps.

## What It Does

- **Resolves issues at the source.** Users describe a problem in natural language and get a grounded answer sourced from the workspace's own documents, with citations back to the source.
- **Escapes gracefully.** When the AI can't answer confidently, the entire chat context — messages, retrieved sources, similarity scores — is bundled into a ticket and routed to a human agent. No re-explaining.
- **Closes the loop automatically.** Resolved tickets flagged as knowledge gaps trigger an AI-drafted article for agent review, so the next user with the same problem gets a Level 0 answer instead of a queue position.
- **Works for any domain.** IT, HR, legal, finance — the knowledge base defines the domain. Bring your own docs, bring your own LLM, ship in an afternoon.

## Highlights

- **Hybrid RAG pipeline** — Reciprocal Rank Fusion over PostgreSQL full-text search (persistent `tsvector` + GIN) and pgvector cosine similarity, with a configurable similarity floor (default 0.65) to suppress hallucination.
- **Modular monolith, package-by-module** — 13 bounded contexts (`user`, `article`, `ticket`, `ai`, `workspace`, `kcs`, …) sharing a single deployable. Clear module boundaries now, microservices later if scale demands it.
- **BYO LLM with encrypted secrets** — Per-workspace OpenAI-compatible endpoint (Ollama, Voyage, OpenAI, anything). API keys encrypted at rest; SSRF validation on every endpoint URL.
- **Event-driven knowledge capture (KCS)** — Spring `ApplicationEventPublisher` wires ticket resolution into AI-drafted articles. No message broker, no eventual-consistency surprises.
- **Security-first by audit** — JWT with HttpOnly cookies + refresh rotation, path-traversal-safe uploads, fail-fast on weak dev secrets. 17 threat mitigations verified in a dedicated v2.1 security pass.
- **Bilingual EN/FR from day one** — `@angular/localize` with dynamic layout handling. No retrofit, no `i18n.addLater()`.

## Features

### Multi-tenant workspaces
Isolated workspaces with their own users, knowledge base, taxonomy, and LLM configuration. Workspace admins manage everything from a single console.

### Document ingestion
Drag-and-drop upload for markdown, plain text, PDF, HTML, XML, and Word. Async ETL pipeline chunks, embeds, and indexes content without blocking the UI.

### AI assistant
Conversational interface with streaming responses, source citations, and hybrid search across both curated articles and raw document chunks. Similarity threshold prevents the assistant from fabricating answers when the KB is silent.

### Domain-agnostic taxonomy
Custom categories and per-workspace system prompts with template variables. The same platform serves IT helpdesk, HR policy, and legal research by changing the taxonomy and the seed content.

### Contextual ticketing
"Get me a human" preserves the full conversation: messages, retrieved sources, similarity scores, and the user's original query. Agents see exactly what the AI tried before escalating.

### KCS auto-drafting
Tickets marked as "knowledge gap" at resolution trigger an AI draft of a new article — pre-populated with the conversation context, ready for an agent to review and publish.

### Bilingual
English and French from initial release. Dynamic layout adapts to language direction and content length without breaking the grid.

### Security & observability
- JWT auth with HttpOnly cookies + refresh rotation (no localStorage)
- Encrypted LLM secrets at rest
- SSRF-safe AI endpoint validation
- Path-traversal-safe file uploads
- `/actuator/{health,info,prometheus,metrics}` exposed for monitoring

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Frontend** | Angular 21.2, Tailwind CSS v4, RxJS, Lucide Icons, Vitest |
| **Backend** | Spring Boot 4.0.6, Java 21, Spring Security, JJWT 0.13.0 |
| **Database** | PostgreSQL 16 + pgvector (persistent `tsvector` + GIN for FTS) |
| **AI** | Spring AI 1.1.7, OpenAI-compatible API (Ollama, Voyage, OpenAI, …) |
| **Deployment** | Docker Compose, Vercel (frontend), Railway (backend + DB) |
| **CI/CD** | GitHub Actions, Maven, pnpm, SonarCloud, commitlint + Husky |

## How It Works

```
User asks a question
        │
        ▼
┌─────────────────────┐
│  Hybrid Retriever   │  ◀── FTS (tsvector + GIN) ──┐
│  (RRF rank fusion)  │  ◀── pgvector cosine sim  ───┤
└─────────┬───────────┘                            │
          │ best chunk similarity > 0.65?           │
          ├── yes ──▶ LLM drafts grounded answer    │
          │              with citations              │
          │                                         │  Workspace
          └── no  ──▶ "Get me a human" button        │  knowledge
                       │                            │  base
                       ▼                            │
              ┌─────────────────┐                   │
              │  Ticket created │ ──── carries ─────┘
              │  with full      │      full chat
              │  chat context   │      context
              └────────┬────────┘
                       │ resolved + flagged
                       ▼
              ┌─────────────────┐
              │  KCS pipeline   │  ──▶ AI-drafted article
              │  (events)       │      for agent review
              └─────────────────┘
```

## Project Status

| Milestone | Status | Date | Scope |
|-----------|--------|------|-------|
| v1.0 Initial MVP | Shipped | 2026-06-08 | Phases 1-8 |
| v2.0 Workspace Platform | Shipped | 2026-06-14 | Phases 9-16 |
| v2.1 Codebase Hardening | Shipped | 2026-06-22 | Security audit (S-1..S-17) + Tiers 1-10 (PRs #72-#125) |
| v2.2 Post-Cleanup Polish | Shipped | 2026-06-23 | Tiers 12-17.6 (PRs #126-#131) |

---

## Developer Notes

<details>
<summary><strong>Quick Start</strong></summary>

```bash
# 1. Start PostgreSQL with pgvector
docker compose up -d

# 2. Start backend (terminal 1)
cd backend && ./mvnw spring-boot:run

# 3. Start frontend (terminal 2)
cd frontend && pnpm install && pnpm start

# 4. Open http://localhost:4200
```

</details>

<details>
<summary><strong>Project Structure</strong></summary>

```
shift-left-hub/
├── backend/                       # Spring Boot 4.0.6 (modular monolith)
│   ├── src/main/java/com/shiftleft/hub/
│   │   ├── agent/                 # Agent dashboard, ticket resolution
│   │   ├── ai/                    # Spring AI, RAG, hybrid search
│   │   ├── article/               # Knowledge base articles
│   │   ├── auth/                  # Authentication endpoints (legacy)
│   │   ├── category/              # Domain taxonomy
│   │   ├── common/                # Shared base entities, context holders, exception handling
│   │   ├── config/                # Security, JWT, app config
│   │   ├── document/              # Document ingestion pipeline
│   │   ├── kcs/                   # KCS auto-drafting
│   │   ├── llmconfig/             # Per-workspace LLM configuration
│   │   ├── tag/                   # Article tags
│   │   ├── ticket/                # Escalation and ticketing
│   │   ├── user/                  # Authentication, user management
│   │   └── workspace/             # Multi-tenant workspace management
│   └── src/main/resources/
│       ├── application.properties
│       ├── db/migration/          # Flyway migrations (V1-V4)
│       └── data/seed/             # Workspace seed content
├── frontend/                      # Angular 21.2 SPA
│   └── src/app/
│       ├── core/                  # Interceptors, guards, services
│       ├── features/              # Domain feature modules
│       └── shared/                # Reusable UI components
├── docs/                          # Design and planning documents
│   ├── PRD.md                     # Product Requirements Document
│   ├── ARD.md                     # Architecture Requirements
│   ├── DDD.md                     # Database Design Document
│   ├── CCG.md                     # Clean Code Guidelines
│   ├── UXD.md                     # UX/UI Design Document
│   ├── TSD.md                     # Testing Strategy Document
│   ├── DHS.md                     # Deployment & Hosting Strategy
│   ├── VCG.md                     # Version Control Guidelines
│   └── LDIG.md                    # Local Development & Infrastructure Guide
├── e2e/                           # Playwright specs (1 Golden Path + 9 exploratory)
├── .github/                       # CI workflows, PR template, CODEOWNERS
├── docker-compose.yml             # PostgreSQL + pgvector
├── AGENTS.md                      # Project guide for AI agents and contributors
├── CONTRIBUTING.md                # Workflow, quality gates, commit types
└── SECURITY.md                    # Reporting vulnerabilities, security model
```

</details>

<details>
<summary><strong>Test Coverage (v2.2)</strong></summary>

| Layer | Count | Stack |
|-------|-------|-------|
| Backend | 469 | JUnit 5 + Mockito + Testcontainers (PostgreSQL 16) |
| Frontend | 306 | Vitest, HttpTestingController, signal-based components |
| E2E | 10 | 1 Golden Path (CI-required) + 9 exploratory (non-gating) |

</details>

<details>
<summary><strong>Documentation</strong></summary>

The `docs/` directory contains the full design rationale. Start here:

- [PRD](docs/PRD.md) — Product requirements and value proposition
- [ARD](docs/ARD.md) — Architecture and data flows
- [DDD](docs/DDD.md) — Database schema and indexing strategy
- [CCG](docs/CCG.md) — Coding standards and conventions
- [TSD](docs/TSD.md) — Testing strategy

For an end-to-end walkthrough, see [demo-walkthrough.md](docs/demo-walkthrough.md).

</details>

---

## License

MIT — see [LICENSE](LICENSE).
