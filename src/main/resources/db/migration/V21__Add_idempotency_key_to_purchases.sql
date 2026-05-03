-- ============================================================
-- purchases: idempotency_key column + partial unique index
-- ============================================================
-- Client can send POST /api/cart/checkout with an Idempotency-Key header
-- to make retries safe: a second request with the same key for the same
-- student profile must NOT create a second Purchase row.
--
-- The unique index is partial (WHERE idempotency_key IS NOT NULL) so that
-- pre-existing rows (and future callers that omit the header) are not
-- constrained — only keyed rows enforce uniqueness.
-- ============================================================

ALTER TABLE purchases ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(80);

CREATE UNIQUE INDEX IF NOT EXISTS uk_purchases_student_idempotency
    ON purchases (student_profile_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
