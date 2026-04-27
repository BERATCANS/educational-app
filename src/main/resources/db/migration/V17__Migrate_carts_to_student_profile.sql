-- ============================================================
-- carts: user_id → student_profile_id (STUDENT-only)
-- ============================================================
-- Cart entity ve servisi yalnızca öğrenci profillerini destekliyor
-- (CartService.getEligibleStudentProfile non-STUDENT'i reddediyor),
-- ancak DB şeması hâlâ user_id üzerinden bağlanıyordu. Bu migration
-- şemayı entity ile hizalar; öğrenci olmayan kullanıcılara ait
-- yetim sepetler (varsa) ve item'ları silinir.
-- ============================================================

-- 1. Yeni kolonu ekle (nullable başlat, doldurduktan sonra NOT NULL yap)
ALTER TABLE carts ADD COLUMN student_profile_id BIGINT;

-- 2. Mevcut sepetleri öğrenci profiline eşle
UPDATE carts c
SET student_profile_id = sp.id
FROM student_profiles sp
WHERE sp.user_id = c.user_id;

-- 3. Yetim sepetleri sil (user_id'si bir student_profile'a denk gelmeyenler:
--    TEACHER/PARENT/ADMIN). cart_items ON DELETE CASCADE ile temizlenir.
DELETE FROM carts WHERE student_profile_id IS NULL;

-- 4. NOT NULL + benzersiz kısıt
ALTER TABLE carts ALTER COLUMN student_profile_id SET NOT NULL;
ALTER TABLE carts ADD CONSTRAINT uk_carts_student_profile UNIQUE (student_profile_id);

-- 5. FK ekle
ALTER TABLE carts ADD CONSTRAINT fk_carts_student_profile
    FOREIGN KEY (student_profile_id) REFERENCES student_profiles(id) ON DELETE CASCADE;

-- 6. Eski user_id kısıtları ve kolonu kaldır
ALTER TABLE carts DROP CONSTRAINT IF EXISTS uk_carts_user;
ALTER TABLE carts DROP CONSTRAINT IF EXISTS carts_user_id_key;
ALTER TABLE carts DROP CONSTRAINT IF EXISTS carts_user_id_fkey;
ALTER TABLE carts DROP COLUMN user_id;

-- 7. Index
CREATE INDEX idx_carts_student_profile ON carts(student_profile_id);
