-- V2: Add CASCADE DELETE to question_results.question_id foreign key
-- This allows questions to be deleted even if they have associated question results

-- Drop the existing foreign key constraint
ALTER TABLE question_results DROP CONSTRAINT IF EXISTS question_results_question_id_fkey;

-- Recreate the foreign key with ON DELETE CASCADE
ALTER TABLE question_results 
ADD CONSTRAINT question_results_question_id_fkey 
FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE;

