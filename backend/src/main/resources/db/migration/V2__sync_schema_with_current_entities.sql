ALTER TABLE games
    ADD COLUMN IF NOT EXISTS source_storage_key VARCHAR(255),
    ADD COLUMN IF NOT EXISTS source_md5 VARCHAR(64),
    ADD COLUMN IF NOT EXISTS package_format VARCHAR(64),
    ADD COLUMN IF NOT EXISTS package_key_ciphertext VARCHAR(512),
    ADD COLUMN IF NOT EXISTS package_key_nonce VARCHAR(128),
    ADD COLUMN IF NOT EXISTS requires_online BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE ops_content_item
    ADD COLUMN IF NOT EXISTS article_tag VARCHAR(64),
    ADD COLUMN IF NOT EXISTS article_title VARCHAR(256),
    ADD COLUMN IF NOT EXISTS article_body TEXT,
    ADD COLUMN IF NOT EXISTS action_text VARCHAR(64);

CREATE TABLE IF NOT EXISTS ops_discover_category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS ops_game_category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS verification_code_logs (
    id BIGSERIAL PRIMARY KEY,
    account VARCHAR(191) NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    debug_code VARCHAR(16),
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(256),
    request_source VARCHAR(64),
    request_scene VARCHAR(64),
    request_ip VARCHAR(64),
    request_uri VARCHAR(256),
    requester_user_id BIGINT,
    requester_role VARCHAR(32),
    user_agent VARCHAR(512),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_vcode_logs_created_at
    ON verification_code_logs(created_at);

CREATE INDEX IF NOT EXISTS idx_vcode_logs_account
    ON verification_code_logs(account);

CREATE INDEX IF NOT EXISTS idx_vcode_logs_purpose
    ON verification_code_logs(purpose);

CREATE INDEX IF NOT EXISTS idx_vcode_logs_source
    ON verification_code_logs(request_source);
