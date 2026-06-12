-- V6: Add workspace invitation table, workspace soft-delete support, and icon metadata
-- Chained after V5__add_taxonomy_and_domain.sql

-- 1. Workspace invitation table: tracks PENDING/ACCEPTED/REJECTED lifecycle
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

CREATE INDEX IF NOT EXISTS idx_wsinvitation_workspace ON workspace_invitation(workspace_id);
CREATE INDEX IF NOT EXISTS idx_wsinvitation_invited_user ON workspace_invitation(invited_user_id);
CREATE INDEX IF NOT EXISTS idx_wsinvitation_status ON workspace_invitation(status);

-- 2. Workspace soft-delete support: deleted_at TIMESTAMP (nullable, NULL = active)
ALTER TABLE workspace ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITHOUT TIME ZONE;
CREATE INDEX IF NOT EXISTS idx_workspace_deleted_at ON workspace(deleted_at);

-- 3. Workspace icon metadata: icon VARCHAR (nullable, stores Lucide icon name like "building2")
ALTER TABLE workspace ADD COLUMN IF NOT EXISTS icon VARCHAR(64);
