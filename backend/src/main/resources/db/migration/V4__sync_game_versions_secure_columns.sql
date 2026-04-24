ALTER TABLE game_versions
    ADD COLUMN IF NOT EXISTS source_storage_key VARCHAR(255),
    ADD COLUMN IF NOT EXISTS source_md5 VARCHAR(64),
    ADD COLUMN IF NOT EXISTS package_format VARCHAR(64),
    ADD COLUMN IF NOT EXISTS package_key_ciphertext VARCHAR(512),
    ADD COLUMN IF NOT EXISTS package_key_nonce VARCHAR(128);
