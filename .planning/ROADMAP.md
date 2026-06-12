# Roadmap: Shift-Left Knowledge Hub

## Milestones

- ✅ **v1.0 Initial MVP** — Phases 1-8 (shipped 2026-06-08)
- 🔷 **v2.0 Workspace Platform** — Phases 9-12 (in planning)

## Phases

<details>
<summary>✅ v1.0 Initial MVP (Phases 1-8) — SHIPPED 2026-06-08</summary>

- [x] Phase 1: Foundation (4/4 plans) — completed 2026-05-31
- [x] Phase 2: Knowledge Base (4/4 plans) — completed 2026-06-01
- [x] Phase 3: AI Self-Service Portal (4/4 plans) — completed 2026-06-02
- [x] Phase 4: Escalation & Ticketing (4/4 plans) — completed 2026-06-03
- [x] Phase 5: Agent Dashboard (4/4 plans) — completed 2026-06-04
- [x] Phase 6: KCS Auto-Drafting & Admin Review (3/3 plans) — completed 2026-06-05
- [x] Phase 7: Quality, Polish & DevOps (9/9 plans) — completed 2026-06-06
- [x] Phase 8: Testing & CI/CD (8/8 plans) — completed 2026-06-08

</details>

<details>
<summary>🔷 v2.0 Workspace Platform (Phases 9-12) — IN PLANNING</summary>

- [x] **Phase 9: Workspace Foundation** — Multi-tenant workspace isolation with data model, JWT claims, Hibernate filters, and pgvector metadata filtering
- [x] **Phase 10: Document Ingestion + BYO LLM** — Upload documents (markdown/text/PDF) via drag-and-drop with async ETL pipeline and per-workspace LLM configuration
- [x] **Phase 11: Domain-Agnostic AI** — Customizable taxonomy, system prompts, and unified hybrid search across articles and document chunks
- [ ] **Phase 12: Workspace Management UI** — Workspace switcher, member invitation with roles, admin panel, and workspace lifecycle

</details>

## Phase Details

### Phase 9: Workspace Foundation
**Goal**: Multi-tenant workspace isolation is established — users can create workspaces, all domain data is scoped by workspace_id, and existing v1.0 data is migrated to a default workspace
**Depends on**: Nothing (v1.0 shipped)
**Requirements**: WSF-01, WSF-02, WSF-03, WSF-04, WSF-05, WSF-06
**Success Criteria** (what must be TRUE):
  1. User can create a workspace with a name, and the system auto-generates a URL-safe slug
  2. All existing domain tables (articles, tickets, tags, AI chat transcripts) carry a workspace_id FK and all repository queries are automatically filtered by the active workspace via Hibernate @Filter + AOP
  3. JWT authentication includes a workspace_id claim, scoping all API operations to the user's current workspace
  4. pgvector similarity searches (hybrid search, RAG) only return vector results from the active workspace via metadata filtering
  5. Existing v1.0 data is migrated to a "Default Workspace" on first startup and remains fully accessible
**Plans**: 4 plans in 3 waves

Plans:
- [ ] 09-01-PLAN.md — Database schema + JPA entities (Flyway V3, Workspace, WorkspaceAwareEntity)
- [ ] 09-02-PLAN.md — Security + AOP + Vector filtering (JWT claims, ContextHolder, FilterAspect)
- [ ] 09-03-PLAN.md — Migration + Admin API (Default Workspace seeder, AdminWorkspaceController)
- [ ] 09-04-PLAN.md — Admin workspace frontend (list, create, assign users)

### Phase 10: Document Ingestion + BYO LLM
**Goal**: Users can upload documents to be processed by an async Spring AI ETL pipeline and configure their own OpenAI-compatible LLM endpoint per workspace
**Depends on**: Phase 9
**Requirements**: DOC-01, DOC-02, DOC-03, DOC-04, DOC-05, LLM-01, LLM-02, LLM-03, LLM-04, LLM-05
**Success Criteria** (what must be TRUE):
  1. User can drag-and-drop markdown, plain text, and PDF files for upload, triggering automatic text extraction, chunking, and embedding via Spring AI ETL pipeline
  2. Upload processing status is visible through 5 stages (UPLOADED → PARSING → CHUNKING → EMBEDDING → READY/FAILED) with user-facing progress tracking
  3. Duplicate content (matching SHA-256 content hash) is detected and reported; user can reprocess a single document after fixing the source file
  4. Workspace admin can configure an OpenAI-compatible endpoint URL with API key encrypted at rest (Spring TextEncryptor), and test connectivity before saving
  5. Workspace admin selects a model name; all AI chat sessions use the workspace's configured ChatModel via a cached WorkspaceChatModelRegistry
**Plans**: 4 plans in 3 waves
**UI hint**: yes

