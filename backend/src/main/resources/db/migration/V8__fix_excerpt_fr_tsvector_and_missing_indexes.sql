-- V8: Fix excerpt_fr tsvector inclusion, add missing indexes
-- Chained after V7__add_excerpt_fr.sql
--
-- Fixes:
-- 1. Update article tsvector trigger to include excerpt_fr in French FTS
-- 2. Add missing FK indexes on ticket, work_note, workspace, workspace_invitation
-- 3. Add ivfflat index on document_chunk.embedding for vector search

-- ============================================================
-- 1. Fix: Include excerpt_fr in French tsvector trigger
-- ============================================================
-- The original update_article_tsv() function did not include excerpt_fr
-- in the French tsvector. This meant French FTS queries would not match
-- content from excerpt_fr.

CREATE OR REPLACE FUNCTION update_article_tsv()
RETURNS TRIGGER AS $$
BEGIN
    NEW.tsv_en := to_tsvector('english', COALESCE(NEW.title_en, '') || ' ' || COALESCE(NEW.content_en, ''));
    NEW.tsv_fr := to_tsvector('french', COALESCE(NEW.title_fr, '') || ' ' || COALESCE(NEW.content_fr, '') || ' ' || COALESCE(NEW.excerpt_fr, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop and recreate the trigger to include excerpt_fr in column list
DROP TRIGGER IF EXISTS trigger_article_tsv ON article;

CREATE TRIGGER trigger_article_tsv
BEFORE INSERT OR UPDATE OF title_en, content_en, title_fr, content_fr, excerpt_fr
ON article
FOR EACH ROW
EXECUTE FUNCTION update_article_tsv();

-- Backfill: update tsv_fr for existing rows that have excerpt_fr set
-- This re-computes the French tsvector including excerpt_fr content.
-- We use the trigger arms (UPDATE OF excerpt_fr) to compute it automatically:
UPDATE article
SET excerpt_fr = excerpt_fr
WHERE excerpt_fr IS NOT NULL;

-- ============================================================
-- 2. Add missing FK indexes for query performance
-- ============================================================

-- ticket.user_id: Used in TicketRepository.findByUserIdOrderByCreatedAtDesc
-- and findByUserIdAndStatusOrderByCreatedAtDesc — primary user ticket lookup.
CREATE INDEX IF NOT EXISTS idx_ticket_user_id ON ticket(user_id);

-- ticket.assigned_to_id: FK to app_user, used in agent assignment queries.
CREATE INDEX IF NOT EXISTS idx_ticket_assigned_to ON ticket(assigned_to_id);

-- ticket.resolved_by_id: FK to app_user, used in resolution audit queries.
CREATE INDEX IF NOT EXISTS idx_ticket_resolved_by ON ticket(resolved_by_id);

-- work_note.author_id: FK to app_user, used in note author lookup.
CREATE INDEX IF NOT EXISTS idx_work_note_author ON work_note(author_id);

-- workspace.created_by: FK to app_user, used in workspace ownership queries.
CREATE INDEX IF NOT EXISTS idx_workspace_created_by ON workspace(created_by);

-- workspace_invitation.invited_by: FK to app_user, used in invitation audit queries.
CREATE INDEX IF NOT EXISTS idx_wsinvitation_invited_by ON workspace_invitation(invited_by);

-- ============================================================
-- 3. Add ivfflat index on document_chunk.embedding for vector search
-- ============================================================
-- document_chunk.embedding: Used in DocumentChunkRepository.vectorSearch()
-- with cosine distance operator (<=>). An ivfflat index accelerates the
-- approximate nearest neighbor search at scale.
CREATE INDEX IF NOT EXISTS idx_document_chunk_embedding
    ON document_chunk USING ivfflat (embedding vector_cosine_ops);
