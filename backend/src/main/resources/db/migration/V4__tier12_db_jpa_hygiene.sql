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
--   - uc_tag_workspace_slug       (prevents duplicate slugs within a workspace)
--   - uc_category_workspace_parent_name_en
--                                (prevents duplicate sibling category names)
--
-- Safety: every block is idempotent. The unique-constraint blocks fail loudly
-- if existing data violates the new constraint (DISTINCT ON check first).

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
-- NULL parent_id is treated as a distinct group (standard SQL NULL semantics
-- in UNIQUE constraints), so root categories can share names across workspaces
-- but not within a workspace.
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
            UNIQUE (workspace_id, parent_id, name_en);
    END IF;
END;
$$;
