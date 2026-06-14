# REV-04 Audit Report — Phase 17 Codebase Review

**Date:** 2026-06-14
**Review areas:** Frontend (Plan 17-01), Backend (Plan 17-02), Database (Plan 17-03)

## Summary

| Area | Issues Found | Fixed | TODO(REV-04) | GitHub Issues |
|------|-------------|-------|-------------|---------------|
| Frontend | 4 | 4 | 0 | 0 |
| Backend  | 0 | 0 | 0 | 0 |
| Database | 8 | 8 | 0 | 0 |
| **Total** | **12** | **12** | **0** | **0** |

**Resolution rate:** 100% — all issues fixed, zero deferred.

---

## Frontend Issues

### Issues Found & Fixed

1. **Unused Lucide icon imports in `app.component.ts`**
   - **Severity:** Build warning
   - **Impact:** 5 unused imports (ChevronsUpDown, Check, Building2, Bell, Loader2) — children import independently
   - **Fix:** Removed unused imports
   - **Commit:** `50463ed`
   - **Status:** ✅ Fixed

2. **Unused NgClass and RouterLink imports in `workspace-list.component.ts`**
   - **Severity:** Build warning
   - **Impact:** Unused Angular imports in admin component
   - **Fix:** Removed unused imports
   - **Commit:** `50463ed`
   - **Status:** ✅ Fixed

3. **Unused LucideLoader2 import in `invitation-badge.component.ts`**
   - **Severity:** Build warning
   - **Impact:** Unused icon import
   - **Fix:** Removed unused import
   - **Commit:** `50463ed`
   - **Status:** ✅ Fixed

4. **Optional chain on non-nullable type in `workspace-settings.component.html`**
   - **Severity:** Build diagnostic (NG8107)
   - **Impact:** `workspace?.name` where `workspace` is a required `@Input()` of non-nullable type
   - **Fix:** Changed to `workspace.name`
   - **Commit:** `50463ed`
   - **Status:** ✅ Fixed

### Unfixable Items

None.

### Pre-existing Items (Out of Scope)

- **Bundle budget warning (557 kB actual vs 500 kB configured):** Pre-existing, not caused by any issue found during the review. Not related to REV-04.

---

## Backend Issues

### Issues Found & Fixed

None. The backend codebase was clean across all audit dimensions:
- Checkstyle: 0 violations
- SpotBugs: 0 bugs
- Compilation: Clean
- Tests: 246/246 passing
- Dead code: None found
- Type safety: All generics and collections properly typed
- Security: No hardcoded secrets, BCrypt for passwords, AES/GCM for API keys
- Native SQL workspace isolation: All native queries explicitly filter by workspace_id

### Known Concerns (Already Mitigated)

- **Native SQL bypasses @Filter (from PROJECT.md):** All 5 native queries reviewed and verified to explicitly filter by `workspace_id = CAST(:workspaceId AS UUID)` in their WHERE clauses. The concern is documented and already properly mitigated. See 17-02-SUMMARY.md for full details.

---

## Database Issues

### Issues Found & Fixed (V8 Migration)

1. **French tsvector missing excerpt_fr** — `update_article_tsv()` trigger function did not include `excerpt_fr` in the French tsvector computation. French FTS queries would not match excerpt_fr content.
   - **Fix:** Updated `update_article_tsv()` to include `COALESCE(NEW.excerpt_fr, '')` in the French tsvector
   - **Also:** Updated trigger column list to fire on `excerpt_fr` changes
   - **Also:** Backfilled existing rows with excerpt_fr populated
   - **V8 Migration:** ✅ Fixed

2. **Missing FK index: `ticket.user_id`** — Primary query pattern in `TicketRepository.findByUserIdOrderByCreatedAtDesc` and `findByUserIdAndStatusOrderByCreatedAtDesc`. High-traffic user lookups.
   - **V8 Migration:** ✅ `CREATE INDEX IF NOT EXISTS idx_ticket_user_id`

