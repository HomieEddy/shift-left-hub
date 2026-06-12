-- V5: Add taxonomy (categories), domain metadata, system prompts, and document chunk FTS
-- Chained after V4__add_document_management.sql

-- 1. Categories table: self-referencing parent-child hierarchy, workspace-scoped, bilingual
CREATE TABLE IF NOT EXISTS category (
    id UUID NOT NULL,
    workspace_id UUID NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    name_fr VARCHAR(255) NOT NULL,
    parent_id UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_category PRIMARY KEY (id),
    CONSTRAINT fk_category_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_category_workspace ON category(workspace_id);
CREATE INDEX IF NOT EXISTS idx_category_parent ON category(parent_id);

-- 2. category_id FK on article (domain metadata)
ALTER TABLE article ADD COLUMN IF NOT EXISTS category_id UUID;
ALTER TABLE article ADD CONSTRAINT IF NOT EXISTS fk_article_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_article_category ON article(category_id);

-- 3. category_id FK on document (domain metadata)
ALTER TABLE document ADD COLUMN IF NOT EXISTS category_id UUID;
ALTER TABLE document ADD CONSTRAINT IF NOT EXISTS fk_document_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_document_category ON document(category_id);

-- 4. system_prompt on workspace_llm_config (customizable AI persona)
ALTER TABLE workspace_llm_config ADD COLUMN IF NOT EXISTS system_prompt TEXT;

-- 5. Document chunk tsvector for full-text search (persistent column + trigger + GIN index)
ALTER TABLE document_chunk ADD COLUMN IF NOT EXISTS tsv_content TSVECTOR;

CREATE OR REPLACE FUNCTION update_document_chunk_tsv()
RETURNS TRIGGER AS $$
BEGIN
    NEW.tsv_content := to_tsvector('english', COALESCE(NEW.content, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_document_chunk_tsv'
    ) THEN
        CREATE TRIGGER trigger_document_chunk_tsv
        BEFORE INSERT OR UPDATE OF content
        ON document_chunk
        FOR EACH ROW
        EXECUTE FUNCTION update_document_chunk_tsv();
    END IF;
END;
$$;

CREATE INDEX IF NOT EXISTS idx_document_chunk_tsv ON document_chunk USING GIN (tsv_content);