-- ==========================================
-- Migration to update Book Review ratings
-- ==========================================

-- 1. Modify book_reviews table
-- Add the new rating columns
ALTER TABLE book_reviews 
    ADD COLUMN content_quality_rating INT NOT NULL DEFAULT 5,
    ADD COLUMN difficulty_rating INT NOT NULL DEFAULT 5,
    ADD COLUMN overall_rating INT NOT NULL DEFAULT 5,
    ADD COLUMN video_quality_rating INT NOT NULL DEFAULT 5;

-- Update the overall_rating to match the existing rating to preserve some historical data (optional but good practice)
UPDATE book_reviews SET overall_rating = rating;

-- Drop the old rating and comment columns
ALTER TABLE book_reviews 
    DROP COLUMN comment,
    DROP COLUMN rating;


-- 2. Modify books table to support new average ratings
-- Add the new average rating columns
ALTER TABLE books
    ADD COLUMN average_content_quality_rating REAL NOT NULL DEFAULT 0.0,
    ADD COLUMN average_difficulty_rating REAL NOT NULL DEFAULT 0.0,
    ADD COLUMN average_overall_rating REAL NOT NULL DEFAULT 0.0,
    ADD COLUMN average_video_quality_rating REAL NOT NULL DEFAULT 0.0;

-- Update the average_overall_rating to match the existing average_rating
UPDATE books SET average_overall_rating = average_rating;

-- Drop the old average_rating column
ALTER TABLE books
    DROP COLUMN average_rating;
