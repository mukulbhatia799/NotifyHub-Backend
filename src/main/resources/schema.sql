-- =============================================================
-- NotifyHub Database Schema
-- All columns that hold Java enums use VARCHAR so that
-- @Enumerated(EnumType.STRING) mappings work without custom
-- PostgreSQL ENUM types (which break Spring Boot's SQL splitter).
-- Every statement here ends with a plain semicolon — no DO $$
-- blocks, no embedded semicolons.
-- =============================================================

-- ---------------------------------------------------------------
-- users
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    name       VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_role  ON users (role);

-- ---------------------------------------------------------------
-- refresh_tokens
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token      TEXT        NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_refresh_token UNIQUE (token)
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token   ON refresh_tokens (token);

-- ---------------------------------------------------------------
-- notification_events
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification_events (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(50)  NOT NULL,
    payload    TEXT         NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    tenant_id  VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ne_status     ON notification_events (status);
CREATE INDEX IF NOT EXISTS idx_ne_event_type ON notification_events (event_type);
CREATE INDEX IF NOT EXISTS idx_ne_tenant_id  ON notification_events (tenant_id);
CREATE INDEX IF NOT EXISTS idx_ne_created_at ON notification_events (created_at DESC);

-- ---------------------------------------------------------------
-- notification_logs
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification_logs (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        UUID        NOT NULL REFERENCES notification_events (id) ON DELETE CASCADE,
    channel         VARCHAR(20) NOT NULL,
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'RETRYING',
    attempt_count   INT         NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMPTZ,
    error_message   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_nl_event_id        ON notification_logs (event_id);
CREATE INDEX IF NOT EXISTS idx_nl_delivery_status ON notification_logs (delivery_status);
CREATE INDEX IF NOT EXISTS idx_nl_channel         ON notification_logs (channel);

-- ---------------------------------------------------------------
-- subscriptions
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS subscriptions (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    channel    VARCHAR(20) NOT NULL,
    active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_subscription UNIQUE (user_id, event_type, channel)
);

CREATE INDEX IF NOT EXISTS idx_sub_user_id    ON subscriptions (user_id);
CREATE INDEX IF NOT EXISTS idx_sub_event_type ON subscriptions (event_type);
CREATE INDEX IF NOT EXISTS idx_sub_active     ON subscriptions (active);
