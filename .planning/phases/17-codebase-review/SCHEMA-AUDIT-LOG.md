# Database Schema Audit Log — Phase 17 Codebase Review

**Date:** 2026-06-14
**Review method:** D-04 dual approach (query-pattern analysis + schema checklist)
**Migrations reviewed:** V1 through V7 (V8 created for fixes)

---

## Part A: Query-Pattern Analysis

| # | Repository | Method | Query Pattern | Index Used | Status |
|---|-----------|--------|---------------|------------|--------|
| 1 | `ArticleRepository` | `findBySlug` | `slug = ?` | UNIQUE(slug) | ✅ Covered |
| 2 | | `findByIdAndWorkspaceId` | `id = ? AND workspace_id = ?` | PK + idx_article_workspace | ✅ Covered |
| 3 | | `findByStatus` | `status = ?` | idx_article_status_published_at | ✅ Covered |
| 4 | | `findByStatusAndWorkspaceId` | `status = ? AND workspace_id = ?` | Composite index partial | ✅ Adequate* |
| 5 | | `searchByText` | Native FTS query | GIN(tsv_en), GIN(tsv_fr) | ✅ Covered |
| 6 | | `findBySourceTicketId` | `source_ticket_id = ?` | UNIQUE(source_ticket_id) | ✅ Covered |
| 7 | | `findByCategoryId` | `category_id = ?` | idx_article_category | ✅ Covered |
| 8 | | `countByCategoryId` | `category_id = ?` | idx_article_category | ✅ Covered |
| 9 | | `findPublishedTagFacets` | JOIN article_tag + tag | PK index + PK index | ✅ Covered |
| 10 | `TicketRepository` | `findByUserIdOrderByCreatedAtDesc` | `user_id = ?` | **MISSING** → V8: idx_ticket_user_id | ✅ Fixed |
| 11 | | `findByUserIdAndStatusOrderByCreatedAtDesc` | `user_id = ? AND status = ?` | **MISSING** → V8: idx_ticket_user_id | ✅ Fixed |
| 12 | | `countByStatus` | `status = ?` | idx_ticket_status_category_urgency | ✅ Covered |
| 13 | `UserRepository` | `findByEmail` | `email = ?` | UNIQUE(email) | ✅ Covered |
| 14 | | `existsByEmail` | `email = ?` | UNIQUE(email) | ✅ Covered |
| 15 | | `findByRole` | `role = ?` | idx_user_role | ✅ Covered |
| 16 | | `findUsersByWorkspaceId` | JOIN workspace_member | Composite PK | ✅ Covered |
| 17 | `TagRepository` | `findByNameEnIn` | `name_en IN (?)` | **MISSING** → Minor (small table) | ⚠️ Noted |
| 18 | `DocumentRepository` | `findByWorkspaceIdOrderByCreatedAtDesc` | `workspace_id = ?` | idx_document_workspace | ✅ Covered |
| 19 | | `findByWorkspaceIdAndContentHashAndStatus` | multi-field lookup | idx_document_workspace_hash (UNIQUE) | ✅ Covered |
| 20 | | `findByWorkspaceIdAndStatus` | `workspace_id = ? AND status = ?` | idx_document_workspace + idx_document_status | ✅ Adequate |
| 21 | | `findByCategoryId` | `category_id = ?` | idx_document_category | ✅ Covered |
| 22 | `DocumentChunkRepository` | `findByDocumentIdOrderByChunkIndexAsc` | `document_id = ?` | idx_document_chunk_document + idx_document_chunk_index | ✅ Covered |
| 23 | | `ftsSearch` | Native FTS | GIN(tsv_content) | ✅ Covered |
| 24 | | `vectorSearch` | Embedding <=> | **MISSING** → V8: idx_document_chunk_embedding | ✅ Fixed |
| 25 | `CategoryRepository` | `findByWorkspaceIdOrderByNameEnAsc` | `workspace_id = ?` | idx_category_workspace | ✅ Covered |
| 26 | | `findByParentId` | `parent_id = ?` | idx_category_parent | ✅ Covered |
| 27 | | `existsByParentId` | `parent_id = ?` | idx_category_parent | ✅ Covered |
| 28 | `WorkspaceRepository` | `findBySlug` | `slug = ?` | UNIQUE(slug) | ✅ Covered |
| 29 | | `existsBySlug` | `slug = ?` | UNIQUE(slug) | ✅ Covered |
| 30 | | `findAllByDeletedAtIsNull` | `deleted_at IS NULL` | idx_workspace_deleted_at | ✅ Covered |
| 31 | `WorkspaceMemberRepository` | `findByIdWorkspaceId` | `workspace_id = ?` | Composite PK prefix | ✅ Covered |
| 32 | | `findByIdUserId` | `user_id = ?` | Composite PK suffix | ✅ Covered |
| 33 | | `findByIdWorkspaceIdAndIdUserId` | composite PK lookup | Composite PK | ✅ Covered |
| 34 | `WorkspaceInvitationRepository` | `findByWorkspaceId` | `workspace_id = ?` | idx_wsinvitation_workspace | ✅ Covered |
| 35 | | `findByInvitedUserIdAndStatus` | `invited_user_id = ? AND status = ?` | idx_wsinvitation_invited_user + idx_wsinvitation_status | ✅ Adequate |
| 36 | | `findByInvitedUserId` | `invited_user_id = ?` | idx_wsinvitation_invited_user | ✅ Covered |
| 37 | | `findByIdAndWorkspaceId` | `id = ? AND workspace_id = ?` | PK + idx_wsinvitation_workspace | ✅ Covered |
| 38 | | `countByWorkspaceIdAndStatus` | `workspace_id = ? AND status = ?` | idx_wsinvitation_workspace + idx_wsinvitation_status | ✅ Adequate |
| 39 | `WorkNoteRepository` | `findByTicketIdOrderByCreatedAtDesc` | `ticket_id = ?` | idx_work_note_ticket_id | ✅ Covered |
| 40 | `WorkspaceLlmConfigRepository` | `findByWorkspaceId` | `workspace_id = ?` | idx_workspace_llm_config_workspace | ✅ Covered |
| 41 | `UsedRefreshTokenRepository` | `findByTokenId` | `token_id = ?` | UNIQUE(token_id) | ✅ Covered |
| 42 | | `deleteByExpiresAtBefore` | `expires_at < ?` | idx_used_refresh_token_expires_at | ✅ Covered |
| 43 | `AiConfigRepository` | `findSingleConfig` | `ORDER BY id LIMIT 1` | PK | ✅ Covered |

