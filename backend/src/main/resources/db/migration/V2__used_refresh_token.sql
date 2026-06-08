CREATE TABLE used_refresh_token (
    id UUID NOT NULL,
    token_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_used_refresh_token PRIMARY KEY (id),
    CONSTRAINT uc_used_refresh_token_token_id UNIQUE (token_id)
);

CREATE INDEX IF NOT EXISTS idx_used_refresh_token_expires_at ON used_refresh_token(expires_at);
