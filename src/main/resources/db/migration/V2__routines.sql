-- ============================================================
-- V2 — Routine templates (static programme catalogue)
-- ============================================================

CREATE TABLE routines (
    id           VARCHAR(50)  PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    level        VARCHAR(20)  NOT NULL,
    goal         VARCHAR(50)  NOT NULL,
    days_per_week INT         NOT NULL,
    description  TEXT         NOT NULL
);

CREATE TABLE routine_days (
    id          UUID         PRIMARY KEY,
    routine_id  VARCHAR(50)  NOT NULL REFERENCES routines (id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    order_index INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_routine_days_routine ON routine_days (routine_id);

CREATE TABLE routine_exercises (
    id             UUID         PRIMARY KEY,
    routine_day_id UUID         NOT NULL REFERENCES routine_days (id) ON DELETE CASCADE,
    exercise_name  VARCHAR(255) NOT NULL,
    muscle_group   VARCHAR(50)  NOT NULL,
    sets           INT          NOT NULL,
    reps_scheme    VARCHAR(50)  NOT NULL,
    notes          VARCHAR(500),
    order_index    INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_routine_exercises_day ON routine_exercises (routine_day_id);
