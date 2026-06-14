# Shift-Left Knowledge Hub

A plug-and-play knowledge platform that turns any collection of documents into an intelligent, AI-powered assistant. Workspaces can bring their own knowledge base and their own LLM — making it domain-agnostic. IT helpdesk, HR policy lookup, legal research: the KB defines the domain.

## Status

| Milestone | Status | Date | Phases |
|-----------|--------|------|--------|
| v1.0 Initial MVP | ✅ Shipped | 2026-06-08 | 1-8 |
| v2.0 Workspace Platform | ✅ Shipped | 2026-06-14 | 9-16 |

## Features

- **Multi-Tenant Workspaces** — Isolated workspaces with their own users, KB, LLM config, and taxonomy
- **Document Ingestion** — Drag-and-drop upload for markdown, text, PDF, HTML, XML, Word; async ETL pipeline
- **BYO LLM** — Per-workspace OpenAI-compatible endpoint configuration; API keys encrypted at rest
- **AI Assistant** — Conversational interface with hybrid search (FTS + vector + RRF) across articles and document chunks
- **Domain-Agnostic Taxonomy** — Custom categories and system prompts with template variables
- **Contextual Ticketing** — Escalate with full AI chat context preserved
- **KCS Auto-Drafting** — AI drafts new articles from resolved tickets flagged as knowledge gaps
- **Bilingual** — English/French from day one with dynamic layout handling

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Angular 21.2, Tailwind CSS v4, RxJS, Lucide Icons |
| Backend | Spring Boot 3.5, Java 21, Spring Security, JWT |
| Database | PostgreSQL 16 + pgvector |
| AI | Spring AI 1.1.7, OpenAI API (Ollama fallback) |
| Deployment | Docker Compose, Vercel (frontend), Railway (backend + DB) |
| CI/CD | GitHub Actions, Maven, pnpm |

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
├── backend/               # Spring Boot 3.5 (modular monolith)
│   ├── src/main/java/com/shiftleft/hub/
│   │   ├── ai/            # Spring AI, RAG, hybrid search
│   │   ├── article/       # Knowledge base articles
│   │   ├── common/        # Shared base entities, context holders
│   │   ├── config/        # Security, JWT, app config
│   │   ├── document/      # Document ingestion pipeline
│   │   ├── ticket/        # Escalation and ticketing
│   │   ├── user/          # Authentication and users
│   │   └── workspace/     # Multi-tenant workspace management
│   └── src/main/resources/
│       ├── db/migration/  # Flyway migrations
│       └── data/seed/     # Workspace seed content
├── frontend/              # Angular 21.2 SPA
│   └── src/app/
│       ├── core/          # Interceptors, guards, services
│       ├── features/      # Domain feature modules
│       └── shared/        # Reusable UI components
├── docs/                  # Design and planning documents
│   ├── PRD.md             # Product Requirements Document
│   ├── ARD.md             # Architecture Requirements
│   ├── DDD.md             # Database Design Document
│   ├── CCG.md             # Clean Code Guidelines
│   ├── UXD.md             # UX/UI Design Document
│   ├── TSD.md             # Testing Strategy Document
│   ├── DHS.md             # Deployment & Hosting Strategy
│   ├── VCG.md             # Version Control Guidelines
│   ├── LDIG.md            # Local Development & Infrastructure Guide
│   └── demo-walkthrough.md # End-to-end demo script
└── docker-compose.yml     # PostgreSQL + pgvector
```

## Documentation

The `docs/` directory contains the full design rationale for the project. Start with:

- [PRD](docs/PRD.md) — Product requirements and value proposition
- [ARD](docs/ARD.md) — Architecture and data flows
- [DDD](docs/DDD.md) — Database schema and indexing strategy
- [CCG](docs/CCG.md) — Coding standards and conventions

## Demo

See [demo-walkthrough.md](docs/demo-walkthrough.md) for a guided end-to-end tour of the application.

## License

MIT
