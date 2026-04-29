-- ============================================================
-- purchases: user_id → student_profile_id (STUDENT-only)
-- ============================================================
-- Purchase entity'si artık StudentProfile'a bağlı (student_profile_id).
-- Hibernate ddl-auto=update ile fark edilmiş ama Flyway migration'ı yoktu.
-- Bu migration şemayı entity ile hizalar.
--
-- Idempotent: Hibernate ddl-auto=update veya kısmi koşumlardan
-- gelen hibrit durumlara karşı dirençlidir.
-- ============================================================

-- 1. Yeni kolonu ekle
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS student_profile_id BIGINT;

-- 2. user_id hâlâ varsa veriyi taşı, yetimleri sil ve eski kolonu kaldır
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = current_schema()
           AND table_name = 'purchases'
           AND column_name = 'user_id'
    ) THEN
        UPDATE purchases p
           SET student_profile_id = sp.id
          FROM student_profiles sp
         WHERE sp.user_id = p.user_id
           AND p.student_profile_id IS NULL;

        DELETE FROM purchases WHERE student_profile_id IS NULL;

        ALTER TABLE purchases DROP CONSTRAINT IF EXISTS purchases_user_id_fkey;
        ALTER TABLE purchases DROP CONSTRAINT IF EXISTS fk_purchases_user;
        ALTER TABLE purchases DROP COLUMN user_id;
    END IF;
END $$;

-- 3. NOT NULL
ALTER TABLE purchases ALTER COLUMN student_profile_id SET NOT NULL;

-- 4. FK — Hibernate auto-isimli FK'leri temizle, bizimkini ekle
DO $$
DECLARE
    cons_name TEXT;
BEGIN
    FOR cons_name IN
        SELECT con.conname
          FROM pg_constraint con
          JOIN pg_attribute att
            ON att.attrelid = con.conrelid
           AND att.attnum = ANY(con.conkey)
         WHERE con.conrelid = 'purchases'::regclass
           AND con.contype = 'f'
           AND att.attname = 'student_profile_id'
           AND con.conname <> 'fk_purchases_student_profile'
    LOOP
        EXECUTE 'ALTER TABLE purchases DROP CONSTRAINT ' || quote_ident(cons_name);
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conrelid = 'purchases'::regclass
           AND conname = 'fk_purchases_student_profile'
    ) THEN
        ALTER TABLE purchases
            ADD CONSTRAINT fk_purchases_student_profile
            FOREIGN KEY (student_profile_id)
            REFERENCES student_profiles(id)
            ON DELETE CASCADE;
    END IF;
END $$;

-- 5. Index
CREATE INDEX IF NOT EXISTS idx_purchases_student_profile ON purchases(student_profile_id);
