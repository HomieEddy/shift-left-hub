-- V12: Comprehensive schema repair for production databases
-- where Flyway was disabled and Hibernate DDL created an incomplete schema.
-- All operations are idempotent (IF NOT EXISTS, OR REPLACE, DROP IF EXISTS).

-- ============================================================
-- FUNCTIONS & TRIGGERS (CRITICAL — required at runtime)
-- ============================================================

-- Article tsvector trigger (includes excerpt_fr for bilingual FTS)
CREATE OR REPLACE FUNCTION update_article_tsv()
RETURNS TRIGGER AS $$
BEGIN
    NEW.tsv_en := to_tsvector('english', COALESCE(NEW.title_en, '') || ' ' || COALESCE(NEW.content_en, ''));
    NEW.tsv_fr := to_tsvector('french', COALESCE(NEW.title_fr, '') || ' ' || COALESCE(NEW.content_fr, '') || ' ' || COALESCE(NEW.excerpt_fr, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_article_tsv ON article;
CREATE TRIGGER trigger_article_tsv
    BEFORE INSERT OR UPDATE OF title_en, content_en, title_fr, content_fr, excerpt_fr
    ON article
    FOR EACH ROW EXECUTE FUNCTION update_article_tsv();

-- Document chunk tsvector trigger
CREATE OR REPLACE FUNCTION update_document_chunk_tsv()
RETURNS TRIGGER AS $$
BEGIN
    NEW.tsv_content := to_tsvector('english', COALESCE(NEW.content, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_document_chunk_tsv ON document_chunk;
CREATE TRIGGER trg_document_chunk_tsv
    BEFORE INSERT OR UPDATE ON document_chunk
    FOR EACH ROW EXECUTE FUNCTION update_document_chunk_tsv();

-- ============================================================
-- FTS GIN INDEXES (CRITICAL — required for full-text search)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_article_tsv_en ON article USING GIN (tsv_en);
CREATE INDEX IF NOT EXISTS idx_article_tsv_fr ON article USING GIN (tsv_fr);
CREATE INDEX IF NOT EXISTS idx_document_chunk_tsv ON document_chunk USING GIN (tsv_content);

-- ============================================================
-- VECTOR INDEXES (HIGH — prevents full table scans on vector search)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_vector_store_embedding ON vector_store USING ivfflat (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_document_chunk_embedding ON document_chunk USING ivfflat (embedding vector_cosine_ops);

-- ============================================================
-- FOREIGN KEY CONSTRAINTS (MEDIUM — data integrity)
-- ============================================================

-- Workspace FKs
ALTER TABLE workspace DROP CONSTRAINT IF EXISTS fk_workspace_created_by;
ALTER TABLE workspace ADD CONSTRAINT fk_workspace_created_by FOREIGN KEY (created_by) REFERENCES app_user(id);

-- Workspace member FKs
ALTER TABLE workspace_member DROP CONSTRAINT IF EXISTS fk_wsmember_workspace;
ALTER TABLE workspace_member ADD CONSTRAINT fk_wsmember_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id);
ALTER TABLE workspace_member DROP CONSTRAINT IF EXISTS fk_wsmember_user;
ALTER TABLE workspace_member ADD CONSTRAINT fk_wsmember_user FOREIGN KEY (user_id) REFERENCES app_user(id);

-- Entity workspace FKs (columns from WorkspaceAwareEntity, not @ManyToOne)
ALTER TABLE article DROP CONSTRAINT IF EXISTS fk_article_workspace;
ALTER TABLE article ADD CONSTRAINT fk_article_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id);
ALTER TABLE ticket DROP CONSTRAINT IF EXISTS fk_ticket_workspace;
ALTER TABLE ticket ADD CONSTRAINT fk_ticket_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id);
ALTER TABLE tag DROP CONSTRAINT IF EXISTS fk_tag_workspace;
ALTER TABLE tag ADD CONSTRAINT fk_tag_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id);
ALTER TABLE work_note DROP CONSTRAINT IF EXISTS fk_work_note_workspace;
ALTER TABLE work_note ADD CONSTRAINT fk_work_note_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id);
ALTER TABLE app_user DROP CONSTRAINT IF EXISTS fk_user_default_workspace;
ALTER TABLE app_user ADD CONSTRAINT fk_user_default_workspace FOREIGN KEY (default_workspace_id) REFERENCES workspace(id);
ALTER TABLE document DROP CONSTRAINT IF EXISTS fk_document_workspace;
ALTER TABLE document ADD CONSTRAINT fk_document_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id);
ALTER TABLE workspace_llm_config DROP CONSTRAINT IF EXISTS fk_wllm_workspace;
ALTER TABLE workspace_llm_config ADD CONSTRAINT fk_wllm_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id);
ALTER TABLE category DROP CONSTRAINT IF EXISTS fk_category_workspace;
ALTER TABLE category ADD CONSTRAINT fk_category_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id);
ALTER TABLE workspace_invitation DROP CONSTRAINT IF EXISTS fk_wsinvitation_workspace;
ALTER TABLE workspace_invitation ADD CONSTRAINT fk_wsinvitation_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id);
ALTER TABLE workspace_invitation DROP CONSTRAINT IF EXISTS fk_wsinvitation_invited_user;
ALTER TABLE workspace_invitation ADD CONSTRAINT fk_wsinvitation_invited_user FOREIGN KEY (invited_user_id) REFERENCES app_user(id);
ALTER TABLE workspace_invitation DROP CONSTRAINT IF EXISTS fk_wsinvitation_invited_by;
ALTER TABLE workspace_invitation ADD CONSTRAINT fk_wsinvitation_invited_by FOREIGN KEY (invited_by) REFERENCES app_user(id);