Plans:
- [x] 10-01-PLAN.md — Database schema + JPA entities (Flyway V4, Document, DocumentChunk, WorkspaceLlmConfig)
- [x] 10-02-PLAN.md — Document upload + async ETL pipeline (event-driven)
- [x] 10-03-PLAN.md — Workspace LLM config + ChatModelRegistry
- [x] 10-04-PLAN.md — Frontend admin document management + LLM config UI

### Phase 11: Domain-Agnostic AI
**Goal**: The AI assistant becomes domain-agnostic — workspaces define their own taxonomy, customize system prompts, and search across both articles and document chunks
**Depends on**: Phase 10
**Requirements**: DOM-01, DOM-02, DOM-03, DOM-04, DOM-05
**Success Criteria** (what must be TRUE):
  1. Workspace admin can define a category/subcategory taxonomy that labels documents and articles for domain organization
  2. Documents and articles display domain metadata (category, subcategory) in their views and search results
  3. Workspace admin can customize the AI assistant's system prompt using template variables ({workspace_name}, {domain}) for domain-appropriate responses
  4. Hybrid search returns unified RRF-ranked results from both KB articles and ingested document chunks in a single result set
  5. All AI queries and similarity searches are scoped exclusively to the active workspace's knowledge base
**Plans**: 6 plans in 4 waves

Plans:
- [x] 11-01-PLAN.md — Database schema + JPA entities (Flyway V5, Category entity, category_id FKs, system_prompt column, doc chunk tsvector)
- [x] 11-02-PLAN.md — Category admin API + service (AdminCategoryController, CRUD, merge, reassign)
- [x] 11-03-PLAN.md — System prompt + AI customization (template variables, buildPrompt refactor)
- [x] 11-04-PLAN.md — Unified hybrid search (document chunk FTS + vector, three-way RRF merge)
- [x] 11-05-PLAN.md — Taxonomy management UI (tree view, article/doc selectors, bulk management)
- [x] 11-06-PLAN.md — AI customization UI + search display (system prompt editor, category badges, doc citations)

### Phase 12: Workspace Management UI
**Goal**: Users can manage their workspaces end-to-end — switch between workspaces, invite members, administer settings, and handle lifecycle operations
**Depends on**: Phase 11
**Requirements**: WSM-01, WSM-02, WSM-03, WSM-04, WSM-05
**Success Criteria** (what must be TRUE):
  1. User can switch between workspaces via a dropdown in the navigation bar; all UI state (articles, chats, documents, settings) reloads for the selected workspace
  2. Workspace admin can invite new members in-app (no email) with role selection (admin / member / read-only); invited users see and can respond to pending invitations
  3. Workspace-scoped roles are enforced in the UI: read-only users cannot create/edit content, members have standard access, admins can manage workspace settings
  4. Workspace admin panel provides a unified interface for managing members (roles, removal), LLM configuration, and document management with a tabbed detail page
  5. Users can leave a workspace with confirmation; workspace admin can delete a workspace with soft-delete and type-to-confirm
**Plans**: 5 plans in 2 waves
**UI hint**: yes

Plans:
- [ ] 12-01-PLAN.md — Backend data model + migration (Flyway V6, workspace_invitation table, workspace.deleted_at/icon)
- [ ] 12-02-PLAN.md — Backend API endpoints (workspace update/delete, invitations, member mgmt, role, leave, mine)
- [ ] 12-03-PLAN.md — Frontend foundation (WorkspaceRoleService, workspace service extensions, models, translation keys)
- [ ] 12-04-PLAN.md — Workspace switcher + invitation badge (header dropdown, workspace icons, accept/reject UI)
- [ ] 12-05-PLAN.md — Admin detail page + lifecycle (tabbed workspace page, members/LLM/docs/settings, icon picker, delete/leave)

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|---------------|--------|-----------|
| 1. Foundation | v1.0 | 4/4 | Complete | 2026-05-31 |
| 2. Knowledge Base | v1.0 | 4/4 | Complete | 2026-06-01 |
| 3. AI Self-Service Portal | v1.0 | 4/4 | Complete | 2026-06-02 |
| 4. Escalation & Ticketing | v1.0 | 4/4 | Complete | 2026-06-03 |
| 5. Agent Dashboard | v1.0 | 4/4 | Complete | 2026-06-04 |
| 6. KCS Auto-Drafting & Admin Review | v1.0 | 3/3 | Complete | 2026-06-05 |
| 7. Quality, Polish & DevOps | v1.0 | 9/9 | Complete | 2026-06-06 |
| 8. Testing & CI/CD | v1.0 | 8/8 | Complete | 2026-06-08 |
| 9. Workspace Foundation | v2.0 | 4/4 | Complete | 2026-06-10 |
| 10. Document Ingestion + BYO LLM | v2.0 | 4/4 | Complete | 2026-06-10 |
| 11. Domain-Agnostic AI | v2.0 | 6/6 | Complete | 2026-06-12 |
| 12. Workspace Management UI | v2.0 | — | Planned | - |

---

*Last updated: 2026-06-12 — Phase 12 planned (5 plans in 2 waves)*
