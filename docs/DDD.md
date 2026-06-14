# Shift-Left Knowledge Hub - Database Design Document (DDD)

## Database Strategy: Postgres-Native Efficiency
The schema relies entirely on PostgreSQL 16 with pgvector. By utilizing native data types (ENUMs, Arrays, JSONB, TSVECTOR, and vector embeddings), the architecture eliminates the need for separate search engines or complex NoSQL sidecars.

## 1. Custom Enumerations
* `user_role`: 'ROLE_USER', 'ROLE_AGENT', 'ROLE_ADMIN'
* `ticket_category`: 'NETWORK', 'HARDWARE', 'SOFTWARE', 'ACCESS', 'PERIPHERALS'
* `ticket_urgency`: 'LOW', 'MEDIUM', 'HIGH'
* `ticket_status`: 'NEW', 'IN_PROGRESS', 'RESOLVED'
* `article_status`: 'DRAFT', 'PUBLISHED', 'ARCHIVED'
* `document_status`: 'UPLOADED', 'PARSING', 'CHUNKING', 'EMBEDDING', 'READY', 'FAILED'

## 2. Core Tables

### Table: `workspace`
Multi-tenant isolation root. All domain entities reference this.
* `id`: UUID (PRIMARY KEY)
* `name`: VARCHAR(255) (NOT NULL)
* `slug`: VARCHAR(100) (UNIQUE, NOT NULL)
* `description`: TEXT
* `icon`: VARCHAR(50) (Lucide icon name)
* `deleted_at`: TIMESTAMP (soft-delete support)
* `created_at`: TIMESTAMP

### Table: `workspace_member`
Maps users to workspaces with scoped roles.
* `workspace_id`: UUID (FK → workspace)
* `user_id`: UUID (FK → app_user)
* `role`: VARCHAR(20) ('ADMIN', 'MEMBER', 'READ_ONLY')

### Table: `workspace_invitation`
In-app member invitations.
* `id`: UUID (PRIMARY KEY)
* `workspace_id`: UUID (FK → workspace)
* `invited_email`: VARCHAR(255)
* `role`: VARCHAR(20)
* `invited_by`: UUID (FK → app_user)
* `status`: VARCHAR(20) ('PENDING', 'ACCEPTED', 'REVOKED')

### Table: `app_user`
Authentication and user management.
* `id`: UUID (PRIMARY KEY)
* `email`: VARCHAR(255) (UNIQUE, NOT NULL)
* `display_name`: VARCHAR(255)
* `password_hash`: VARCHAR(255) (NOT NULL)
* `role`: user_role (NOT NULL)
* `default_workspace_id`: UUID (FK → workspace)
* `enabled`: BOOLEAN
* `created_at`: TIMESTAMP

### Table: `article` (The Knowledge Base)
* `id`: BIGSERIAL (PRIMARY KEY)
* `workspace_id`: UUID (FK → workspace, NOT NULL)
* `title_en`: VARCHAR(255)
* `title_fr`: VARCHAR(255)
* `content_en`: TEXT
* `content_fr`: TEXT
* `slug`: VARCHAR(255) (UNIQUE per workspace)
* `excerpt`: VARCHAR(500)
* `status`: article_status
* `author_id`: UUID (FK → app_user)
* `last_editor_id`: UUID (FK → app_user)
* `source_ticket_id`: BIGINT (FK → ticket, for KCS)
* `category_id`: BIGINT (FK → category)
* `tsv_en`: TSVECTOR (persistent, generated)
* `tsv_fr`: TSVECTOR (persistent, generated)
* `published_at`: TIMESTAMP
* `created_at`: TIMESTAMP

### Table: `tag`
* `id`: BIGSERIAL (PRIMARY KEY)
* `workspace_id`: UUID (FK → workspace)
* `name_en`: VARCHAR(50)
* `name_fr`: VARCHAR(50)
* `color`: VARCHAR(7) (hex color)
* `article_count`: INTEGER (denormalized counter)

### Table: `article_tag`
* `article_id`: BIGINT (FK → article)
* `tag_id`: BIGINT (FK → tag)