*\* Composite index (status, workspace_id) would be more efficient but single-column indexes are adequate.*

---

## Part B: Schema Checklist

### 1. FK Column Indexes

| FK Column | Referenced Table | Index Exists | Status |
|-----------|-----------------|--------------|--------|
| article.author_id | app_user | `idx_article_author_id` | ✅ |
| article.last_editor_id | app_user | None | ⚠️ Not queried directly |
| article.workspace_id | workspace | `idx_article_workspace` | ✅ |
| article.source_ticket_id | article (self) | UNIQUE(source_ticket_id) | ✅ |
| article.category_id | category | `idx_article_category` | ✅ |
| ticket.user_id | app_user | V8: `idx_ticket_user_id` | ✅ Fixed |
| ticket.assigned_to_id | app_user | V8: `idx_ticket_assigned_to` | ✅ Fixed |
| ticket.resolved_by_id | app_user | V8: `idx_ticket_resolved_by` | ✅ Fixed |
| ticket.workspace_id | workspace | `idx_ticket_workspace` | ✅ |
| work_note.ticket_id | ticket | `idx_work_note_ticket_id` | ✅ |
| work_note.author_id | app_user | V8: `idx_work_note_author` | ✅ Fixed |
| work_note.workspace_id | workspace | `idx_work_note_workspace` | ✅ |
| document.workspace_id | workspace | `idx_document_workspace` | ✅ |
| document.category_id | category | `idx_document_category` | ✅ |
| document_chunk.document_id | document | `idx_document_chunk_document` | ✅ |
| workspace.created_by | app_user | V8: `idx_workspace_created_by` | ✅ Fixed |
| workspace_member.workspace_id | workspace | Composite PK | ✅ |
| workspace_member.user_id | app_user | Composite PK | ✅ |
| workspace_invitation.workspace_id | workspace | `idx_wsinvitation_workspace` | ✅ |
| workspace_invitation.invited_user_id | app_user | `idx_wsinvitation_invited_user` | ✅ |
| workspace_invitation.invited_by | app_user | V8: `idx_wsinvitation_invited_by` | ✅ Fixed |
| tag.workspace_id | workspace | `idx_tag_workspace` | ✅ |
| category.workspace_id | workspace | `idx_category_workspace` | ✅ |
| category.parent_id | category (self) | `idx_category_parent` | ✅ |
| workspace_llm_config.workspace_id | workspace | `idx_workspace_llm_config_workspace` (UNIQUE) | ✅ |
| used_refresh_token.user_id | app_user | None | ⚠️ Small table, not queried by user_id |
| article_tag.article_id | article | Composite PK | ✅ |
| article_tag.tag_id | tag | Composite PK | ✅ |

### 2. JSONB/Text Columns Used in WHERE → GIN Index

