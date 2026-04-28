-- ============================================================
-- V19: Orphan tablo + kolon temizliği
-- ============================================================
-- Hibernate ddl-auto=update zaman içinde eski entity sürümlerinden
-- kalan iki yapı bıraktı:
--
--   1) student_books tablosu — eski "StudentProfile.ownedBooks"
--      mapping'inden kalma. Mevcut entity User.ownedBooks artık
--      user_books'a yazıyor; student_books'taki veri "kayıp"
--      (kütüphane boş görünüyor). Burada veri user_books'a
--      taşınır ve student_books drop edilir.
--
--   2) discount_code_usages.student_profile_id kolonu — eski bir
--      DiscountCodeUsage entity sürümünden kalma. Şu anki entity
--      bu alanı doldurmadığı için kolon NOT NULL olmasına rağmen
--      yeni INSERT'lerde NULL kalır → constraint violation.
--      Kolon ve FK'si kaldırılır.
--
-- Idempotent: koşulun zaten çözülmüş olduğu durumlarda guard'lar
-- sayesinde no-op çalışır.
-- ============================================================

-- ============================================================
-- 1) student_books → user_books veri taşıma + drop
-- ============================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
         WHERE table_schema = current_schema()
           AND table_name = 'student_books'
    ) THEN
        -- Veriyi taşı: student_profile_id → user_id mapping ile.
        -- ON CONFLICT DO NOTHING: aynı (user_id, book_id) zaten
        -- user_books'taysa atla.
        INSERT INTO user_books (user_id, book_id)
        SELECT sp.user_id, sb.book_id
          FROM student_books sb
          JOIN student_profiles sp ON sp.id = sb.student_profile_id
        ON CONFLICT (user_id, book_id) DO NOTHING;

        RAISE NOTICE 'Migrated % rows from student_books to user_books',
            (SELECT COUNT(*) FROM student_books);

        DROP TABLE student_books;
    END IF;
END $$;

-- ============================================================
-- 2) discount_code_usages.student_profile_id orphan kolon
-- ============================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = current_schema()
           AND table_name = 'discount_code_usages'
           AND column_name = 'student_profile_id'
    ) THEN
        ALTER TABLE discount_code_usages
            DROP CONSTRAINT IF EXISTS fk_discount_code_usages_student_profile;
        DROP INDEX IF EXISTS idx_discount_code_usage_student;
        ALTER TABLE discount_code_usages
            DROP COLUMN student_profile_id;

        RAISE NOTICE 'Dropped orphan column discount_code_usages.student_profile_id';
    END IF;
END $$;
