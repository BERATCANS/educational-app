-- ============================================================
-- Add indexes on FK columns that were missing in V1
-- ============================================================
-- tests.topic_id and questions.test_id were declared as FOREIGN KEYs
-- but no supporting index was created. PostgreSQL does NOT auto-index
-- FK columns, so cascading deletes and child lookups (e.g. "all
-- questions of a test", "all tests of a topic") perform sequential
-- scans. As data grows this becomes a visible latency hit.
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_tests_topic_id ON tests(topic_id);
CREATE INDEX IF NOT EXISTS idx_questions_test_id ON questions(test_id);
