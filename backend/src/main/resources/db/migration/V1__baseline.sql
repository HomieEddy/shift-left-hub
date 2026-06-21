-- V1: Consolidated baseline schema
-- This migration replaces the historical V1–V12 migration chain with a single
-- idempotent baseline. It is the result of squashing the schema evolution
-- that landed in:
--   V1  baseline
--   V2  used_refresh_token
--   V3  add_workspace_tables
--   V4  add_document_management
--   V5  add_taxonomy_and_domain
--   V6  add_workspace_invitation_and_icon
--   V7  add_excerpt_fr
--   V8  fix_excerpt_fr_tsvector_and_missing_indexes
--   V9  ensure_document_chunk_tsv
--   V10 align_vector_store_embedding_dimension
--   V11 repair_pgvector_vector_store
--   V12 comprehensive_schema_repair
--
-- All DDL is idempotent. On a fresh database, this creates the full schema.
-- On an existing database (where the V1–V12 chain already ran), Flyway's
-- companion V2 migration rewrites flyway_schema_history so this file is
-- recorded as a single V1 application without re-running the DDL.
--
-- Placeholders (set via spring.flyway.placeholders.*):
--   ${embeddingDimensions} — embedding vector width (default 768)

-- ============================================================
-- EXTENSIONS
-- ============================================================
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS vector;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'vector extension unavailable (pgvector may be pre-installed or insufficient privileges): %', SQLERRM;
END;
$$;

-- ============================================================
-- IDENTITY & AUTH
-- ============================================================
CREATE TABLE IF NOT EXISTS app_user (
    id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL,
    default_workspace_id UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_app_user PRIMARY KEY (id),
    CONSTRAINT uc_app_user_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS used_refresh_token (
    id UUID NOT NULL,
    token_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_used_refresh_token PRIMARY KEY (id),
    CONSTRAINT uc_used_refresh_token_token_id UNIQUE (token_id)
);

-- ============================================================
-- WORKSPACE
-- ============================================================
CREATE TABLE IF NOT EXISTS workspace (
    id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(128) NOT NULL,
    description TEXT,
    logo_url VARCHAR(512),
    icon VARCHAR(64),
    created_by UUID NOT NULL,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_workspace PRIMARY KEY (id),
    CONSTRAINT uc_workspace_slug UNIQUE (slug),
    CONSTRAINT fk_workspace_created_by FOREIGN KEY (created_by) REFERENCES app_user(id)
);

-- Backfill the FK from app_user.default_workspace_id now that the workspace
-- table exists. Originally added in V3__add_workspace_tables.sql.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_default_workspace'
    ) THEN
        ALTER TABLE app_user
            ADD CONSTRAINT fk_user_default_workspace
            FOREIGN KEY (default_workspace_id) REFERENCES workspace(id);
    END IF;
END;
$$;

CREATE TABLE IF NOT EXISTS workspace_member (
    workspace_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_workspace_member PRIMARY KEY (workspace_id, user_id),
    CONSTRAINT fk_wsmember_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id),
    CONSTRAINT fk_wsmember_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE TABLE IF NOT EXISTS workspace_invitation (
    id UUID NOT NULL,
    workspace_id UUID NOT NULL,
    invited_user_id UUID NOT NULL,
    invited_by UUID NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_workspace_invitation PRIMARY KEY (id),
    CONSTRAINT fk_wsinvitation_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id),
    CONSTRAINT fk_wsinvitation_invited_user FOREIGN KEY (invited_user_id) REFERENCES app_user(id),
    CONSTRAINT fk_wsinvitation_invited_by FOREIGN KEY (invited_by) REFERENCES app_user(id)
);

-- ============================================================
-- KNOWLEDGE BASE
-- ============================================================
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

CREATE TABLE IF NOT EXISTS tag (
    id UUID NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    name_fr VARCHAR(255) NOT NULL,
    color VARCHAR(255) NOT NULL,
    workspace_id UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_tag PRIMARY KEY (id)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tag_workspace'
    ) THEN
        ALTER TABLE tag
            ADD CONSTRAINT fk_tag_workspace
            FOREIGN KEY (workspace_id) REFERENCES workspace(id);
    END IF;
