-- ==========================================
-- Migration to update Book Review ratings
-- ==========================================

-- 1. Modify book_reviews table
-- Add the new rating columns (idempotent)
ALTER TABLE book_reviews
    ADD COLUMN IF NOT EXISTS content_quality_rating INT NOT NULL DEFAULT 5,
    ADD COLUMN IF NOT EXISTS difficulty_rating INT NOT NULL DEFAULT 5,
    ADD COLUMN IF NOT EXISTS overall_rating INT NOT NULL DEFAULT 5,
    ADD COLUMN IF NOT EXISTS video_quality_rating INT NOT NULL DEFAULT 5;

-- If legacy rating column exists, carry its value to overall_rating
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'book_reviews'
          AND column_name = 'rating'
    ) THEN
        EXECUTE 'UPDATE book_reviews SET overall_rating = rating';
    END IF;
END $$;

-- Drop old columns if they still exist
ALTER TABLE book_reviews
    DROP COLUMN IF EXISTS comment,
    DROP COLUMN IF EXISTS rating;

-- 2. Modify books table to support new average ratings
-- Add the new average rating columns (idempotent)
ALTER TABLE books
    ADD COLUMN IF NOT EXISTS average_content_quality_rating REAL NOT NULL DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS average_difficulty_rating REAL NOT NULL DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS average_overall_rating REAL NOT NULL DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS average_video_quality_rating REAL NOT NULL DEFAULT 0.0;

-- If legacy average_rating exists, carry its value to average_overall_rating
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'books'
          AND column_name = 'average_rating'
    ) THEN
        EXECUTE 'UPDATE books SET average_overall_rating = average_rating';
    END IF;
END $$;

-- Drop old average_rating if it still exists
ALTER TABLE books
    DROP COLUMN IF EXISTS average_rating;
