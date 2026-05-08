CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email              VARCHAR(255) NOT NULL UNIQUE,
    password_hash      VARCHAR(255) NOT NULL,
    role               VARCHAR(20) NOT NULL DEFAULT 'USER',
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    email_verified     BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified_at  TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_activity_at   TIMESTAMPTZ
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_status ON users (status);

CREATE TABLE user_profiles (
    id                         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                    UUID NOT NULL UNIQUE REFERENCES users (id),
    nickname                   VARCHAR(50),
    cep                        VARCHAR(9),
    city                       VARCHAR(100),
    state                      VARCHAR(2),
    approximate_latitude       DECIMAL(10, 7),
    approximate_longitude      DECIMAL(10, 7),
    show_city_state_publicly   BOOLEAN NOT NULL DEFAULT FALSE,
    use_location_for_search    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_profiles_user_id ON user_profiles (user_id);
CREATE INDEX idx_user_profiles_nickname ON user_profiles (nickname);

CREATE TABLE security_tokens (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type           VARCHAR(40) NOT NULL,
    token_hash     VARCHAR(64) NOT NULL UNIQUE,
    expires_at     TIMESTAMPTZ NOT NULL,
    consumed_at    TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_security_tokens_user_type ON security_tokens (user_id, type);
CREATE INDEX idx_security_tokens_token_hash_type ON security_tokens (token_hash, type);
CREATE INDEX idx_security_tokens_expires_at ON security_tokens (expires_at);