| Column | Table | Used in WHERE | GIN Index | Status |
|--------|-------|--------------|-----------|--------|
| shift_left_context | ticket | Not directly in repository queries (stored/retrieved) | Not needed | ✅ |
| metadata | vector_store | Used by Spring AI PgVectorStore internally | Handled by Spring AI | ✅ |

### 3. tsvector + GIN Index

| Column | Table | GIN Index | Status |
|--------|-------|-----------|--------|
| tsv_en | article | `idx_article_tsv_en` (GIN) | ✅ |
| tsv_fr | article | `idx_article_tsv_fr` (GIN) | ✅ |
| tsv_content | document_chunk | `idx_document_chunk_tsv` (GIN) | ✅ |

**Trigger verification:**
- `update_article_tsv()` — Covers title_en + content_en (English) and title_fr + content_fr + excerpt_fr (French) ✅ (V8 fixed missing excerpt_fr)
- `update_document_chunk_tsv()` — Covers content (English) ✅

**Locale-aware FTS:** English uses `'english'` regconfig, French uses `'french'` regconfig ✅

### 4. pgvector Index

| Table | Column | Index Type | Status |
|-------|--------|-----------|--------|
| vector_store | embedding (768d) | ivfflat (vector_cosine_ops) — `idx_vector_store_embedding` | ✅ |
| document_chunk | embedding (768d) | V8: ivfflat (vector_cosine_ops) — `idx_document_chunk_embedding` | ✅ Fixed |

### 5. Normalization (3NF Check)

| Check | Finding | Status |
|-------|---------|--------|
| Duplicated data across tables | No user name stored in ticket — uses FK to app_user | ✅ 3NF |
| Non-key attributes depend on full PK | All tables have proper PK dependency | ✅ 3NF |
| JSONB for relational data | `ticket.shift_left_context` stores AI-generated context (document-shaped), not relational data | ✅ Appropriate |
| JSONB for relational data | `vector_store.metadata` stores arbitrary metadata (document-shaped) | ✅ Appropriate |
| Array columns for relational data | No array columns used — tags use join table | ✅ 3NF |

### 6. Unused Migrations

| Migration | Changes | Referenced by Entity | Status |
|-----------|---------|---------------------|--------|
| V1 | Baseline (8 tables + indexes + triggers) | All entities present | ✅ Active |
| V2 | used_refresh_token | UsedRefreshToken entity | ✅ Active |
| V3 | Workspace tables + workspace_id columns | Workspace, WorkspaceMember + updates | ✅ Active |
| V4 | Document management tables | Document, DocumentChunk, WorkspaceLlmConfig | ✅ Active |
| V5 | Taxonomy + domain + document_chunk FTS | Category entity + updates | ✅ Active |
| V6 | Invitations + soft-delete + icon | WorkspaceInvitation + Workspace updates | ✅ Active |
| V7 | excerpt_fr column | Article entity field | ✅ Active |

**Context correction:** The plan's context (interfaces section) listed `refresh_token, kcs_draft, article_category` as V1 tables, but these don't exist in the actual schema:
- Refresh token tracking uses `used_refresh_token` (V2) ✅
- KCS is handled via `article.source_ticket_id` (no separate KCS table needed) ✅
- Article-category relationship uses `article.category_id` directly (not a join table) ✅

---

## Summary of V8 Migration Changes

| # | Change | Type | Rationale |
|---|--------|------|-----------|
| 1 | Update `update_article_tsv()` to include excerpt_fr | Fix | Bug: French FTS didn't index excerpt_fr |
| 2 | Update trigger column list to include excerpt_fr | Fix | Trigger must fire when excerpt_fr changes |
| 3 | Backfill tsv_fr for existing rows with excerpt_fr | Fix | Existing data needs reindexing |
| 4 | `idx_ticket_user_id` on ticket(user_id) | Index | Primary query pattern in TicketRepository |
| 5 | `idx_ticket_assigned_to` on ticket(assigned_to_id) | Index | FK index best practice |
| 6 | `idx_ticket_resolved_by` on ticket(resolved_by_id) | Index | FK index best practice |
| 7 | `idx_work_note_author` on work_note(author_id) | Index | FK index best practice |
| 8 | `idx_workspace_created_by` on workspace(created_by) | Index | FK index best practice |
| 9 | `idx_wsinvitation_invited_by` on workspace_invitation(invited_by) | Index | FK index best practice |
| 10 | `idx_document_chunk_embedding` ivfflat on document_chunk(embedding) | Index | Missing vector search index |

## Minor Items (No Fix Needed)

- **tag.name_en** — `TagRepository.findByNameEnIn` queries by name_en without an index. Tag tables are small per workspace; index overhead not justified.
- **article.last_editor_id** — FK without direct query pattern. Not indexed. Acceptable for current usage.
- **used_refresh_token.user_id** — FK without direct query pattern. Table is append-only and periodically cleaned by TTL.
