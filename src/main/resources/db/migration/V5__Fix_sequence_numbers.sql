-- V5: Fix sequence numbers for all tables
-- This migration fixes PostgreSQL sequence values to match the current MAX(id) values
-- This prevents "duplicate key value violates unique constraint" errors

-- Function to safely fix a sequence
-- If table is empty, sets sequence to 1, otherwise sets to MAX(id)
DO $$
DECLARE
    max_id BIGINT;
BEGIN
    -- Fix users sequence
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM users;
    IF max_id = 0 THEN
        PERFORM setval('users_id_seq', 1, false);
    ELSE
        PERFORM setval('users_id_seq', max_id, true);
    END IF;
    RAISE NOTICE 'Fixed users_id_seq to %', max_id;

    -- Fix publishers sequence
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM publishers;
    IF max_id = 0 THEN
        PERFORM setval('publishers_id_seq', 1, false);
    ELSE
        PERFORM setval('publishers_id_seq', max_id, true);
    END IF;
    RAISE NOTICE 'Fixed publishers_id_seq to %', max_id;

    -- Fix books sequence
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM books;
    IF max_id = 0 THEN
        PERFORM setval('books_id_seq', 1, false);
    ELSE
        PERFORM setval('books_id_seq', max_id, true);
    END IF;
    RAISE NOTICE 'Fixed books_id_seq to %', max_id;

    -- Fix topics sequence
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM topics;
    IF max_id = 0 THEN
        PERFORM setval('topics_id_seq', 1, false);
    ELSE
        PERFORM setval('topics_id_seq', max_id, true);
    END IF;
    RAISE NOTICE 'Fixed topics_id_seq to %', max_id;

    -- Fix tests sequence
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM tests;
    IF max_id = 0 THEN
        PERFORM setval('tests_id_seq', 1, false);
    ELSE
        PERFORM setval('tests_id_seq', max_id, true);
    END IF;
    RAISE NOTICE 'Fixed tests_id_seq to %', max_id;

    -- Fix questions sequence
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM questions;
    IF max_id = 0 THEN
        PERFORM setval('questions_id_seq', 1, false);
    ELSE
        PERFORM setval('questions_id_seq', max_id, true);
    END IF;
    RAISE NOTICE 'Fixed questions_id_seq to %', max_id;

    -- Fix book_codes sequence
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM book_codes;
    IF max_id = 0 THEN
        PERFORM setval('book_codes_id_seq', 1, false);
    ELSE
        PERFORM setval('book_codes_id_seq', max_id, true);
    END IF;
    RAISE NOTICE 'Fixed book_codes_id_seq to %', max_id;

    -- Fix discount_codes sequence
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM discount_codes;
    IF max_id = 0 THEN
        PERFORM setval('discount_codes_id_seq', 1, false);
    ELSE
        PERFORM setval('discount_codes_id_seq', max_id, true);
    END IF;
    RAISE NOTICE 'Fixed discount_codes_id_seq to %', max_id;

    -- Fix carts sequence
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM carts;
    IF max_id = 0 THEN
        PERFORM setval('carts_id_seq', 1, false);
    ELSE
        PERFORM setval('carts_id_seq', max_id, true);
    END IF;
    RAISE NOTICE 'Fixed carts_id_seq to %', max_id;

    -- Fix cart_items sequence
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM cart_items;
    IF max_id = 0 THEN
        PERFORM setval('cart_items_id_seq', 1, false);
    ELSE
        PERFORM setval('cart_items_id_seq', max_id, true);
    END IF;
    RAISE NOTICE 'Fixed cart_items_id_seq to %', max_id;

    -- Fix purchases sequence
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM purchases;
    IF max_id = 0 THEN
        PERFORM setval('purchases_id_seq', 1, false);
    ELSE
        PERFORM setval('purchases_id_seq', max_id, true);
    END IF;
    RAISE NOTICE 'Fixed purchases_id_seq to %', max_id;

    -- Fix book_reviews sequence (the problematic one)
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM book_reviews;
    IF max_id = 0 THEN
        PERFORM setval('book_reviews_id_seq', 1, false);
    ELSE
        PERFORM setval('book_reviews_id_seq', max_id, true);
    END IF;
    RAISE NOTICE 'Fixed book_reviews_id_seq to %', max_id;

    -- Fix test_results sequence
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM test_results;
    IF max_id = 0 THEN
        PERFORM setval('test_results_id_seq', 1, false);
    ELSE
        PERFORM setval('test_results_id_seq', max_id, true);
    END IF;
    RAISE NOTICE 'Fixed test_results_id_seq to %', max_id;

    -- Fix question_results sequence
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM question_results;
    IF max_id = 0 THEN
        PERFORM setval('question_results_id_seq', 1, false);
    ELSE
        PERFORM setval('question_results_id_seq', max_id, true);
    END IF;
    RAISE NOTICE 'Fixed question_results_id_seq to %', max_id;

    -- Fix discount_code_usages sequence
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM discount_code_usages;
    IF max_id = 0 THEN
        PERFORM setval('discount_code_usages_id_seq', 1, false);
    ELSE
        PERFORM setval('discount_code_usages_id_seq', max_id, true);
    END IF;
    RAISE NOTICE 'Fixed discount_code_usages_id_seq to %', max_id;

    -- Fix daily_questions sequence
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM daily_questions;
    IF max_id = 0 THEN
        PERFORM setval('daily_questions_id_seq', 1, false);
    ELSE
        PERFORM setval('daily_questions_id_seq', max_id, true);
    END IF;
    RAISE NOTICE 'Fixed daily_questions_id_seq to %', max_id;

    -- Fix daily_question_answers sequence
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM daily_question_answers;
    IF max_id = 0 THEN
        PERFORM setval('daily_question_answers_id_seq', 1, false);
    ELSE
        PERFORM setval('daily_question_answers_id_seq', max_id, true);
    END IF;
    RAISE NOTICE 'Fixed daily_question_answers_id_seq to %', max_id;

    -- Fix question_ai_conversations sequence
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM question_ai_conversations;
    IF max_id = 0 THEN
        PERFORM setval('question_ai_conversations_id_seq', 1, false);
    ELSE
        PERFORM setval('question_ai_conversations_id_seq', max_id, true);
    END IF;
    RAISE NOTICE 'Fixed question_ai_conversations_id_seq to %', max_id;

    -- Fix question_ai_messages sequence
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM question_ai_messages;
    IF max_id = 0 THEN
        PERFORM setval('question_ai_messages_id_seq', 1, false);
    ELSE
        PERFORM setval('question_ai_messages_id_seq', max_id, true);
    END IF;
    RAISE NOTICE 'Fixed question_ai_messages_id_seq to %', max_id;

END $$;