### Table: `category`
Domain taxonomy for articles and documents.
* `id`: BIGSERIAL (PRIMARY KEY)
* `workspace_id`: UUID (FK → workspace)
* `name`: VARCHAR(255)
* `parent_id`: BIGINT (self-referencing FK for subcategories)
* `sort_order`: INTEGER

### Table: `document`
Uploaded documents for the ingestion pipeline.
* `id`: UUID (PRIMARY KEY)
* `workspace_id`: UUID (FK → workspace)
* `filename`: VARCHAR(255)
* `mime_type`: VARCHAR(100)
* `file_size`: BIGINT
* `content_hash`: VARCHAR(64) (SHA-256)
* `status`: document_status
* `error_message`: TEXT
* `uploaded_by`: UUID (FK → app_user)
* `category_id`: BIGINT (FK → category)
* `created_at`: TIMESTAMP

### Table: `document_chunk`
Vectorized chunks from document ingestion.
* `id`: UUID (PRIMARY KEY)
* `document_id`: UUID (FK → document, ON DELETE CASCADE)
* `workspace_id`: UUID (FK → workspace)
* `chunk_index`: INTEGER
* `content`: TEXT
* `embedding`: vector(1536) (pgvector)
* `tsv`: TSVECTOR (for FTS on document chunks)
* `created_at`: TIMESTAMP

### Table: `ticket` (Incident Management)
* `id`: BIGSERIAL (PRIMARY KEY)
* `workspace_id`: UUID (FK → workspace)
* `ticket_number`: VARCHAR(20) (UNIQUE, TKT-NNNNN format)
* `requester_id`: UUID (FK → app_user)
* `assigned_to`: UUID (FK → app_user)
* `category`: ticket_category
* `urgency`: ticket_urgency
* `description`: TEXT
* `status`: ticket_status
* `shift_left_context`: JSONB (AI chat transcript)
* `resolution_notes`: TEXT
* `resolved_by`: UUID (FK → app_user)
* `is_knowledge_gap`: BOOLEAN
* `created_at`: TIMESTAMP
* `resolved_at`: TIMESTAMP

### Table: `work_note`
Agent notes on ticket resolution.
* `id`: BIGSERIAL (PRIMARY KEY)
* `ticket_id`: BIGINT (FK → ticket)
* `workspace_id`: UUID (FK → workspace)
* `author_id`: UUID (FK → app_user)
* `content`: TEXT
* `created_at`: TIMESTAMP

### Table: `ai_chat_session`
AI conversation sessions.
* `id`: UUID (PRIMARY KEY)
* `workspace_id`: UUID (FK → workspace)
* `user_id`: UUID (FK → app_user)
* `title`: VARCHAR(255)
* `created_at`: TIMESTAMP

### Table: `ai_chat_message`
Individual messages in AI chat sessions.
* `id`: UUID (PRIMARY KEY)
* `session_id`: UUID (FK → ai_chat_session)
* `role`: VARCHAR(10) ('user', 'assistant')
* `content`: TEXT
* `sources`: JSONB (cited sources metadata)
* `created_at`: TIMESTAMP

### Table: `workspace_llm_config`
Per-workspace LLM endpoint configuration.
* `id`: UUID (PRIMARY KEY)
* `workspace_id`: UUID (FK → workspace, UNIQUE)
* `endpoint_url`: VARCHAR(500)
* `api_key_encrypted`: TEXT (Spring TextEncryptor)
* `model_name`: VARCHAR(100)
* `system_prompt`: TEXT (customizable with template variables)
* `is_active`: BOOLEAN
* `updated_at`: TIMESTAMP

## 3. Indexing Strategy
* **FTS:** GIN index on `article.tsv_en` and `article.tsv_fr`, `document_chunk.tsv`
* **Vector:** IVFFlat index on `document_chunk.embedding`
* **Workspace Isolation:** Composite B-Tree on `(workspace_id, ...)` for all domain tables
* **Lookup:** B-Tree on `workspace.slug`, `article.slug`, `ticket.ticket_number`, `user.email`
* **Filtering:** B-Tree on `ticket(status)`, `article(status)`, `document(status)`
