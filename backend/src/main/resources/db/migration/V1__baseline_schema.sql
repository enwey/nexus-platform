CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(255),
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS user_profile (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    display_name VARCHAR(128),
    avatar_url VARCHAR(512),
    language_tag VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS wallet_account (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    balance NUMERIC(18, 2) NOT NULL,
    frozen_balance NUMERIC(18, 2) NOT NULL,
    today_income NUMERIC(18, 2) NOT NULL,
    total_income NUMERIC(18, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS games (
    id BIGSERIAL PRIMARY KEY,
    app_id VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    icon_url VARCHAR(255),
    download_url VARCHAR(255),
    storage_key VARCHAR(255),
    version VARCHAR(255),
    md5 VARCHAR(255),
    category VARCHAR(32),
    tags_json VARCHAR(255),
    status VARCHAR(32),
    developer_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_games_status_created_at ON games(status, created_at);
CREATE INDEX IF NOT EXISTS idx_games_developer_created_at ON games(developer_id, created_at);

CREATE TABLE IF NOT EXISTS game_versions (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL,
    version_name VARCHAR(64) NOT NULL,
    entry_file VARCHAR(255) NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    download_url VARCHAR(255),
    md5 VARCHAR(64),
    submit_note VARCHAR(256),
    audit_reason VARCHAR(256),
    is_forced_update BOOLEAN NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_game_versions_game_created_at ON game_versions(game_id, created_at);
CREATE INDEX IF NOT EXISTS idx_game_versions_status_created_at ON game_versions(status, created_at);

CREATE TABLE IF NOT EXISTS game_media_assets (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL,
    version_id BIGINT,
    media_type VARCHAR(32) NOT NULL,
    url VARCHAR(512) NOT NULL,
    width INTEGER,
    height INTEGER,
    size_bytes BIGINT,
    locale VARCHAR(16) NOT NULL,
    sort_order INTEGER NOT NULL,
    is_primary BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS game_ops_profile (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL UNIQUE,
    studio_name VARCHAR(128),
    player_count_text VARCHAR(128),
    runtime_banner_url VARCHAR(512),
    runtime_logo_url VARCHAR(512),
    share_title VARCHAR(128),
    share_subtitle VARCHAR(256),
    share_image_url VARCHAR(512),
    discover_card_cover_url VARCHAR(512),
    discover_card_logo_url VARCHAR(512),
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(64) NOT NULL,
    operator_id BIGINT NOT NULL,
    operator_role VARCHAR(32) NOT NULL,
    target_game_id BIGINT,
    target_app_id VARCHAR(64),
    success BOOLEAN NOT NULL,
    reason VARCHAR(256),
    request_uri VARCHAR(256),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);

CREATE TABLE IF NOT EXISTS ops_collection (
    id BIGSERIAL PRIMARY KEY,
    collection_code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    page_code VARCHAR(64) NOT NULL,
    description VARCHAR(512),
    cover_url VARCHAR(512),
    status VARCHAR(32) NOT NULL,
    start_at TIMESTAMP,
    end_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS ops_collection_game_rel (
    id BIGSERIAL PRIMARY KEY,
    collection_id BIGINT NOT NULL,
    game_id BIGINT NOT NULL,
    sort_order INTEGER NOT NULL,
    pin_top BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS ops_content_slot (
    id BIGSERIAL PRIMARY KEY,
    slot_code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    page_code VARCHAR(64) NOT NULL,
    position_code VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS ops_content_item (
    id BIGSERIAL PRIMARY KEY,
    slot_id BIGINT NOT NULL,
    game_id BIGINT NOT NULL,
    title VARCHAR(128),
    subtitle VARCHAR(256),
    cover_url VARCHAR(512),
    badge_text VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    start_at TIMESTAMP,
    end_at TIMESTAMP,
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS user_game_action_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    game_id BIGINT,
    app_id VARCHAR(255),
    action_type VARCHAR(32) NOT NULL,
    scene VARCHAR(64),
    payload_json TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS user_game_engagement (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    app_id VARCHAR(255) NOT NULL,
    play_count BIGINT NOT NULL,
    last_played_at TIMESTAMP,
    is_favorite BOOLEAN NOT NULL,
    favorite_at TIMESTAMP,
    share_count BIGINT NOT NULL,
    last_shared_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_user_game_engagement_user_app UNIQUE (user_id, app_id)
);

CREATE INDEX IF NOT EXISTS idx_user_game_engagement_user_last_played
    ON user_game_engagement(user_id, last_played_at);
CREATE INDEX IF NOT EXISTS idx_user_game_engagement_user_favorite
    ON user_game_engagement(user_id, is_favorite, favorite_at);