-- Document chunk FK (with CASCADE)
ALTER TABLE document_chunk DROP CONSTRAINT IF EXISTS fk_dchunk_document;
ALTER TABLE document_chunk ADD CONSTRAINT fk_dchunk_document FOREIGN KEY (document_id) REFERENCES document(id) ON DELETE CASCADE;

-- ============================================================
-- ON DELETE SET NULL (MEDIUM — prevents FK violation on category delete)
-- ============================================================
ALTER TABLE article DROP CONSTRAINT IF EXISTS fk_article_category;
ALTER TABLE article ADD CONSTRAINT fk_article_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL;
ALTER TABLE document DROP CONSTRAINT IF EXISTS fk_document_category;
ALTER TABLE document ADD CONSTRAINT fk_document_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL;
ALTER TABLE category DROP CONSTRAINT IF EXISTS fk_category_parent;
ALTER TABLE category ADD CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category(id) ON DELETE SET NULL;

-- ============================================================
-- UNIQUE CONSTRAINTS (MEDIUM — data integrity)
-- ============================================================
ALTER TABLE workspace_llm_config DROP CONSTRAINT IF EXISTS uc_workspace_llm_config_workspace;
ALTER TABLE workspace_llm_config ADD CONSTRAINT uc_workspace_llm_config_workspace UNIQUE (workspace_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_document_workspace_hash ON document(workspace_id, content_hash);

-- ============================================================
-- PERFORMANCE INDEXES (MEDIUM — query performance)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_article_workspace ON article(workspace_id);
CREATE INDEX IF NOT EXISTS idx_ticket_workspace ON ticket(workspace_id);
CREATE INDEX IF NOT EXISTS idx_tag_workspace ON tag(workspace_id);
CREATE INDEX IF NOT EXISTS idx_work_note_workspace ON work_note(workspace_id);
CREATE INDEX IF NOT EXISTS idx_document_workspace ON document(workspace_id);
CREATE INDEX IF NOT EXISTS idx_category_workspace ON category(workspace_id);
CREATE INDEX IF NOT EXISTS idx_workspace_llm_config_workspace ON workspace_llm_config(workspace_id);
CREATE INDEX IF NOT EXISTS idx_ticket_status_category_urgency ON ticket(status, category, urgency);
CREATE INDEX IF NOT EXISTS idx_work_note_ticket_id ON work_note(ticket_id);
CREATE INDEX IF NOT EXISTS idx_article_status_published_at ON article(status, published_at);
CREATE INDEX IF NOT EXISTS idx_article_author_id ON article(author_id);
CREATE INDEX IF NOT EXISTS idx_document_status ON document(status);
CREATE INDEX IF NOT EXISTS idx_document_content_hash ON document(content_hash);
CREATE INDEX IF NOT EXISTS idx_document_chunk_document ON document_chunk(document_id);
CREATE INDEX IF NOT EXISTS idx_document_chunk_index ON document_chunk(document_id, chunk_index);
CREATE INDEX IF NOT EXISTS idx_category_parent ON category(parent_id);
CREATE INDEX IF NOT EXISTS idx_article_category ON article(category_id);
CREATE INDEX IF NOT EXISTS idx_document_category ON document(category_id);
CREATE INDEX IF NOT EXISTS idx_wsinvitation_workspace ON workspace_invitation(workspace_id);
CREATE INDEX IF NOT EXISTS idx_wsinvitation_invited_user ON workspace_invitation(invited_user_id);
CREATE INDEX IF NOT EXISTS idx_wsinvitation_invited_by ON workspace_invitation(invited_by);
CREATE INDEX IF NOT EXISTS idx_ticket_user_id ON ticket(user_id);
CREATE INDEX IF NOT EXISTS idx_ticket_assigned_to ON ticket(assigned_to_id);
CREATE INDEX IF NOT EXISTS idx_ticket_resolved_by ON ticket(resolved_by_id);
CREATE INDEX IF NOT EXISTS idx_work_note_author ON work_note(author_id);
CREATE INDEX IF NOT EXISTS idx_workspace_created_by ON workspace(created_by);
CREATE INDEX IF NOT EXISTS idx_user_role ON app_user(role);
CREATE INDEX IF NOT EXISTS idx_used_refresh_token_expires_at ON used_refresh_token(expires_at);
CREATE INDEX IF NOT EXISTS idx_wsinvitation_status ON workspace_invitation(status);
CREATE INDEX IF NOT EXISTS idx_workspace_deleted_at ON workspace(deleted_at);

-- ============================================================
-- BACKFILL EXISTING DATA
-- ============================================================
UPDATE article SET title_en = title_en;
UPDATE document_chunk SET content = content;
