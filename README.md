# Shift-Left Knowledge Hub

A plug-and-play knowledge platform that turns any collection of documents into an intelligent, AI-powered assistant. Workspaces can bring their own knowledge base and their own LLM — making it domain-agnostic. IT helpdesk, HR policy lookup, legal research: the KB defines the domain.

## Status

| Milestone | Status | Date | Phases |
|-----------|--------|------|--------|
| v1.0 Initial MVP | Shipped | 2026-06-08 | 1-8 |
| v2.0 Workspace Platform | Shipped | 2026-06-14 | 9-16 |
| v2.1 Codebase Hardening | Shipped | 2026-06-22 | Security (S-1..S-17) + Tiers 1-10 cleanup (PRs #72-#125) |
| v2.2 Post-Cleanup Polish | Shipped | 2026-06-23 | Tiers 12, 13, 14/18, 15, 16/17, 17.5/17.6 (PRs #126-#131) |

## Features

- **Multi-Tenant Workspaces** — Isolated workspaces with their own users, KB, LLM config, and taxonomy
- **Document Ingestion** — Drag-and-drop upload for markdown, text, PDF, HTML, XML, Word; async ETL pipeline
- **BYO LLM** — Per-workspace OpenAI-compatible endpoint configuration; API keys encrypted at rest
- **AI Assistant** — Conversational interface with hybrid search (FTS + vector + RRF) across articles and document chunks
- **Domain-Agnostic Taxonomy** — Custom categories and system prompts with template variables
- **Contextual Ticketing** — Escalate with full AI chat context preserved
- **KCS Auto-Drafting** — AI drafts new articles from resolved tickets flagged as knowledge gaps
- **Bilingual** — English/French from day one with dynamic layout handling
- **Security** — JWT with HttpOnly cookies + refresh rotation, path-traversal-safe uploads, SSRF-safe AI endpoints, fail-fast on weak dev secrets (v2.1 audit, S-1..S-17)
- **Accessibility** — ARIA-labelled dialogs, focus management, keyboard navigation (v2.2 tier 17)
- **Observability** — `/actuator/prometheus`, `/actuator/metrics`, `/actuator/info` (v2.2 tier 17)

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Angular 21.2, Tailwind CSS v4, RxJS, Lucide Icons, Vitest |
| Backend | Spring Boot 4.0.6, Java 21, Spring Security, JJWT 0.13.0 |
| Database | PostgreSQL 16 + pgvector |
| AI | Spring AI 1.1.7, OpenAI-compatible API (Ollama, Voyage, etc.) |
| Deployment | Docker Compose, Vercel (frontend), Railway (backend + DB) |
| CI/CD | GitHub Actions, Maven, pnpm, SonarCloud, commitlint |

## Quick Start

```bash
# 1. Start PostgreSQL with pgvector
docker compose up -d

# 2. Start backend (terminal 1)
cd backend && ./mvnw spring-boot:run

# 3. Start frontend (terminal 2)
cd frontend && pnpm install && pnpm start

# 4. Open http://localhost:4200
```

## Project Structure

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
│   ├── LDIG.md                    # Local Development & Infrastructure Guide
│   ├── pre-deploy-checklist.md    # Step-by-step production deploy
│   └── demo-walkthrough.md        # End-to-end demo script
├── e2e/                           # Playwright specs (1 Golden Path + 9 exploratory)
├── scripts/                       # Local helper scripts
├── run/                           # Local run configurations
├── .github/                       # CI workflows, PR template, CODEOWNERS
├── LICENSE
├── CONTRIBUTING.md
├── SECURITY.md
└── docker-compose.yml             # PostgreSQL + pgvector
```

## Documentation

The `docs/` directory contains the full design rationale for the project. Start with:

- [PRD](docs/PRD.md) — Product requirements and value proposition
- [ARD](docs/ARD.md) — Architecture and data flows
- [DDD](docs/DDD.md) — Database schema and indexing strategy
- [CCG](docs/CCG.md) — Coding standards and conventions

The project also ships developer-facing guides at the repo root:

- [AGENTS.md](AGENTS.md) — Project guide for AI agents and human contributors
- [CONTRIBUTING.md](CONTRIBUTING.md) — Workflow, quality gates, commit types
- [SECURITY.md](SECURITY.md) — Reporting vulnerabilities, security model

## Test counts (as of v2.2)

| Layer | Count | Note |
|-------|-------|------|
| Backend tests | 469 | JUnit 5 + Mockito + Testcontainers (PostgreSQL 16) |
| Frontend tests | 306 | Vitest, HttpTestingController, signal-based components |
| E2E specs | 10 | 1 Golden Path (CI-required) + 9 exploratory (non-gating) |

## Demo

See [demo-walkthrough.md](docs/demo-walkthrough.md) for a guided end-to-end tour of the application.

## License

MIT — see [LICENSE](LICENSE).
