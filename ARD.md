# Shift-Left Knowledge Hub - Architecture Requirements Document (ARD v1.1)

## 1. Architectural Strategy: The Modular Monolith
To balance rapid development with enterprise-grade maintainability, the application will be structured as a **Modular Monolith**. The architecture strictly adheres to **SOLID**, **KISS**, and **YAGNI** principles.

* **Presentation Layer (Client):** Angular, RxJS, Tailwind CSS. A reactive SPA supporting an i18n module for seamless bilingual (English/French) toggling.
* **Application / API Layer (Server):** Spring Boot 3.x, Spring Security, JWT. Stateless RESTful APIs routing commands to domain services.
* **AI & Integration Layer:** Spring AI, LLM API (OpenAI/Local). Orchestrates Retrieval-Augmented Generation (RAG) workflows and KCS summarization.

## 2. Core System Data Flows

### A. The RxJS Search & RAG Pipeline
1.  **User Input:** User types query in Angular UI.
2.  **Debounce:** RxJS `debounceTime` limits API spam.
3.  **FTS Execution:** Spring Boot executes native PostgreSQL Full-Text Search.
4.  **Context Construction (Authenticated):** Top matches fed into Spring AI alongside user prompt.
5.  **Response:** LLM streams contextual answer back to Angular.

### B. The Ticket Escalation Pipeline
1.  **Trigger:** Authenticated user clicks "Escalate".
2.  **Payload Assembly:** Angular bundles form inputs + AI chat transcript.
3.  **Persistence:** Spring Boot saves a `Ticket` entity with "Shift-Left Context".
4.  **Notification:** Ticket enters the "NEW" queue for agents.

### C. The KCS Automated Drafting Pipeline
1.  **Trigger:** Agent resolves a ticket and flags as "Knowledge Gap".
2.  **Async Processing:** Spring Boot fires a background event.
3.  **AI Synthesis:** Spring AI feeds ticket timeline to the LLM.
4.  **Draft Creation:** Markdown is saved as an unpublished Draft Article.

## 3. Security & Access Control
Security is handled via stateless **JSON Web Tokens (JWT)**.

* **Unauthenticated (Anonymous):** Read-only access to published knowledge base articles (`GET /api/articles/**`). Cannot access AI chat or submit tickets.
* **Authentication:** Users and Admins authenticate via login endpoint, receiving a JWT.
* **Authorization (RBAC):**
    * `ROLE_USER`: Granted access to AI Assistant and permission to POST tickets.
    * `ROLE_ADMIN`: Granted full CRUD over tickets, users, and article drafts.