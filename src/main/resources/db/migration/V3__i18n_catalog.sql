-- ============================================================
-- V3 — Add Spanish translation columns to catalogue tables
-- ============================================================

ALTER TABLE exercises
    ADD COLUMN name_es VARCHAR(120) NOT NULL DEFAULT '';

ALTER TABLE routines
    ADD COLUMN name_es        VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN description_es TEXT         NOT NULL DEFAULT '';

ALTER TABLE routine_days
    ADD COLUMN name_es VARCHAR(255) NOT NULL DEFAULT '';

ALTER TABLE routine_exercises
    ADD COLUMN exercise_name_es VARCHAR(255) NOT NULL DEFAULT '';
