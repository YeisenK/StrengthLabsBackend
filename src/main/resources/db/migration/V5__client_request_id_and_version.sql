-- ============================================================
-- V5 — Idempotent workout creation + optimistic locking
-- ============================================================
-- client_request_id allows the client to deduplicate retries after a
-- network timeout: re-sending POST /workouts with the same UUID returns
-- the existing workout instead of creating a duplicate.
--
-- version supports JPA @Version optimistic locking so concurrent PUTs
-- to the same workout don't silently overwrite each other.

ALTER TABLE workouts
    ADD COLUMN client_request_id UUID,
    ADD COLUMN version           BIGINT NOT NULL DEFAULT 0;

-- Partial unique index — Postgres allows multiple NULLs in plain UNIQUE,
-- but we want to be explicit that uniqueness only applies when the key
-- is supplied (older clients pre-idempotency send NULL).
CREATE UNIQUE INDEX uq_workouts_user_client_request
    ON workouts (user_id, client_request_id)
    WHERE client_request_id IS NOT NULL;