3. **Missing FK index: `ticket.assigned_to_id`** — FK to app_user for agent assignment queries.
   - **V8 Migration:** ✅ `CREATE INDEX IF NOT EXISTS idx_ticket_assigned_to`

4. **Missing FK index: `ticket.resolved_by_id`** — FK to app_user for resolution audit queries.
   - **V8 Migration:** ✅ `CREATE INDEX IF NOT EXISTS idx_ticket_resolved_by`

5. **Missing FK index: `work_note.author_id`** — FK to app_user for work note author lookups.
   - **V8 Migration:** ✅ `CREATE INDEX IF NOT EXISTS idx_work_note_author`

6. **Missing FK index: `workspace.created_by`** — FK to app_user for workspace ownership queries.
   - **V8 Migration:** ✅ `CREATE INDEX IF NOT EXISTS idx_workspace_created_by`

7. **Missing FK index: `workspace_invitation.invited_by`** — FK to app_user for invitation audit.
   - **V8 Migration:** ✅ `CREATE INDEX IF NOT EXISTS idx_wsinvitation_invited_by`

8. **Missing ivfflat index: `document_chunk.embedding`** — `DocumentChunkRepository.vectorSearch()` uses cosine distance operator on embedding column but had no vector index. Performance-critical for document search at scale.
   - **V8 Migration:** ✅ `CREATE INDEX IF NOT EXISTS idx_document_chunk_embedding USING ivfflat (embedding vector_cosine_ops)`

### Minor Items (No Fix Required)

- **`tag.name_en`** — Used in `TagRepository.findByNameEnIn` without index. Tag tables are small per workspace; index overhead not justified.
- **`article.last_editor_id`** — FK to app_user without index. No direct query pattern uses this column.
- **`used_refresh_token.user_id`** — FK to app_user without index. Table is small (append-only with TTL cleanup), and no repository method queries by user_id.
- **`vector_store.metadata` JSONB** — No GIN index needed. The JSONB is used purely for storage by Spring AI PgVectorStore, not for custom queries.

### Context Correction

The plan's context description incorrectly listed `refresh_token`, `kcs_draft`, and `article_category` as V1 tables. Actual schema:
- Refresh token tracking → `used_refresh_token` (V2)
- KCS (Knowledge-Centered Support) → `article.source_ticket_id` (no separate table)
- Article-category relationship → direct `article.category_id` FK (no join table)

This is a factual correction — the actual schema is correct and all entity mappings are consistent.

---

## GitHub Issues Created

None. All 12 issues found across frontend, backend, and database were fixable and have been fixed. No cross-cutting items rose to the level of requiring GitHub Issue tracking:
- The PROJECT.md known gap (native SQL bypasses @Filter) is already mitigated in practice — all native queries explicitly filter by workspace_id
- No architectural changes, schema-breaking changes, or future-proofing concerns were identified that could not be addressed immediately

---

## Verification

- ✅ `grep -rn "TODO(REV-04)"` returns zero results — no unfixable issues documented in code
- ✅ Zero `// TODO(REV-04)` comments means all issues were either fixed immediately or not applicable
- ✅ All 12 issues fixed: 4 frontend, 0 backend, 8 database
- ✅ V8 migration documents all 8 database fixes (6 indexes, 1 trigger fix, 1 ivfflat index)

---

## Summary of Compliance

| Requirement | Status | Evidence |
|-------------|--------|----------|
| REV-01 (Frontend Review) | ✅ Complete | 17-01-SUMMARY.md — all 95+ TS/HTML files reviewed |
| REV-02 (Backend Review) | ✅ Complete | 17-02-SUMMARY.md — all 150+ Java files reviewed |
| REV-03 (Database Review) | ✅ Complete | SCHEMA-AUDIT-LOG.md — all 7 migrations + V8 reviewed |
| REV-04 (Issue Resolution) | ✅ Complete | This report — all 12 issues fixed |
