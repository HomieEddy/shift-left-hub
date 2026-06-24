# Shift-Left Knowledge Hub - Local Development & Infrastructure Guide (LDIG)

> **Updated:** 2026-06-23 — v2.2.
> Backend is Spring Boot 4 (not 3.5). Configuration file is `application.properties` (not yml). Module list reflects the actual structure.

## 1. Monorepo File Structure

```text
shift-left-hub/
├── .gitignore
├── docker-compose.yml             # Orchestrates PostgreSQL 16 + pgvector
├── README.md                      # Project overview and setup guide
├── AGENTS.md                      # AI-agent / contributor guide
├── AUDIT_FINDINGS.md              # v2.1 cleanup audit (historical)
├── LICENSE                        # MIT
├── CONTRIBUTING.md
├── SECURITY.md
├── commitlint.config.cjs          # Conventional-commits enforcement
│
├── backend/                       # Spring Boot 4.0.6 (Java 21)
│   ├── Dockerfile                 # Multi-stage production build
│   ├── pom.xml                    # Maven dependencies
│   └── src/main/
│       ├── java/com/shiftleft/hub/
│       │   ├── KnowledgeHubApplication.java
│       │   ├── agent/             # Agent dashboard, ticket resolution
│       │   ├── ai/                # Spring AI, RAG, hybrid search
│       │   ├── article/           # Knowledge base articles
│       │   ├── auth/              # Login/refresh/logout endpoints
│       │   ├── category/          # Domain taxonomy (self-referencing)
│       │   ├── common/            # Shared base entities, GlobalExceptionHandler, seeders
│       │   ├── config/            # Security, JWT, rate limit, SSRF guard
│       │   ├── document/          # Document ingestion pipeline
│       │   ├── kcs/               # KCS auto-drafting (5 single-responsibility classes)
│       │   ├── llmconfig/         # Per-workspace LLM config
│       │   ├── tag/               # Article tags
│       │   ├── ticket/            # Escalation and ticketing
│       │   ├── user/              # Auth, user management
│       │   └── workspace/         # Multi-tenant workspace management
│       └── resources/
│           ├── application.properties
│           ├── db/migration/      # Flyway migrations V1-V4
│           └── data/seed/         # Workspace seed content (hr/, legal/, it/, public/)
│
├── frontend/                      # Angular 21.2 application
│   ├── Dockerfile                 # Multi-stage Nginx build
│   ├── package.json               # pnpm dependencies
│   ├── angular.json
│   ├── proxy.conf.json            # Local dev: /api -> backend
│   └── src/
│       ├── app/
│       │   ├── core/              # Interceptors, Auth Guards, Services
│       │   ├── features/          # Sub-modules per domain
│       │   └── shared/            # UI components, pipes, directives
│       ├── styles.css
│       └── tailwind.src.css
│
├── docs/                          # Design and planning documents
│   ├── PRD.md
│   ├── ARD.md
│   ├── DDD.md
│   ├── CCG.md
│   ├── UXD.md
│   ├── TSD.md
│   ├── DHS.md
│   ├── VCG.md
│   ├── LDIG.md (this file)
│   ├── pre-deploy-checklist.md
│   └── demo-walkthrough.md
│
├── e2e/                           # Playwright specs
│   ├── pages/                     # Page objects
│   ├── tests/                     # 9 exploratory specs
│   ├── fixtures/                  # sample.md/html/xml/pdf/docx
│   └── playwright/                # golden-path.spec.ts (CI-required)
│
├── scripts/                       # Local helper scripts
├── run/                           # Local run configurations
├── .github/
│   ├── workflows/ci.yml
│   ├── pull_request_template.md
│   ├── ISSUE_TEMPLATE/change.yml
│   └── CODEOWNERS
└── .husky/                        # pre-commit, pre-push, commit-msg
```

## 2. Prerequisites

- **Docker Desktop** (for PostgreSQL 16 + pgvector)
- **Node.js 22+** and **pnpm** (for frontend)
- **Java 21+** (for backend)
- **Maven** (wrapper included: `./mvnw`)

## 3. Quick Start

```bash
# 1. Start PostgreSQL with pgvector
docker compose up -d

# 2. Start backend
cd backend && ./mvnw spring-boot:run

# 3. In another terminal, start frontend
cd frontend && pnpm install && pnpm start

# 4. Open http://localhost:4200
```

## 4. Containerization Strategy (`docker-compose.yml`)

```yaml
version: '3.8'
services:
  postgres:
    image: pgvector/pgvector:0.8.0-pg16
    container_name: shift_left_db
    environment:
      POSTGRES_USER: devuser
      POSTGRES_PASSWORD: devpassword
      POSTGRES_DB: shiftleft_db
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U devuser"]
      interval: 5s

volumes:
  pgdata:
```

The `pgvector/pgvector:0.8.0-pg16` image is mandatory — the plain `postgres:16-alpine` image lacks the pgvector extension.

## 5. Backend Dependencies (Spring Boot 4.0.6 / Java 21)

- **Core:** `spring-boot-starter-web`
- **Data:** `spring-boot-starter-data-jpa`, `postgresql`, `flyway-core`, `flyway-database-postgresql`
- **Security:** `spring-boot-starter-security`, `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (0.13.0)
- **Spring AI:** `spring-ai-openai-spring-boot-starter`, `spring-ai-pgvector-store`, `spring-ai-tika-document-reader` (BOM 1.1.7)
- **Actuator + Observability:** `spring-boot-starter-actuator`, `micrometer-registry-prometheus` (v2.2)
- **Parsing:** `jsoup` (1.19.1), `poi-ooxml` (5.4.0)
- **Rate limiting:** `caffeine`
- **Testing:** `spring-boot-starter-test`, `testcontainers` (postgresql, junit-jupiter)

## 6. Frontend Dependencies (Angular 21.2)

- **Styling:** `tailwindcss` v4 (`@tailwindcss/cli`, `@tailwindcss/postcss`, `@tailwindcss/vite`)
- **Markdown:** `ngx-markdown`
- **Sanitization:** `dompurify`
- **Icons:** `@lucide/angular`
- **i18n:** `@angular/localize`
- **Testing:** `vitest` (not Karma), `jsdom`, `@vitest/coverage-v8`
- **Linting:** `eslint`, `angular-eslint`, `typescript-eslint`
- **E2E:** `@playwright/test`

## 7. Configuration

All backend configuration is in `backend/src/main/resources/application.properties`. Frontend runtime config (the API base URL) is injected via `frontend/public/env.js` at deploy time, with a dev default of `apiBaseUrl = ''` so Vercel rewrites work.

## 8. Dev URLs

| Service | URL |
|---------|-----|
| Frontend dev | `http://localhost:4200` |
| Backend dev | `http://localhost:8080` |
| Health check | `http://localhost:8080/actuator/health` |
| Prometheus metrics | `http://localhost:8080/actuator/prometheus` |
| PostgreSQL | `localhost:5432` (`devuser` / `devpassword` / `shiftleft_db`) |

The frontend dev server proxies `/api/*` to `http://localhost:8080` via `proxy.conf.json`. The Docker dev compose uses a separate `proxy.docker.conf.json` (with `host.docker.internal`).
