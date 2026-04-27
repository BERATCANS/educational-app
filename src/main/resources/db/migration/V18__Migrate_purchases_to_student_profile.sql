-- ============================================================
-- purchases: user_id → student_profile_id (STUDENT-only)
-- ============================================================
-- Purchase entity'si artık StudentProfile'a bağlı (student_profile_id).
-- Hibernate ddl-auto=update ile fark edilmiş ama Flyway migration'ı yoktu.
-- Bu migration şemayı entity ile hizalar.
-- ============================================================

-- 1. Yeni kolonu nullable olarak ekle
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS student_profile_id BIGINT;

-- 2. Mevcut purchase'ları öğrenci profiline eşle
UPDATE purchases p
SET student_profile_id = sp.id
FROM student_profiles sp
WHERE sp.user_id = p.user_id
  AND p.student_profile_id IS NULL;

-- 3. Yetim purchase'ları temizle (TEACHER/PARENT/ADMIN'e ait — business logic
--    zaten engelliyor ama eski test verisi olabilir). purchase_items CASCADE
--    ile temizlenir.
DELETE FROM purchases WHERE student_profile_id IS NULL;

-- 4. NOT NULL kısıtı
ALTER TABLE purchases ALTER COLUMN student_profile_id SET NOT NULL;

-- 5. FK ekle
ALTER TABLE purchases ADD CONSTRAINT fk_purchases_student_profile
    FOREIGN KEY (student_profile_id) REFERENCES student_profiles(id) ON DELETE CASCADE;

-- 6. Eski user_id kısıtları ve kolonu kaldır
ALTER TABLE purchases DROP CONSTRAINT IF EXISTS purchases_user_id_fkey;
ALTER TABLE purchases DROP CONSTRAINT IF EXISTS fk_purchases_user;
ALTER TABLE purchases DROP COLUMN IF EXISTS user_id;

-- 7. Index
CREATE INDEX IF NOT EXISTS idx_purchases_student_profile ON purchases(student_profile_id);