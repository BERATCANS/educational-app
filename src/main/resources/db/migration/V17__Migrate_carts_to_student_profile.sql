-- ============================================================
-- carts: user_id → student_profile_id (STUDENT-only)
-- ============================================================
-- Cart entity ve servisi yalnızca öğrenci profillerini destekliyor
-- (CartService.getEligibleStudentProfile non-STUDENT'i reddediyor),
-- ancak DB şeması hâlâ user_id üzerinden bağlanıyordu. Bu migration
-- şemayı entity ile hizalar; öğrenci olmayan kullanıcılara ait
-- yetim sepetler (varsa) ve item'ları silinir.
--
-- Idempotent: Hibernate ddl-auto=update veya kısmi koşumlardan
-- gelen hibrit durumlara karşı dirençlidir.
-- ============================================================

-- 1. Yeni kolonu ekle
ALTER TABLE carts ADD COLUMN IF NOT EXISTS student_profile_id BIGINT;

-- 2. user_id hâlâ varsa veriyi taşı, yetimleri sil ve eski kolonu kaldır
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = current_schema()
           AND table_name = 'carts'
           AND column_name = 'user_id'
    ) THEN
        UPDATE carts c
           SET student_profile_id = sp.id
          FROM student_profiles sp
         WHERE sp.user_id = c.user_id
           AND c.student_profile_id IS NULL;

        DELETE FROM carts WHERE student_profile_id IS NULL;

        ALTER TABLE carts DROP CONSTRAINT IF EXISTS uk_carts_user;
        ALTER TABLE carts DROP CONSTRAINT IF EXISTS carts_user_id_key;
        ALTER TABLE carts DROP CONSTRAINT IF EXISTS carts_user_id_fkey;
        ALTER TABLE carts DROP COLUMN user_id;
    END IF;
END $$;

-- 3. NOT NULL
ALTER TABLE carts ALTER COLUMN student_profile_id SET NOT NULL;

-- 4. UNIQUE constraint — Hibernate auto-isimli çakışan UNIQUE'leri temizle,
--    bizimkini idempotent ekle
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
         WHERE con.conrelid = 'carts'::regclass
           AND con.contype = 'u'
           AND att.attname = 'student_profile_id'
           AND con.conname <> 'uk_carts_student_profile'
    LOOP
        EXECUTE 'ALTER TABLE carts DROP CONSTRAINT ' || quote_ident(cons_name);
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conrelid = 'carts'::regclass
           AND conname = 'uk_carts_student_profile'
    ) THEN
        ALTER TABLE carts ADD CONSTRAINT uk_carts_student_profile UNIQUE (student_profile_id);
    END IF;
END $$;

-- 5. FK — aynı mantık (Hibernate auto-isimli FK'leri temizle, bizimkini ekle)
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
         WHERE con.conrelid = 'carts'::regclass
           AND con.contype = 'f'
           AND att.attname = 'student_profile_id'
           AND con.conname <> 'fk_carts_student_profile'
    LOOP
        EXECUTE 'ALTER TABLE carts DROP CONSTRAINT ' || quote_ident(cons_name);
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conrelid = 'carts'::regclass
           AND conname = 'fk_carts_student_profile'
    ) THEN
        ALTER TABLE carts
            ADD CONSTRAINT fk_carts_student_profile
            FOREIGN KEY (student_profile_id)
            REFERENCES student_profiles(id)
            ON DELETE CASCADE;
    END IF;
END $$;

-- 6. Index
CREATE INDEX IF NOT EXISTS idx_carts_student_profile ON carts(student_profile_id);
