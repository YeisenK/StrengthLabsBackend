CREATE TYPE user_role   AS ENUM ('USER', 'TRAINER', 'ADMIN');
CREATE TYPE risk_zone   AS ENUM ('GREEN', 'YELLOW', 'RED');
CREATE TYPE muscle_group AS ENUM (
    'CHEST', 'BACK', 'SHOULDERS',
    'BICEPS', 'TRICEPS', 'FOREARMS',
    'QUADS', 'HAMSTRINGS', 'GLUTES',
    'CALVES', 'CORE', 'FULL_BODY', 'OTHER'
);

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- users
-- ============================================================

CREATE TABLE users (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    email                       VARCHAR(255)    NOT NULL UNIQUE,
    username                    VARCHAR(100)    NOT NULL UNIQUE,
    password_hash               TEXT            NOT NULL,
    role                        user_role       NOT NULL DEFAULT 'USER',
    is_active                   BOOLEAN         NOT NULL DEFAULT TRUE,
    refresh_token_hash          TEXT,
    refresh_token_expires_at    TIMESTAMPTZ,
    failed_login_attempts       SMALLINT        NOT NULL DEFAULT 0,
    locked_until                TIMESTAMPTZ,
    last_login_at               TIMESTAMPTZ,
    last_login_ip               INET,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email    ON users (email);
CREATE INDEX idx_users_role     ON users (role);
CREATE INDEX idx_users_active   ON users (is_active);

-- ============================================================
-- training_sessions
-- ============================================================

CREATE TABLE training_sessions (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID            NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    session_date        DATE            NOT NULL,
    muscle_group        muscle_group    NOT NULL,
    sets                SMALLINT        NOT NULL CHECK (sets > 0),
    reps_per_set        SMALLINT[]      NOT NULL,
    weight_kg           NUMERIC(6, 2)   NOT NULL CHECK (weight_kg >= 0),
    avg_rir             NUMERIC(3, 1)   CHECK (avg_rir BETWEEN 0 AND 10),
    rpe                 NUMERIC(3, 1)   CHECK (rpe BETWEEN 1 AND 10),
    duration_minutes    SMALLINT        CHECK (duration_minutes > 0),
    notes               TEXT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sessions_user_id      ON training_sessions (user_id);
CREATE INDEX idx_sessions_date         ON training_sessions (session_date);
CREATE INDEX idx_sessions_user_date    ON training_sessions (user_id, session_date DESC);
CREATE INDEX idx_sessions_muscle       ON training_sessions (muscle_group);

-- ============================================================
-- training_metrics
-- ============================================================

CREATE TABLE training_metrics (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID            NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    session_id          UUID            REFERENCES training_sessions (id) ON DELETE SET NULL,
    metric_date         DATE            NOT NULL,
    atl                 NUMERIC(8, 4)   NOT NULL,   
    ctl                 NUMERIC(8, 4)   NOT NULL,   
    acwr                NUMERIC(6, 4)   NOT NULL,   
    tsb                 NUMERIC(8, 4)   NOT NULL,   
    fatigue_score       NUMERIC(5, 2)   NOT NULL CHECK (fatigue_score BETWEEN 0 AND 100),
    risk_zone           risk_zone       NOT NULL DEFAULT 'GREEN',
    risk_reason         TEXT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_metrics_user_date UNIQUE (user_id, metric_date)
);

CREATE INDEX idx_metrics_user_id    ON training_metrics (user_id);
CREATE INDEX idx_metrics_date       ON training_metrics (metric_date DESC);
CREATE INDEX idx_metrics_user_date  ON training_metrics (user_id, metric_date DESC);
CREATE INDEX idx_metrics_risk_zone  ON training_metrics (risk_zone);


CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();