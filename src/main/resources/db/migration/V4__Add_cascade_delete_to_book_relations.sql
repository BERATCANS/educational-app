-- V4: Add CASCADE DELETE to book/topic/test/question hierarchy
-- This allows proper cascade deletion from book down to all related entities

-- 1. topics.book_id: CASCADE DELETE (book silinince topic'ler de silinmeli)
ALTER TABLE topics DROP CONSTRAINT IF EXISTS topics_book_id_fkey;
ALTER TABLE topics 
ADD CONSTRAINT topics_book_id_fkey 
FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE;

-- 2. tests.topic_id: CASCADE DELETE (topic silinince test'ler de silinmeli)
ALTER TABLE tests DROP CONSTRAINT IF EXISTS tests_topic_id_fkey;
ALTER TABLE tests 
ADD CONSTRAINT tests_topic_id_fkey 
FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE;

-- 3. questions.test_id: CASCADE DELETE (test silinince question'lar da silinmeli)
ALTER TABLE questions DROP CONSTRAINT IF EXISTS questions_test_id_fkey;
ALTER TABLE questions 
ADD CONSTRAINT questions_test_id_fkey 
FOREIGN KEY (test_id) REFERENCES tests(id) ON DELETE CASCADE;

-- 4. question_ai_conversations.question_id: CASCADE DELETE (question silinince AI konuşmaları da silinmeli)
ALTER TABLE question_ai_conversations DROP CONSTRAINT IF EXISTS question_ai_conversations_question_id_fkey;
ALTER TABLE question_ai_conversations 
ADD CONSTRAINT question_ai_conversations_question_id_fkey 
FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE;

-- 5. test_results.test_id: CASCADE DELETE (test silinince test sonuçları da silinmeli)
ALTER TABLE test_results DROP CONSTRAINT IF EXISTS test_results_test_id_fkey;
ALTER TABLE test_results 
ADD CONSTRAINT test_results_test_id_fkey 
FOREIGN KEY (test_id) REFERENCES tests(id) ON DELETE CASCADE;

-- 6. book_reviews.book_id: CASCADE DELETE (book silinince yorumlar da silinmeli)
ALTER TABLE book_reviews DROP CONSTRAINT IF EXISTS book_reviews_book_id_fkey;
ALTER TABLE book_reviews 
ADD CONSTRAINT book_reviews_book_id_fkey 
FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE;

-- 7. purchases.book_id: SET NULL (book silinince satın alma kaydı kalmalı ama book_id null olmalı)
-- NOT: purchases tablosunda book_id nullable değilse bu çalışmaz, o zaman CASCADE olmalı
-- Şimdilik CASCADE yapalım çünkü book_id NOT NULL
ALTER TABLE purchases DROP CONSTRAINT IF EXISTS purchases_book_id_fkey;
ALTER TABLE purchases 
ADD CONSTRAINT purchases_book_id_fkey 
FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE;

-- 8. cart_items.book_id: CASCADE DELETE (book silinince sepetteki item da silinmeli)
ALTER TABLE cart_items DROP CONSTRAINT IF EXISTS cart_items_book_id_fkey;
ALTER TABLE cart_items 
ADD CONSTRAINT cart_items_book_id_fkey 
FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE;

-- 9. book_codes.book_id: CASCADE DELETE (book silinince kodlar da silinmeli)
-- NOT: Aslında SET NULL da olabilir ama kodlar book'a özeldir, CASCADE daha mantıklı
ALTER TABLE book_codes DROP CONSTRAINT IF EXISTS book_codes_book_id_fkey;
ALTER TABLE book_codes 
ADD CONSTRAINT book_codes_book_id_fkey 
FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE;

