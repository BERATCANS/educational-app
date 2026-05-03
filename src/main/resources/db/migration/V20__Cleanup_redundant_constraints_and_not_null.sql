-- ============================================================
-- V20: Yapısal kirlilik temizliği
-- ============================================================
-- 1) Hibernate'in zaman içinde bıraktığı hash-isimli yedek
--    UNIQUE constraint'leri kaldırır (named eşleniği zaten var).
-- 2) user_books ve discount_code_usages'taki hash-isimli FK'leri
--    açıklayıcı isimlerle yeniden yaratır.
-- 3) Entity'de NOT NULL olan ama V1'de gevşek kalan kolonları
--    NOT NULL'a alır. NULL satır varsa SET NOT NULL atlanır,
--    log'a NOTICE yazılır (operatör görür ve elle düzeltir).
--
-- Hepsi idempotent.
-- ============================================================

-- ============================================================
-- 1) Redundant Hibernate-isimli UNIQUE constraint'ler
-- ============================================================

ALTER TABLE users           DROP CONSTRAINT IF EXISTS uk6dotkott2kjsp8vw4d0m25fb7;
ALTER TABLE users           DROP CONSTRAINT IF EXISTS ukkwds03ohobcd8p6eowkw0f5bm;
ALTER TABLE users           DROP CONSTRAINT IF EXISTS ukr43af9ap4edm43mmtq01oddj6;
ALTER TABLE book_reviews    DROP CONSTRAINT IF EXISTS ukrqyw17eb0qpy1icpysmo91ya0;
ALTER TABLE daily_question_answers
                            DROP CONSTRAINT IF EXISTS uk875aahc2m85wd0hqlrfk0yxia;
ALTER TABLE question_ai_conversations
                            DROP CONSTRAINT IF EXISTS question_ai_conversations_user_id_question_id_key;

-- ============================================================
-- 2) user_books ve discount_code_usages'ta hash-isimli FK'leri
--    açıklayıcı isimlerle yeniden yarat
-- ============================================================

DO $$
DECLARE
    cons_name TEXT;
BEGIN
    -- user_books üzerindeki Hibernate-isimli FK'leri kaldır
    FOR cons_name IN
        SELECT conname FROM pg_constraint
         WHERE conrelid = 'user_books'::regclass
           AND contype = 'f'
           AND conname NOT IN ('fk_user_books_book', 'fk_user_books_user')
    LOOP
        EXECUTE 'ALTER TABLE user_books DROP CONSTRAINT ' || quote_ident(cons_name);
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conrelid = 'user_books'::regclass
           AND conname = 'fk_user_books_book'
    ) THEN
        ALTER TABLE user_books ADD CONSTRAINT fk_user_books_book
            FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conrelid = 'user_books'::regclass
           AND conname = 'fk_user_books_user'
    ) THEN
        ALTER TABLE user_books ADD CONSTRAINT fk_user_books_user
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;

    -- discount_code_usages.user_id üzerindeki Hibernate-isimli FK'yi kaldır
    FOR cons_name IN
        SELECT con.conname
          FROM pg_constraint con
          JOIN pg_attribute att
            ON att.attrelid = con.conrelid
           AND att.attnum = ANY(con.conkey)
         WHERE con.conrelid = 'discount_code_usages'::regclass
           AND con.contype = 'f'
           AND att.attname = 'user_id'
           AND con.conname <> 'fk_discount_code_usages_user'
    LOOP
        EXECUTE 'ALTER TABLE discount_code_usages DROP CONSTRAINT ' || quote_ident(cons_name);
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conrelid = 'discount_code_usages'::regclass
           AND conname = 'fk_discount_code_usages_user'
    ) THEN
        ALTER TABLE discount_code_usages ADD CONSTRAINT fk_discount_code_usages_user
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

-- ============================================================
-- 3) NOT NULL geri yükle (NULL satır varsa atla + NOTICE)
-- ============================================================

