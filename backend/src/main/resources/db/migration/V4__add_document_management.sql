CREATE EXTENSION IF NOT EXISTS vector;

-- Document table: stores uploaded file metadata with 5-stage processing status
CREATE TABLE document (
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
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_document PRIMARY KEY (id),
    CONSTRAINT fk_document_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id)
);

CREATE UNIQUE INDEX idx_document_workspace_hash ON document(workspace_id, content_hash);
CREATE INDEX idx_document_workspace ON document(workspace_id);
CREATE INDEX idx_document_status ON document(status);
CREATE INDEX idx_document_content_hash ON document(content_hash);

-- Document chunk table: stores extracted text with embeddings in separate collection
CREATE TABLE document_chunk (
    id UUID NOT NULL,
    document_id UUID NOT NULL,
    content TEXT NOT NULL,
    chunk_index INTEGER NOT NULL,
    embedding vector(768),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_document_chunk PRIMARY KEY (id),
    CONSTRAINT fk_dchunk_document FOREIGN KEY (document_id) REFERENCES document(id) ON DELETE CASCADE
);

CREATE INDEX idx_document_chunk_document ON document_chunk(document_id);
CREATE INDEX idx_document_chunk_index ON document_chunk(document_id, chunk_index);

-- Workspace LLM config table: per-workspace LLM endpoint configuration
CREATE TABLE workspace_llm_config (
    id UUID NOT NULL,
    workspace_id UUID NOT NULL,
    llm_provider VARCHAR(32) NOT NULL DEFAULT 'OLLAMA',
    endpoint_url VARCHAR(512),
    api_key VARCHAR(512),
    model_name VARCHAR(128) NOT NULL DEFAULT 'llama3.2',
    embedding_model_name VARCHAR(128) NOT NULL DEFAULT 'nomic-embed-text',
    similarity_threshold DOUBLE PRECISION NOT NULL DEFAULT 0.65,
    embedding_dimension INTEGER NOT NULL DEFAULT 768,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_workspace_llm_config PRIMARY KEY (id),
    CONSTRAINT uc_workspace_llm_config_workspace UNIQUE (workspace_id),
    CONSTRAINT fk_wllm_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id)
);

CREATE INDEX idx_workspace_llm_config_workspace ON workspace_llm_config(workspace_id);
