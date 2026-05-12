-- ============================================================
-- V1 — Initial schema matching JPA entities as of 2026-05-10
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── users ────────────────────────────────────────────────────

CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash TEXT         NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    active        BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_users_email  ON users (email);
CREATE INDEX idx_users_role   ON users (role);
CREATE INDEX idx_users_active ON users (active);

-- ── exercises ────────────────────────────────────────────────

CREATE TABLE exercises (
    id           UUID         PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    muscle_group VARCHAR(50)  NOT NULL,
    is_custom    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by   UUID         REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_exercises_muscle_group ON exercises (muscle_group);
CREATE INDEX idx_exercises_created_by   ON exercises (created_by);

-- ── workouts ─────────────────────────────────────────────────

CREATE TABLE workouts (
    id               UUID         PRIMARY KEY,
    user_id          UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name             VARCHAR(255) NOT NULL,
    date             TIMESTAMPTZ  NOT NULL,
    duration_seconds INT          NOT NULL DEFAULT 0,
    notes            TEXT
);

CREATE INDEX idx_workouts_user_id   ON workouts (user_id);
CREATE INDEX idx_workouts_user_date ON workouts (user_id, date DESC);

-- ── workout_exercises ─────────────────────────────────────────

CREATE TABLE workout_exercises (
    id          UUID    PRIMARY KEY,
    workout_id  UUID    NOT NULL REFERENCES workouts (id) ON DELETE CASCADE,
    exercise_id UUID    NOT NULL REFERENCES exercises (id),
    order_index INT     NOT NULL DEFAULT 0
);

CREATE INDEX idx_workout_exercises_workout  ON workout_exercises (workout_id);
CREATE INDEX idx_workout_exercises_exercise ON workout_exercises (exercise_id);

-- ── workout_sets ─────────────────────────────────────────────

CREATE TABLE workout_sets (
    id                  UUID           PRIMARY KEY,
    workout_exercise_id UUID           NOT NULL REFERENCES workout_exercises (id) ON DELETE CASCADE,
    weight_kg           NUMERIC(7, 2),
    reps                INT            NOT NULL,
    rpe                 NUMERIC(4, 1),
    order_index         INT            NOT NULL DEFAULT 0
);

CREATE INDEX idx_workout_sets_exercise ON workout_sets (workout_exercise_id);

-- ── training_metrics (daily fatigue cache) ────────────────────

CREATE TABLE training_metrics (
    id                       UUID           PRIMARY KEY,
    user_id                  UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    metric_date              DATE           NOT NULL,
    atl                      NUMERIC(10, 4) NOT NULL DEFAULT 0,
    ctl                      NUMERIC(10, 4) NOT NULL DEFAULT 0,
    tsb                      NUMERIC(10, 4) NOT NULL DEFAULT 0,
    acwr                     NUMERIC(8, 4)  NOT NULL DEFAULT 0,
    monotony                 NUMERIC(8, 4)  NOT NULL DEFAULT 0,
    strain                   NUMERIC(10, 4) NOT NULL DEFAULT 0,
    ramp_rate                NUMERIC(8, 4)  NOT NULL DEFAULT 0,
    readiness_score          NUMERIC(6, 2)  NOT NULL DEFAULT 0,
    injury_risk_score        NUMERIC(6, 2)  NOT NULL DEFAULT 0,
    overtraining_risk_score  NUMERIC(6, 2)  NOT NULL DEFAULT 0,
    composite_risk_score     NUMERIC(6, 2)  NOT NULL DEFAULT 0,
    risk_level               VARCHAR(20)    NOT NULL DEFAULT 'low',
    dominant_factor          VARCHAR(100)   NOT NULL DEFAULT '',
    risk_flags_json          TEXT           NOT NULL DEFAULT '[]',
    recommendations_json     TEXT           NOT NULL DEFAULT '[]',
    weekly_volume_json       TEXT           NOT NULL DEFAULT '{}',
    trend_json               TEXT           NOT NULL DEFAULT '[]',
    compute_available        BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_training_metrics_user_date UNIQUE (user_id, metric_date)
);

CREATE INDEX idx_training_metrics_user_date ON training_metrics (user_id, metric_date DESC);