DO $$
BEGIN
    -- users
    IF NOT EXISTS (SELECT 1 FROM users WHERE first_name IS NULL) THEN
        ALTER TABLE users ALTER COLUMN first_name SET NOT NULL;
    ELSE
        RAISE NOTICE 'SKIP users.first_name SET NOT NULL: % NULL rows',
            (SELECT COUNT(*) FROM users WHERE first_name IS NULL);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM users WHERE last_name IS NULL) THEN
        ALTER TABLE users ALTER COLUMN last_name SET NOT NULL;
    ELSE
        RAISE NOTICE 'SKIP users.last_name SET NOT NULL: % NULL rows',
            (SELECT COUNT(*) FROM users WHERE last_name IS NULL);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM users WHERE email IS NULL) THEN
        ALTER TABLE users ALTER COLUMN email SET NOT NULL;
    ELSE
        RAISE NOTICE 'SKIP users.email SET NOT NULL: % NULL rows',
            (SELECT COUNT(*) FROM users WHERE email IS NULL);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM users WHERE phone_number IS NULL) THEN
        ALTER TABLE users ALTER COLUMN phone_number SET NOT NULL;
    ELSE
        RAISE NOTICE 'SKIP users.phone_number SET NOT NULL: % NULL rows',
            (SELECT COUNT(*) FROM users WHERE phone_number IS NULL);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM users WHERE username IS NULL) THEN
        ALTER TABLE users ALTER COLUMN username SET NOT NULL;
    ELSE
        RAISE NOTICE 'SKIP users.username SET NOT NULL: % NULL rows',
            (SELECT COUNT(*) FROM users WHERE username IS NULL);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM users WHERE password IS NULL) THEN
        ALTER TABLE users ALTER COLUMN password SET NOT NULL;
    ELSE
        RAISE NOTICE 'SKIP users.password SET NOT NULL: % NULL rows',
            (SELECT COUNT(*) FROM users WHERE password IS NULL);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM users WHERE role IS NULL) THEN
        ALTER TABLE users ALTER COLUMN role SET NOT NULL;
    ELSE
        RAISE NOTICE 'SKIP users.role SET NOT NULL: % NULL rows',
            (SELECT COUNT(*) FROM users WHERE role IS NULL);
    END IF;

    -- books
    IF NOT EXISTS (SELECT 1 FROM books WHERE title IS NULL) THEN
        ALTER TABLE books ALTER COLUMN title SET NOT NULL;
    ELSE
        RAISE NOTICE 'SKIP books.title SET NOT NULL: % NULL rows',
            (SELECT COUNT(*) FROM books WHERE title IS NULL);
    END IF;

    -- topics
    IF NOT EXISTS (SELECT 1 FROM topics WHERE title IS NULL) THEN
        ALTER TABLE topics ALTER COLUMN title SET NOT NULL;
    ELSE
        RAISE NOTICE 'SKIP topics.title SET NOT NULL: % NULL rows',
            (SELECT COUNT(*) FROM topics WHERE title IS NULL);
    END IF;

    -- tests
    IF NOT EXISTS (SELECT 1 FROM tests WHERE title IS NULL) THEN
        ALTER TABLE tests ALTER COLUMN title SET NOT NULL;
    ELSE
        RAISE NOTICE 'SKIP tests.title SET NOT NULL: % NULL rows',
            (SELECT COUNT(*) FROM tests WHERE title IS NULL);
    END IF;

    -- book_codes
    IF NOT EXISTS (SELECT 1 FROM book_codes WHERE code IS NULL) THEN
        ALTER TABLE book_codes ALTER COLUMN code SET NOT NULL;
    ELSE
        RAISE NOTICE 'SKIP book_codes.code SET NOT NULL: % NULL rows',
            (SELECT COUNT(*) FROM book_codes WHERE code IS NULL);
    END IF;

    -- discount_codes
    IF NOT EXISTS (SELECT 1 FROM discount_codes WHERE code IS NULL) THEN
        ALTER TABLE discount_codes ALTER COLUMN code SET NOT NULL;
    ELSE
        RAISE NOTICE 'SKIP discount_codes.code SET NOT NULL: % NULL rows',
            (SELECT COUNT(*) FROM discount_codes WHERE code IS NULL);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM discount_codes WHERE discount_type IS NULL) THEN
        ALTER TABLE discount_codes ALTER COLUMN discount_type SET NOT NULL;
    ELSE
        RAISE NOTICE 'SKIP discount_codes.discount_type SET NOT NULL: % NULL rows',
            (SELECT COUNT(*) FROM discount_codes WHERE discount_type IS NULL);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM discount_codes WHERE discount_value IS NULL) THEN
        ALTER TABLE discount_codes ALTER COLUMN discount_value SET NOT NULL;
    ELSE
        RAISE NOTICE 'SKIP discount_codes.discount_value SET NOT NULL: % NULL rows',
            (SELECT COUNT(*) FROM discount_codes WHERE discount_value IS NULL);
    END IF;
END $$;
