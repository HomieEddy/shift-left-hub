# Shift-Left Knowledge Hub - Architecture Requirements Document (ARD)

## 1. Architectural Strategy: The Modular Monolith

To balance rapid development with enterprise-grade maintainability, the application is structured as a **Modular Monolith**. The architecture adheres to **SOLID**, **KISS**, and **YAGNI** principles.

- **Presentation Layer (Client):** Angular 21.2, RxJS, Tailwind CSS v4. Reactive SPA with `@angular/localize` for bilingual (English/French) toggling.
- **Application / API Layer (Server):** Spring Boot 4.0.6, Spring Security, JJWT 0.13.0. Stateless RESTful APIs routing commands to domain services.
- **AI & Integration Layer:** Spring AI 1.1.7, OpenAI-compatible API (Ollama, Voyage, etc.). Orchestrates RAG workflows, KCS summarization, and document ETL.
- **Data Layer:** PostgreSQL 16 with pgvector extension for vector similarity search.

## 2. Module Map (Backend)

Package-by-module: each top-level package under `com.shiftleft.hub` is a domain, not a layer. Cross-module communication goes through service APIs or Spring `ApplicationEventPublisher`, never through shared mutable state.

| Module | Responsibility |
|--------|---------------|
| `common` | Shared base entities, problem detail builder, global exception handler, `WorkspaceFilterAspect` (Hibernate `@Filter` activation), `MasterSeeder` orchestrator + per-entity seeders |
| `config` | Security config (JWT filter chain, `@PreAuthorize` defaults, CORS), `RateLimitingFilter`, `EndpointUrlValidator` (SSRF guard) |
| `user` | `User` entity, `UserService`, `AuthService` (login/refresh), `AdminUserService` (admin CRUD), `SelfModificationException` |
| `workspace` | `Workspace`, `WorkspaceMember`, `WorkspaceInvitation`, `WorkspaceService` (incl. bulk member count), `WorkspaceRoleService` (per-workspace role context) |
| `auth` | Login/refresh/logout endpoints, `JwtService`, `AuthTokenService` (HttpOnly cookie management) |
| `article` | Knowledge base articles (bilingual), KCS drafts |
| `tag` | Workspace-scoped tags, `uc_tag_workspace_name_en` unique constraint (V4) |
| `category` | Workspace-scoped categories (self-referencing tree), `uc_category_workspace_parent_name_en` unique constraint (V4) |
| `document` | Upload pipeline: `DocumentFileStorage` (FS write + path-traversal sanitization), `DocumentConverter` (doc→article), `DocumentWorkspaceAccess` (lookup), `DocumentService` (orchestration) |
| `ticket` | Escalation tickets, `TicketService` (incl. `findAll` with `@EntityGraph` for user/assignedTo/resolvedBy — N+1 fix), work notes |
| `agent` | Agent dashboard, ticket resolution flow |
| `kcs` | KCS auto-drafting pipeline: `KcsDraftingService` (orchestrator) + `KcsPromptBuilder` + `KcsResponseParser` + `KcsDuplicateDetector` + `KcsSimilaritySearch` + `KcsSupportService` |
| `ai` | Spring AI integration: `AiConfigService` (bootstrap + update), `AiChatService` (hybrid search + streaming), `EmbeddingService`, `OpenAiCompatibleChatModel` + `OpenAiCompatibleEmbeddingModel`, `SseEmitterHelper` |
| `llmconfig` | Per-workspace LLM endpoint config (separate from global `AiConfig`) |

## 3. Core System Data Flows

### A. Multi-Tenant Workspace Isolation

1. **JWT Authentication:** User authenticates and receives JWT with `workspace_id` claim.
2. **AOP Filter Activation:** `WorkspaceFilterAspect` activates Hibernate `@Filter` on every request.
3. **Repository Queries:** All JPQL/HQL queries are automatically scoped to the active workspace via `workspace_id`.
4. **Vector Search:** pgvector metadata filtering scopes similarity searches to the active workspace.
5. **Note:** Native SQL queries require explicit `WHERE workspace_id = :workspaceId`.

