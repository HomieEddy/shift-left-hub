# Shift-Left Knowledge Hub - Deployment & Hosting Strategy (DHS)

> **Updated:** 2026-06-23 — v2.2.
> Default branch is `master` (not `main`). Frontend build output is `dist/shiftleft-hub/browser` (Angular 17+ application builder). Actuator now exposes `health`, `info`, `prometheus`, `metrics` (v2.2 tier 17).

## 1. Hosting Architecture

To achieve an enterprise-grade deployment (HTTPS, global edge caching, automated builds) without incurring unnecessary cloud costs, the application utilizes a decoupled hosting strategy leveraging modern Platform-as-a-Service (PaaS) providers.

### A. Frontend (Angular) → Vercel

- **The Strategy:** Angular compiles to static HTML, CSS, and JavaScript. It does not require a running server environment.
- **The Execution:** Connected directly to the GitHub repository, Vercel automatically builds the application (`pnpm build`) and deploys the `dist/shiftleft-hub/browser` output to its Global Edge CDN.
- **The Value:** Ensures sub-50ms Time-To-Interactive (TTI) for the initial UI load, regardless of the user's geographic location.

### B. Backend (Spring Boot 4 / Java 21) → Railway

- **The Strategy:** Spring Boot requires a continuous JVM runtime. Railway provides a robust, managed environment with native Docker support.
- **The Execution:** Railway monitors the `backend/` directory in the repository. Upon changes, it detects the `Dockerfile`, builds the `.jar`, and runs the container.
- **The Value:** Guarantees the backend environment is completely immutable and perfectly mirrors the local Docker setup.

### C. Database (PostgreSQL 16 + pgvector) → Railway Managed Database

- **The Strategy:** Keep the database and the backend API within the same private network for security and zero-latency communication.
- **The Execution:** Provision a managed PostgreSQL 16 service with pgvector extension directly within the Railway project environment alongside the backend service.
- **The Value:** The Spring Boot API connects to the database internally. The database is not exposed to the public internet, dramatically reducing the security attack surface.

---

## 2. CI/CD Pipeline & Automated Deployments

Continuous Integration and Continuous Deployment (CI/CD) are handled natively by the hosting providers upon merging code into `master`.

### A. Frontend CI/CD (Vercel)

1. **Trigger:** Pull Request merged into `master` affecting the `frontend/` directory.
2. **Build:** Vercel provisions a Node.js 22 environment, runs `pnpm install`, and executes the Angular production build (`pnpm build`).
3. **Deploy:** The new static assets are pushed to the Edge CDN, and the previous cache is invalidated automatically.

### B. Backend CI/CD (Railway)

1. **Trigger:** Pull Request merged into `master` affecting the `backend/` directory.
2. **Build:** Railway executes the `Dockerfile`, running the Maven build and creating the Java 21 runtime image.
3. **Deploy:** Railway performs a zero-downtime rollout. The new container spins up, health checks are verified, and traffic is seamlessly routed from the old container to the new one.

### C. CI Gatekeeping (GitHub Actions `.github/workflows/ci.yml`)

| Job | Trigger | Command | Blocks |
|-----|---------|---------|--------|
| Backend Checkstyle + SpotBugs + Compile | PR + push to `master` | `./mvnw compile spotbugs:check -B` | Vercel + Railway |
| Backend Unit Tests | PR + push to `master` | `./mvnw test -B -Dtest='!*IntegrationTest,!KnowledgeHubApplicationTests'` | Vercel + Railway |
| Backend Integration Tests | PR + push to `master` | `./mvnw test -B -Dtest='*IntegrationTest,KnowledgeHubApplicationTests'` (Testcontainers) | Vercel + Railway |
| Backend SonarQube Cloud Scan | after unit + integration | `./mvnw sonar:sonar` | non-blocking (`continue-on-error: true`) |
| Frontend Lint + Test + Build | PR + push to `master` | `pnpm lint && pnpm test && pnpm build` | Vercel |
| Vercel Preview Comments | PR | automatic | non-blocking |

### D. Post-Deploy

Optional Playwright Golden Path run against the live Vercel URL (`https://shift-left-hub.vercel.app`) to catch environment-specific configuration errors before users do.

---

## 3. Environment Variable & Secret Management

Credentials and API keys are strictly managed via provider environment variables. **No secrets are ever committed to the repository.** See `pre-deploy-checklist.md` for the full env-var matrix.

### A. Backend Variables (Configured in Railway)

Key variables (see `pre-deploy-checklist.md` for the full table):

- `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` — Railway internal network
- `APP_JWT_SECRET` — 256-bit random; `JwtService` constructor fails fast on weak/dev-literal secrets (S-6)
- `SPRING_AI_OPENAI_API_KEY` or per-workspace `APP_*_AI_*` overrides
- `APP_AUTH_COOKIE_SECURE=true`, `APP_AUTH_COOKIE_SAME_SITE=Lax` (or `Strict`)
- `APP_CORS_ALLOWED_ORIGINS=https://[your-vercel-app].vercel.app` (no wildcards)
- `APP_AI_ENCRYPTION_KEY` + `APP_AI_ENCRYPTION_SALT` for at-rest encryption of LLM API keys
- `SPRING_PROFILES_ACTIVE=docker` enables the Docker profile

### B. Frontend Variables (Configured in Vercel)

The Vercel-hosted Angular SPA is fully static. Runtime API calls go to the relative `/api/...` path, which `frontend/vercel.json` rewrites to the Railway backend. The frontend therefore does not need any backend-related env vars at build time.

---

## 4. Observability Endpoints

All exposed at `/actuator/*` on the backend (v2.2 tier 17):

| Endpoint | Use | Auth |
|----------|-----|------|
| `/actuator/health` | Docker + Railway healthcheck | public; details only when authorized |
| `/actuator/health/liveness` | Kubernetes-style liveness probe | public |
| `/actuator/health/readiness` | Kubernetes-style readiness probe | public |
| `/actuator/prometheus` | Prometheus scrape (Micrometer registry) | public; network-isolated via Railway private network |
| `/actuator/metrics` | Human-readable metric list | public; network-isolated |
| `/actuator/info` | Build metadata | public |

Configuration lives in `backend/src/main/resources/application.properties` under `management.endpoints.web.exposure.include`.

---

## 5. Domain Routing & DNS Strategy

To present a professional, unified product, custom DNS records route traffic appropriately.

- **Frontend:** A custom domain (e.g. `shiftleft.dev`) is attached to the Vercel project via a `CNAME` record. Vercel automatically provisions and renews the SSL certificate.
- **Backend:** A subdomain (e.g. `api.shiftleft.dev`) is attached to the Railway project. This allows for secure, predictable CORS rules since both the frontend and backend share the same root domain.
