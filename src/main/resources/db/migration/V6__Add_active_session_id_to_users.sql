-- V6: Add active_session_id column to users table
-- This column stores the UUID of the active session for concurrent session control
-- Added to support JWT token session management

ALTER TABLE users 
ADD COLUMN IF NOT EXISTS active_session_id VARCHAR(255);

-- Add comment for documentation
COMMENT ON COLUMN users.active_session_id IS 'Tekil oturum ID''si (UUID) - Concurrent session kontrolü için kullanılır';
