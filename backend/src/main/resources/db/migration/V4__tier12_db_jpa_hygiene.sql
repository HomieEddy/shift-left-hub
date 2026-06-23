-- V4: Tier 12 — DB/JPA hygiene (post-cleanup).
--
-- Adds missing indexes and unique constraints that the JPA entities declare
-- via @Index / @UniqueConstraint but that V1__baseline.sql did not emit.
-- Each statement is wrapped in DO $$ ... IF NOT EXISTS for idempotency.
--
-- Indexes:
--   - idx_ticket_user_id         (TicketService.getUserTickets: findByUserIdOrderByCreatedAtDesc)
--   - idx_ticket_assigned_to_id  (AgentTicketService: agent dashboard filter by assignee)
--   - idx_ticket_resolved_by_id  (AgentTicketService: resolved-by filter)
--   - idx_article_last_editor_id (admin article listing by last editor)
--
-- Unique constraints:
--   - uc_tag_workspace_name_en    (prevents duplicate tag names within a workspace)
--   - uc_category_workspace_parent_name_en
--                                (prevents duplicate sibling category names,
--                                 NULLS NOT DISTINCT so root categories are
--                                 also unique on (workspace_id, name_en))
--
-- Safety: every block is idempotent. The unique-constraint blocks delete
-- existing duplicates (keeping the lowest id) and remap dependent
-- article_tag rows to the canonical tag first.

-- ============================================================
-- INDEXES
-- ============================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_ticket_user_id'
    ) THEN
        CREATE INDEX idx_ticket_user_id ON ticket(user_id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_ticket_assigned_to_id'
    ) THEN
        CREATE INDEX idx_ticket_assigned_to_id ON ticket(assigned_to_id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_ticket_resolved_by_id'
    ) THEN
        CREATE INDEX idx_ticket_resolved_by_id ON ticket(resolved_by_id);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_article_last_editor_id'
    ) THEN
        CREATE INDEX idx_article_last_editor_id ON article(last_editor_id);
    END IF;
END;
$$;

-- ============================================================
-- UNIQUE CONSTRAINTS
-- ============================================================

-- Tag: (workspace_id, name_en) must be unique. Tags have no slug column —
-- the entity identifies them by name within a workspace. The audit's
-- recommended constraint was on a slug that doesn't exist, so the
-- constraint is on name_en instead (the user-facing identifier).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uc_tag_workspace_name_en'
    ) THEN
        -- Remap article_tag rows from duplicate tags to the canonical (lowest-id)
        -- tag BEFORE deleting the duplicates. article_tag.tag_id has no
        -- ON DELETE CASCADE, so a blind DELETE would fail with FK violation
        -- in production data. Uses a CTE to pick MIN(id) per (workspace_id,
        -- name_en) — PostgreSQL has no built-in MIN(uuid) aggregate, so we
        -- pick via an ordered subquery.
        WITH canonical AS (
            SELECT DISTINCT ON (workspace_id, name_en)
                id AS canonical_id,
                workspace_id,
                name_en
            FROM tag
            ORDER BY workspace_id, name_en, id
        )
        UPDATE article_tag at
        SET tag_id = c.canonical_id
        FROM tag dup
        JOIN canonical c
          ON c.workspace_id = dup.workspace_id
         AND c.name_en = dup.name_en
        WHERE at.tag_id = dup.id
          AND dup.id > c.canonical_id;

        DELETE FROM tag a
        USING tag b
        WHERE a.workspace_id = b.workspace_id
          AND a.name_en = b.name_en
          AND a.id > b.id;

        ALTER TABLE tag
            ADD CONSTRAINT uc_tag_workspace_name_en UNIQUE (workspace_id, name_en);
    END IF;
END;
$$;

-- Category: (workspace_id, parent_id, name_en) must be unique among siblings.
-- Root categories (parent_id IS NULL) must also be unique on (workspace_id,
-- name_en). Standard SQL UNIQUE treats NULLs as distinct, so we use
-- NULLS NOT DISTINCT (PostgreSQL 15+; AGENTS.md mandates pgvector 0.8.0-pg16,
-- i.e. PostgreSQL 16, so this syntax is available).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uc_category_workspace_parent_name_en'
    ) THEN
        DELETE FROM category a
        USING category b
        WHERE a.workspace_id = b.workspace_id
          AND a.parent_id IS NOT DISTINCT FROM b.parent_id
          AND a.name_en = b.name_en
          AND a.id > b.id;

        ALTER TABLE category
            ADD CONSTRAINT uc_category_workspace_parent_name_en
            UNIQUE NULLS NOT DISTINCT (workspace_id, parent_id, name_en);
    END IF;
END;
$$;
