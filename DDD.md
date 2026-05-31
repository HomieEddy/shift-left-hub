# Shift-Left Knowledge Hub - Database Design Document (DDD)

## Database Strategy: Postgres-Native Efficiency
The schema relies entirely on PostgreSQL. By utilizing native data types (ENUMs, Arrays, JSONB, and TSVECTOR), the architecture eliminates the need for separate search engines (like Elasticsearch) or complex NoSQL sidecars.

## 1. Custom Enumerations (ENUMs)
* `user_role`: 'ROLE_USER', 'ROLE_ADMIN'
* `ticket_category`: 'NETWORK', 'HARDWARE', 'SOFTWARE', 'ACCESS', 'PERIPHERALS'
* `ticket_urgency`: 'LOW', 'MEDIUM', 'HIGH'
* `ticket_status`: 'NEW', 'IN_PROGRESS', 'RESOLVED'
* `article_status`: 'DRAFT', 'PUBLISHED', 'ARCHIVED'

## 2. Core Tables

### Table: `app_user`
Manages authentication and relational ownership of tickets.
* `id`: UUID (PRIMARY KEY)
* `email`: VARCHAR(255) (UNIQUE, NOT NULL)
* `password_hash`: VARCHAR(255) (NOT NULL)
* `role`: user_role (NOT NULL)
* `created_at`: TIMESTAMP

### Table: `article` (The Knowledge Base)
Houses markdown documentation and leverages native arrays and Full-Text Search.
* `id`: BIGSERIAL (PRIMARY KEY)
* `title`: VARCHAR(255) (NOT NULL)
* `content_markdown`: TEXT (NOT NULL)
* `status`: article_status (NOT NULL, Default 'DRAFT')
* **`tags`: TEXT[]** (PostgreSQL Array)
* `linked_ticket_id`: BIGINT (FOREIGN KEY, Nullable)
* **`search_vector`: TSVECTOR** (Generated from title/content for fast FTS)

### Table: `ticket` (Incident Management)
Handles user escalation and provides context for the AI KCS loop.
* `id`: BIGSERIAL (PRIMARY KEY)
* `requester_id`: UUID (FOREIGN KEY)
* `category`: ticket_category (NOT NULL)
* `urgency`: ticket_urgency (NOT NULL)
* `description`: TEXT (NOT NULL)
* `status`: ticket_status (NOT NULL, Default 'NEW')
* **`shift_left_context`: JSONB** (Stores AI chat transcript)
* `resolution_notes`: TEXT (Fed to AI for KCS)
* `created_at`: TIMESTAMP
* `resolved_at`: TIMESTAMP

## 3. Indexing Strategy
* **FTS Index:** `CREATE INDEX idx_article_search ON article USING GIN (search_vector);`
* **Foreign Keys:** B-Tree indexes on `requester_id` and `linked_ticket_id`.
* **Filtering:** B-Tree indexes on `ticket(status)` and `article(status)`.