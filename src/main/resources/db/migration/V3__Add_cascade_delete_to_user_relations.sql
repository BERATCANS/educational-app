-- V3: Add CASCADE DELETE to user-related foreign keys
-- This allows users to be deleted even if they have associated records

-- 1. book_codes: SET NULL (user_id is nullable - kullanıcı silinse bile kod kalmalı)
ALTER TABLE book_codes DROP CONSTRAINT IF EXISTS book_codes_user_id_fkey;
ALTER TABLE book_codes 
ADD CONSTRAINT book_codes_user_id_fkey 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

-- 2. carts: CASCADE DELETE (her kullanıcının bir sepeti var)
ALTER TABLE carts DROP CONSTRAINT IF EXISTS carts_user_id_fkey;
ALTER TABLE carts 
ADD CONSTRAINT carts_user_id_fkey 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 3. purchases: CASCADE DELETE (kullanıcı silinince satın alma geçmişi de silinmeli)
ALTER TABLE purchases DROP CONSTRAINT IF EXISTS purchases_user_id_fkey;
ALTER TABLE purchases 
ADD CONSTRAINT purchases_user_id_fkey 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 4. book_reviews: CASCADE DELETE (kullanıcı silinince yorumları da silinmeli)
ALTER TABLE book_reviews DROP CONSTRAINT IF EXISTS book_reviews_user_id_fkey;
ALTER TABLE book_reviews 
ADD CONSTRAINT book_reviews_user_id_fkey 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 5. test_results: CASCADE DELETE (kullanıcı silinince test sonuçları da silinmeli)
ALTER TABLE test_results DROP CONSTRAINT IF EXISTS test_results_user_id_fkey;
ALTER TABLE test_results 
ADD CONSTRAINT test_results_user_id_fkey 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 6. discount_code_usages: CASCADE DELETE (kullanıcı silinince kullanım geçmişi de silinmeli)
ALTER TABLE discount_code_usages DROP CONSTRAINT IF EXISTS discount_code_usages_user_id_fkey;
ALTER TABLE discount_code_usages 
ADD CONSTRAINT discount_code_usages_user_id_fkey 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 7. daily_question_answers: CASCADE DELETE (kullanıcı silinince günlük soru cevapları da silinmeli)
ALTER TABLE daily_question_answers DROP CONSTRAINT IF EXISTS daily_question_answers_user_id_fkey;
ALTER TABLE daily_question_answers 
ADD CONSTRAINT daily_question_answers_user_id_fkey 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 8. question_ai_conversations: CASCADE DELETE (kullanıcı silinince AI konuşmaları da silinmeli)
ALTER TABLE question_ai_conversations DROP CONSTRAINT IF EXISTS question_ai_conversations_user_id_fkey;
ALTER TABLE question_ai_conversations 
ADD CONSTRAINT question_ai_conversations_user_id_fkey 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

