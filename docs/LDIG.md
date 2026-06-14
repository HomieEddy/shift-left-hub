# Shift-Left Knowledge Hub - Local Development & Infrastructure Guide (LDIG)

## 1. Monorepo File Structure
```text
shift-left-hub/
├── .gitignore
├── docker-compose.yml       # Orchestrates PostgreSQL 16 + pgvector
├── README.md                # Project overview and setup guide
│
├── backend/                 # Spring Boot 3.5 Application (Java 21)
│   ├── Dockerfile           # Multi-stage production build
│   ├── pom.xml              # Maven dependencies
│   └── src/main/
│       ├── java/com/shiftleft/hub/
│       │   ├── config/      # Security, JWT, Spring AI configs
│       │   ├── common/      # Shared base entities, contexts
│       │   ├── user/        # Authentication, user management
│       │   ├── article/     # Knowledge base articles
│       │   ├── ticket/      # Escalation and ticketing
│       │   ├── agent/       # Agent dashboard
│       │   ├── ai/          # Spring AI, RAG, hybrid search
│       │   ├── workspace/   # Multi-tenant workspace management
│       │   └── document/    # Document ingestion pipeline
│       └── resources/
│           ├── application.yml
│           ├── db/migration/ # Flyway migrations (V1-V7)
│           └── data/seed/   # Workspace seed data (hr/, legal/, it/, public/)
│
└── frontend/                # Angular 21.2 Application
    ├── Dockerfile           # Multi-stage Nginx build
    ├── package.json         # pnpm dependencies
    └── src/
        ├── app/
        │   ├── core/        # Interceptors, Auth Guards, Services
        │   ├── features/    # Sub-modules per domain
        │   └── shared/      # UI components, pipes, directives
        └── styles.css       # Tailwind imports
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

## 5. Backend Dependencies (Spring Boot 3.5 / Java 21)
* **Core:** `spring-boot-starter-web`, `spring-boot-devtools`
* **Data:** `spring-boot-starter-data-jpa`, `postgresql`, `flyway-core`, `flyway-database-postgresql`
* **Security:** `spring-boot-starter-security`, `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (0.13.0)
* **Spring AI:** `spring-ai-openai-spring-boot-starter`, `spring-ai-pgvector-store`, `spring-ai-tika-document-reader` (BOM 1.1.7)
* **Parsing:** `jsoup` (1.19.1), `poi-ooxml` (5.4.0)
* **Testing:** `spring-boot-starter-test`, `testcontainers` (postgresql, junit-jupiter)

## 6. Frontend Dependencies (Angular 21.2)
* **Styling:** `tailwindcss` v4, `postcss`, `autoprefixer`
* **Markdown:** `ngx-markdown`
* **Icons:** `lucide-angular`
* **i18n:** `@angular/localize`
