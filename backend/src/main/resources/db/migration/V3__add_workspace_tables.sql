CREATE TABLE workspace (
    id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(128) NOT NULL,
    description TEXT,
    logo_url VARCHAR(512),
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_workspace PRIMARY KEY (id),
    CONSTRAINT uc_workspace_slug UNIQUE (slug),
    CONSTRAINT fk_workspace_created_by FOREIGN KEY (created_by) REFERENCES app_user(id)
);

CREATE TABLE workspace_member (
    workspace_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_workspace_member PRIMARY KEY (workspace_id, user_id),
    CONSTRAINT fk_wsmember_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id),
    CONSTRAINT fk_wsmember_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

ALTER TABLE article ADD COLUMN workspace_id UUID;
ALTER TABLE article ADD CONSTRAINT fk_article_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id);
CREATE INDEX idx_article_workspace ON article(workspace_id);

ALTER TABLE ticket ADD COLUMN workspace_id UUID;
ALTER TABLE ticket ADD CONSTRAINT fk_ticket_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id);
CREATE INDEX idx_ticket_workspace ON ticket(workspace_id);

ALTER TABLE tag ADD COLUMN workspace_id UUID;
ALTER TABLE tag ADD CONSTRAINT fk_tag_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id);
CREATE INDEX idx_tag_workspace ON tag(workspace_id);

ALTER TABLE work_note ADD COLUMN workspace_id UUID;
ALTER TABLE work_note ADD CONSTRAINT fk_work_note_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id);
CREATE INDEX idx_work_note_workspace ON work_note(workspace_id);

ALTER TABLE app_user ADD COLUMN default_workspace_id UUID;
ALTER TABLE app_user ADD CONSTRAINT fk_user_default_workspace FOREIGN KEY (default_workspace_id) REFERENCES workspace(id);
