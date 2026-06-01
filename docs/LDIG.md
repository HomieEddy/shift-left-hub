# Shift-Left Knowledge Hub - Local Development & Infrastructure Guide (LDIG)

## 1. Monorepo File Structure
```text
shift-left-hub/
├── .gitignore
├── docker-compose.yml       # Orchestrates PostgreSQL and pgAdmin
├── README.md                # Contains PRD, ARD, DDD, and UXD
│
├── backend/                 # Spring Boot 3.x Application (Java 21)
│   ├── Dockerfile           # For full-stack prod build testing
│   ├── pom.xml              # Maven dependencies
│   └── src/main/
│       ├── java/com/shiftleft/
│       │   ├── config/      # Security, JWT, Spring AI configs
│       │   ├── domain/      # Sub-packages: ticket, article, user
│       │   └── ShiftLeftApplication.java
│       └── resources/
│           ├── application.yml
│           └── knowledge-base/ # Markdown files for ETL ingestion
│
└── frontend/                # Angular Application
    ├── Dockerfile           # Multi-stage Nginx build
    ├── package.json         # NPM dependencies
    ├── tailwind.config.js   # UI Styling configuration
    └── src/
        ├── app/
        │   ├── core/        # Interceptors, Auth Guards, Services
        │   ├── features/    # Sub-modules: portal, agent-dashboard
        │   └── shared/      # UI components, animations
        └── styles.css       # Tailwind imports
```
		
## 2. Containerization Strategy (`docker-compose.yml`)
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16-alpine
    container_name: shift_left_db
    environment:
      POSTGRES_USER: devuser
      POSTGRES_PASSWORD: devpassword
      POSTGRES_DB: shiftleft_db
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  pgadmin:
    image: dpage/pgadmin4
    container_name: shift_left_pgadmin
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@shiftleft.com
      PGADMIN_DEFAULT_PASSWORD: admin
    ports:
      - "5050:80"
    depends_on:
      - postgres

volumes:
  pgdata:
```

## 3. Backend Dependencies (Spring Boot 3.x / Java 21)
* **Core:** `spring-boot-starter-web`, `spring-boot-devtools`
* **Data:** `spring-boot-starter-data-jpa`, `postgresql`
* **Security:** `spring-boot-starter-security`, `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (v0.12.5)
* **Spring AI:** `spring-ai-openai-spring-boot-starter`, `spring-ai-markdown-document-reader` (managed via `spring-ai-bom` v1.0.0-M1).

## 4. Frontend Dependencies (Angular)
* **Styling:** `tailwindcss`, `postcss`, `autoprefixer`, `@tailwindcss/typography`
* **Parsing:** `ngx-markdown`
* **Animations:** `@angular/animations`