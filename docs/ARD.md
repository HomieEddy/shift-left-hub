# Shift-Left Knowledge Hub - Architecture Requirements Document (ARD)

## 1. Architectural Strategy: The Modular Monolith
To balance rapid development with enterprise-grade maintainability, the application is structured as a **Modular Monolith**. The architecture adheres to **SOLID**, **KISS**, and **YAGNI** principles.

* **Presentation Layer (Client):** Angular 21.2, RxJS, Tailwind CSS v4. Reactive SPA with `@angular/localize` for bilingual (English/French) toggling.
* **Application / API Layer (Server):** Spring Boot 3.5, Spring Security, JWT. Stateless RESTful APIs routing commands to domain services.
* **AI & Integration Layer:** Spring AI 1.1.7, OpenAI API (Ollama fallback). Orchestrates RAG workflows, KCS summarization, and document ETL.
* **Data Layer:** PostgreSQL 16 with pgvector extension for vector similarity search.

## 2. Core System Data Flows

### A. Multi-Tenant Workspace Isolation
1. **JWT Authentication:** User authenticates and receives JWT with `workspace_id` claim.
2. **AOP Filter Activation:** `WorkspaceFilterAspect` activates Hibernate `@Filter` on every request.
3. **Repository Queries:** All JPQL/HQL queries are automatically scoped to the active workspace via `workspace_id`.
4. **Vector Search:** pgvector metadata filtering scopes similarity searches to the active workspace.
5. **Note:** Native SQL queries require explicit `WHERE workspace_id = :workspaceId`.

### B. The RxJS Search & RAG Pipeline
1. **User Input:** User types query in Angular UI.
2. **Debounce:** RxJS `debounceTime` limits API spam.
3. **Hybrid Search:** Spring Boot executes PostgreSQL FTS + pgvector similarity search + document chunk search.
4. **RRF Fusion:** Results from all three sources are merged via Reciprocal Rank Fusion.
5. **Context Construction:** Top matches fed into Spring AI alongside user prompt.
6. **Response:** LLM streams contextual answer back to Angular via SSE.

### C. Document Ingestion Pipeline
1. **Upload:** User drags-and-drops file in admin document management UI.
2. **Validation:** MIME type + file extension validation; SHA-256 dedup check.
3. **Async Processing:** Spring `@Async` event triggers ETL: PARSING → CHUNKING → EMBEDDING.
4. **Format-Specific Parsing:** Jsoup (HTML/XML), Apache POI (Word), basic text/markdown, PDF parsing.
5. **Storage:** Document chunks stored alongside vector embeddings in pgvector.

### D. The Ticket Escalation Pipeline
1. **Trigger:** User clicks "Escalate" after AI assistant fails to resolve.
2. **Payload Assembly:** Angular bundles form inputs + AI chat transcript.
3. **Persistence:** Spring Boot saves a `Ticket` entity with "Shift-Left Context".
4. **Notification:** Ticket enters the "NEW" queue for agents.

### E. The KCS Automated Drafting Pipeline
1. **Trigger:** Agent resolves a ticket and flags as "Knowledge Gap".
2. **Async Processing:** Spring Boot fires a background event.
3. **AI Synthesis:** Spring AI feeds ticket timeline to the LLM.
4. **Draft Creation:** Markdown is saved as an unpublished Draft Article.

## 3. Security & Access Control
Security is handled via stateless **JSON Web Tokens (JWT)**.

* **Authentication:** Users authenticate via login endpoint, receiving a JWT in an HttpOnly cookie.
* **Workspace Authorization:**
  * JWT includes `workspace_id` claim scoping all API operations.
  * Hibernate `@Filter` + AOP provides defense-in-depth data isolation.
  * Workspace-scoped roles: `ADMIN`, `MEMBER`, `READ_ONLY`.
* **Global RBAC:**
  * `ROLE_USER`: Standard access to AI Assistant and ticket escalation.
  * `ROLE_AGENT`: Access to Agent Dashboard and ticket resolution.
  * `ROLE_ADMIN`: Full CRUD over tickets, users, workspaces, and system settings.
