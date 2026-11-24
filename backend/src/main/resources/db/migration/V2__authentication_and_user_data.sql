CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE refresh_session (
    token_hash VARCHAR(64) PRIMARY KEY,
    family_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES app_user(id),
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    revoked BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX refresh_session_family_idx ON refresh_session(family_id);
CREATE INDEX refresh_session_expiry_idx ON refresh_session(expires_at);

CREATE TABLE user_watchlist (
    user_id UUID PRIMARY KEY REFERENCES app_user(id),
    state TEXT NOT NULL
);
CREATE TABLE user_settings (
    user_id UUID PRIMARY KEY REFERENCES app_user(id),
    theme VARCHAR(10) NOT NULL DEFAULT 'system',
    CONSTRAINT user_settings_theme CHECK (theme IN ('light', 'dark', 'system'))
);

-- Legacy alerts have no trustworthy owner. Keep them inaccessible to user APIs.
ALTER TABLE price_alert ADD COLUMN user_id UUID REFERENCES app_user(id);
CREATE INDEX price_alert_user_idx ON price_alert(user_id, created_at DESC);