END;
$$;

CREATE TABLE IF NOT EXISTS article (
    id UUID NOT NULL,
    title_en VARCHAR(255) NOT NULL,
    content_en TEXT NOT NULL,
    title_fr VARCHAR(255),
    content_fr TEXT,
    slug VARCHAR(255),
    excerpt TEXT,
    excerpt_fr TEXT,
    featured_image VARCHAR(255),
    source_ticket_id UUID,
    status VARCHAR(255) NOT NULL,
    view_count INTEGER NOT NULL DEFAULT 0,
    published_at TIMESTAMP WITHOUT TIME ZONE,
    author_id UUID NOT NULL,
    last_editor_id UUID,
    workspace_id UUID,
    category_id UUID,
    tsv_en TSVECTOR,
    tsv_fr TSVECTOR,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_article PRIMARY KEY (id),
    CONSTRAINT uc_article_slug UNIQUE (slug),
    CONSTRAINT uc_article_source_ticket UNIQUE (source_ticket_id),
    CONSTRAINT fk_article_author FOREIGN KEY (author_id) REFERENCES app_user(id),
    CONSTRAINT fk_article_last_editor FOREIGN KEY (last_editor_id) REFERENCES app_user(id)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_article_workspace'
    ) THEN
        ALTER TABLE article
            ADD CONSTRAINT fk_article_workspace
            FOREIGN KEY (workspace_id) REFERENCES workspace(id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_article_category'
    ) THEN
        ALTER TABLE article
            ADD CONSTRAINT fk_article_category
            FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL;
    END IF;
END;
$$;

CREATE TABLE IF NOT EXISTS article_tag (
    article_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    CONSTRAINT pk_article_tag PRIMARY KEY (article_id, tag_id),
    CONSTRAINT fk_article_tag_article FOREIGN KEY (article_id) REFERENCES article(id),
    CONSTRAINT fk_article_tag_tag FOREIGN KEY (tag_id) REFERENCES tag(id)
);

-- ============================================================
-- TICKETS
-- ============================================================
CREATE TABLE IF NOT EXISTS ticket (
    id UUID NOT NULL,
    ticket_number VARCHAR(9) NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    urgency VARCHAR(255) NOT NULL,
    issue TEXT NOT NULL,
    shift_left_context JSONB,
    assigned_to_id UUID,
    resolved_by_id UUID,
    resolution_notes TEXT,
    is_knowledge_gap BOOLEAN DEFAULT FALSE,
    workspace_id UUID,
    resolved_at TIMESTAMP WITHOUT TIME ZONE,
    cancelled_at TIMESTAMP WITHOUT TIME ZONE,
    cancel_reason TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_ticket PRIMARY KEY (id),
    CONSTRAINT uc_ticket_number UNIQUE (ticket_number),
    CONSTRAINT fk_ticket_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT fk_ticket_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES app_user(id),
    CONSTRAINT fk_ticket_resolved_by FOREIGN KEY (resolved_by_id) REFERENCES app_user(id)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_ticket_workspace'
    ) THEN
        ALTER TABLE ticket
            ADD CONSTRAINT fk_ticket_workspace
            FOREIGN KEY (workspace_id) REFERENCES workspace(id);
    END IF;
END;
$$;