### B. The RxJS Search & RAG Pipeline

1. **User Input:** User types query in Angular UI.
2. **Debounce:** RxJS `debounceTime` limits API spam.
3. **Hybrid Search:** Spring Boot executes PostgreSQL FTS + pgvector similarity search + document chunk search in parallel (CompletableFuture pool, v2.1 fix).
4. **RRF Fusion:** Results from all three sources are merged via Reciprocal Rank Fusion.
5. **Context Construction:** Top matches fed into Spring AI alongside user prompt.
6. **Response:** LLM streams contextual answer back to Angular via SSE.

### C. Document Ingestion Pipeline

1. **Upload:** User drags-and-drops file in admin document management UI.
2. **Validation:** MIME type + file extension validation; SHA-256 dedup check; path-traversal-safe filename sanitization (S-1 fix).
3. **Async Processing:** Spring `@Async` event triggers ETL on a dedicated `documentEtlExecutor` (v2.1 fix) so it does not compete with KCS for the same thread pool: PARSING → CHUNKING → EMBEDDING.
4. **Format-Specific Parsing:** Jsoup (HTML/XML), Apache POI (Word), basic text/markdown, PDF parsing.
5. **Storage:** Document chunks stored alongside vector embeddings in pgvector (batch insert in groups of 50, v2.1 fix).

### D. The Ticket Escalation Pipeline

1. **Trigger:** User clicks "Escalate" after AI assistant fails to resolve.
2. **Payload Assembly:** Angular bundles form inputs + AI chat transcript (via `buildEscalationPayload` helper, v2.1 fix).
3. **Persistence:** Spring Boot saves a `Ticket` entity with "Shift-Left Context" JSONB.
4. **Notification:** Ticket enters the "NEW" queue for agents.

### E. The KCS Automated Drafting Pipeline

1. **Trigger:** Agent resolves a ticket and flags as "Knowledge Gap".
2. **Async Processing:** Spring Boot fires a background event; `KcsEventListener` extracts work (no DB connection held during backoff `Thread.sleep`, v2.1 fix).
3. **AI Synthesis:** Spring AI feeds ticket timeline to the LLM.
4. **Draft Creation:** Markdown is saved as an unpublished Draft Article.

## 4. Security & Access Control

Security is handled via stateless **JSON Web Tokens (JWT)**.

- **Authentication:** Users authenticate via login endpoint, receiving a JWT in an HttpOnly cookie. The `JwtService` constructor fails fast on weak or dev-literal secrets (S-6 fix).
- **Workspace Authorization:**
  - JWT includes `workspace_id` claim scoping all API operations.
  - Hibernate `@Filter` + AOP provides defense-in-depth data isolation.
  - Workspace-scoped roles: `ADMIN`, `MEMBER`, `READ_ONLY` (checked via `WorkspaceRoleService`).
- **Global RBAC** (enforced via `@PreAuthorize` on controllers, S-14 fix):
  - `ROLE_USER`: Standard access to AI Assistant and ticket escalation.
  - `ROLE_AGENT`: Access to Agent Dashboard and ticket resolution.
  - `ROLE_ADMIN`: Full CRUD over tickets, users, workspaces, and system settings.
- **AI endpoint SSRF guard:** `EndpointUrlValidator` rejects private/loopback hosts in production unless `app.ai.endpoint.allow-private-hosts=true` (S-12 fix).
- **Path-traversal guard:** All uploaded filenames are sanitized; `..` segments rejected (S-1 fix).

## 5. Observability

- **Health:** `/actuator/health` (probes enabled; details shown when authorized) — used by Docker and Railway healthchecks.
- **Metrics:** `/actuator/prometheus`, `/actuator/metrics` (v2.2) — Micrometer Prometheus registry tagged with `application=knowledge-hub`.
- **Info:** `/actuator/info` (v2.2) — build metadata.
- **Logging:** SLF4J/Logback, with PII redaction (S-7 fix: JWT filter logs at DEBUG, with userId not email).