CREATE TABLE IF NOT EXISTS ticket_number_sequence (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    next_number INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT pk_ticket_number_sequence PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS work_note (
    id UUID NOT NULL,
    ticket_id UUID NOT NULL,
    author_id UUID NOT NULL,
    content TEXT NOT NULL,
    workspace_id UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_work_note PRIMARY KEY (id),
    CONSTRAINT fk_work_note_ticket FOREIGN KEY (ticket_id) REFERENCES ticket(id),
    CONSTRAINT fk_work_note_author FOREIGN KEY (author_id) REFERENCES app_user(id)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_work_note_workspace'
    ) THEN
        ALTER TABLE work_note
            ADD CONSTRAINT fk_work_note_workspace
            FOREIGN KEY (workspace_id) REFERENCES workspace(id);
    END IF;
END;
$$;

-- ============================================================
-- DOCUMENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS document (
    id UUID NOT NULL,
    workspace_id UUID NOT NULL,
    filename VARCHAR(512) NOT NULL,
    mime_type VARCHAR(128) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'UPLOADED',
    error_message TEXT,
    file_path VARCHAR(1024),
    file_size BIGINT NOT NULL DEFAULT 0,
    chunk_count INTEGER NOT NULL DEFAULT 0,
    category_id UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_document PRIMARY KEY (id),
    CONSTRAINT fk_document_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_document_category'
    ) THEN
        ALTER TABLE document
            ADD CONSTRAINT fk_document_category
            FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL;
    END IF;
END;
$$;

CREATE TABLE IF NOT EXISTS document_chunk (
    id UUID NOT NULL,
    document_id UUID NOT NULL,
    content TEXT NOT NULL,
    chunk_index INTEGER NOT NULL,
    embedding vector(${embeddingDimensions}),
    tsv_content TSVECTOR,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_document_chunk PRIMARY KEY (id),
    CONSTRAINT fk_dchunk_document FOREIGN KEY (document_id) REFERENCES document(id) ON DELETE CASCADE
);

-- ============================================================
-- AI
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_config (
    id UUID NOT NULL,
    llm_provider VARCHAR(255),
    ollama_endpoint_url VARCHAR(255),
    openai_api_key VARCHAR(255),
    chat_model_name VARCHAR(255),
    embedding_model_name VARCHAR(255),
    similarity_threshold DOUBLE PRECISION,
    embedding_dimension INTEGER,
    CONSTRAINT pk_ai_config PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS workspace_llm_config (
    id UUID NOT NULL,
    workspace_id UUID NOT NULL,
    llm_provider VARCHAR(32) NOT NULL DEFAULT 'OLLAMA',
    endpoint_url VARCHAR(512),
    api_key VARCHAR(512),
    model_name VARCHAR(128) NOT NULL DEFAULT 'llama3.2',
    embedding_model_name VARCHAR(128) NOT NULL DEFAULT 'nomic-embed-text',
    similarity_threshold DOUBLE PRECISION NOT NULL DEFAULT 0.65,
    embedding_dimension INTEGER NOT NULL DEFAULT 768,
    system_prompt TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_workspace_llm_config PRIMARY KEY (id),
    CONSTRAINT uc_workspace_llm_config_workspace UNIQUE (workspace_id),
    CONSTRAINT fk_wllm_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id)
);

-- Spring AI's vector_store table is normally created at runtime by Spring AI.
-- We declare it here so it is part of the baseline and the dimension matches
-- the configured embedding model.
CREATE TABLE IF NOT EXISTS vector_store (
    id UUID NOT NULL,
    content TEXT,
    metadata JSONB,
    embedding vector(${embeddingDimensions}),
    CONSTRAINT pk_vector_store PRIMARY KEY (id)
);

-- ============================================================
-- FUNCTIONS & TRIGGERS
-- ============================================================
-- Article tsvector: bilingual full-text search. Includes excerpt_fr so the
-- French index matches the editorial summary in addition to body content.
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

-- Document chunk tsvector: English FTS on extracted document text.
CREATE OR REPLACE FUNCTION update_document_chunk_tsv()
RETURNS TRIGGER AS $$
BEGIN
    NEW.tsv_content := to_tsvector('english', COALESCE(NEW.content, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_document_chunk_tsv ON document_chunk;
CREATE TRIGGER trigger_document_chunk_tsv
    BEFORE INSERT OR UPDATE OF content
    ON document_chunk
    FOR EACH ROW EXECUTE FUNCTION update_document_chunk_tsv();

-- ============================================================
-- INDEXES
-- ============================================================
-- Full-text search (GIN)
CREATE INDEX IF NOT EXISTS idx_article_tsv_en ON article USING GIN (tsv_en);
CREATE INDEX IF NOT EXISTS idx_article_tsv_fr ON article USING GIN (tsv_fr);
CREATE INDEX IF NOT EXISTS idx_document_chunk_tsv ON document_chunk USING GIN (tsv_content);

-- Vector search (ivfflat)
CREATE INDEX IF NOT EXISTS idx_vector_store_embedding
    ON vector_store USING ivfflat (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_document_chunk_embedding
    ON document_chunk USING ivfflat (embedding vector_cosine_ops);

-- Workspace scoping
CREATE INDEX IF NOT EXISTS idx_workspace_created_by ON workspace(created_by);
CREATE INDEX IF NOT EXISTS idx_workspace_deleted_at ON workspace(deleted_at);
CREATE INDEX IF NOT EXISTS idx_wsinvitation_workspace ON workspace_invitation(workspace_id);
CREATE INDEX IF NOT EXISTS idx_wsinvitation_invited_user ON workspace_invitation(invited_user_id);
CREATE INDEX IF NOT EXISTS idx_wsinvitation_invited_by ON workspace_invitation(invited_by);
CREATE INDEX IF NOT EXISTS idx_wsinvitation_status ON workspace_invitation(status);
CREATE INDEX IF NOT EXISTS idx_article_workspace ON article(workspace_id);
CREATE INDEX IF NOT EXISTS idx_ticket_workspace ON ticket(workspace_id);
CREATE INDEX IF NOT EXISTS idx_tag_workspace ON tag(workspace_id);
CREATE INDEX IF NOT EXISTS idx_work_note_workspace ON work_note(workspace_id);
CREATE INDEX IF NOT EXISTS idx_document_workspace ON document(workspace_id);
CREATE INDEX IF NOT EXISTS idx_category_workspace ON category(workspace_id);
CREATE INDEX IF NOT EXISTS idx_workspace_llm_config_workspace ON workspace_llm_config(workspace_id);

-- Category hierarchy and content classification
CREATE INDEX IF NOT EXISTS idx_category_parent ON category(parent_id);
CREATE INDEX IF NOT EXISTS idx_article_category ON article(category_id);
CREATE INDEX IF NOT EXISTS idx_document_category ON document(category_id);

-- Document integrity and lookup
CREATE UNIQUE INDEX IF NOT EXISTS idx_document_workspace_hash ON document(workspace_id, content_hash);
CREATE INDEX IF NOT EXISTS idx_document_status ON document(status);
CREATE INDEX IF NOT EXISTS idx_document_content_hash ON document(content_hash);
CREATE INDEX IF NOT EXISTS idx_document_chunk_document ON document_chunk(document_id);
CREATE INDEX IF NOT EXISTS idx_document_chunk_index ON document_chunk(document_id, chunk_index);

-- Article query paths
CREATE INDEX IF NOT EXISTS idx_article_status_published_at ON article(status, published_at);
CREATE INDEX IF NOT EXISTS idx_article_author_id ON article(author_id);
CREATE INDEX IF NOT EXISTS idx_article_category_lookup ON article(category_id);

-- Ticket query paths
CREATE INDEX IF NOT EXISTS idx_ticket_user_id ON ticket(user_id);
CREATE INDEX IF NOT EXISTS idx_ticket_assigned_to ON ticket(assigned_to_id);
CREATE INDEX IF NOT EXISTS idx_ticket_resolved_by ON ticket(resolved_by_id);
CREATE INDEX IF NOT EXISTS idx_ticket_status_category_urgency ON ticket(status, category, urgency);
CREATE INDEX IF NOT EXISTS idx_work_note_ticket_id ON work_note(ticket_id);
CREATE INDEX IF NOT EXISTS idx_work_note_author ON work_note(author_id);

-- User and session indexes
CREATE INDEX IF NOT EXISTS idx_user_role ON app_user(role);
CREATE INDEX IF NOT EXISTS idx_used_refresh_token_expires_at ON used_refresh_token(expires_at);

-- Backfill tsvectors on existing rows in case this baseline is applied
-- to a database that pre-dated the trigger columns.
UPDATE article SET title_en = title_en WHERE tsv_en IS NULL OR tsv_fr IS NULL;
UPDATE document_chunk SET content = content WHERE tsv_content IS NULL;
