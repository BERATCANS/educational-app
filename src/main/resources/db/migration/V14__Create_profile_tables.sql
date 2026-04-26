-- V14: Create profile tables and migrate student-specific data
-- Composition/Profile pattern: users tablosu ortak alanları tutar,
-- rol-spesifik veriler ayrı profil tablolarında tutulur.

-- ============================================================
-- 1. Profil tablolarını oluştur
-- ============================================================

CREATE TABLE student_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    grade_level VARCHAR(50),
    exam_type VARCHAR(50),
    field_type VARCHAR(50),
    school_type VARCHAR(50),
    daily_question_target INT NOT NULL DEFAULT 200,
    daily_solved_questions INT NOT NULL DEFAULT 0,
    current_streak INT NOT NULL DEFAULT 0,
    longest_streak INT NOT NULL DEFAULT 0,
    last_activity_date DATE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE teacher_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE admin_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE parent_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE super_admin_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- ============================================================
-- 2. Mevcut verileri profil tablolarına migrate et
-- ============================================================

INSERT INTO student_profiles (user_id, grade_level, exam_type, field_type, school_type,
    daily_question_target, daily_solved_questions, current_streak, longest_streak, last_activity_date)
SELECT id, grade_level, exam_type, field_type, school_type,
    daily_question_target, daily_solved_questions, current_streak, longest_streak, last_activity_date
FROM users WHERE role = 'STUDENT';

INSERT INTO teacher_profiles (user_id)
SELECT id FROM users WHERE role = 'TEACHER';

INSERT INTO admin_profiles (user_id)
SELECT id FROM users WHERE role = 'ADMIN';

-- ============================================================
-- 2.1. Student-specific tablolarda verisi olan ama profili olmayan
-- kullanıcılar için güvenli backfill
-- ============================================================

WITH referenced_student_users AS (
    SELECT user_id FROM user_flagged_questions
    UNION
    SELECT user_id FROM test_results
    UNION
    SELECT user_id FROM book_reviews
    UNION
    SELECT user_id FROM daily_question_answers
)
INSERT INTO student_profiles (
    user_id,
    grade_level,
    exam_type,
    field_type,
    school_type,
    daily_question_target,
    daily_solved_questions,
    current_streak,
    longest_streak,
    last_activity_date
)
SELECT
    u.id,
    u.grade_level,
    u.exam_type,
    u.field_type,
    u.school_type,
    u.daily_question_target,
    u.daily_solved_questions,
    u.current_streak,
    u.longest_streak,
    u.last_activity_date
FROM users u
JOIN referenced_student_users rsu ON rsu.user_id = u.id
LEFT JOIN student_profiles sp ON sp.user_id = u.id
WHERE sp.id IS NULL;

-- ============================================================
-- 4. Flagged questions: user_flagged_questions → student_flagged_questions
-- ============================================================

CREATE TABLE student_flagged_questions (
    student_profile_id BIGINT NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    CONSTRAINT uk_student_flagged_question UNIQUE (student_profile_id, question_id)
);

INSERT INTO student_flagged_questions (student_profile_id, question_id)
SELECT sp.id, ufq.question_id
FROM user_flagged_questions ufq
JOIN student_profiles sp ON sp.user_id = ufq.user_id;

-- ============================================================
-- 4. test_results: user_id → student_profile_id
-- ============================================================

ALTER TABLE test_results ADD COLUMN student_profile_id BIGINT;

UPDATE test_results tr
SET student_profile_id = sp.id
FROM student_profiles sp
WHERE sp.user_id = tr.user_id;

ALTER TABLE test_results ALTER COLUMN student_profile_id SET NOT NULL;

ALTER TABLE test_results ADD CONSTRAINT fk_test_results_student_profile
    FOREIGN KEY (student_profile_id) REFERENCES student_profiles(id) ON DELETE CASCADE;

-- ============================================================
-- 5. carts user bazlı kalır
-- ============================================================

ALTER TABLE carts ADD CONSTRAINT uk_carts_user UNIQUE (user_id);

-- ============================================================
-- 6. purchases user bazlı kalır
-- ============================================================

-- ============================================================
-- 7. book_reviews: user_id → student_profile_id
-- ============================================================

-- Önce mevcut unique constraint'i düşür (book_id, user_id)
ALTER TABLE book_reviews DROP CONSTRAINT IF EXISTS book_reviews_book_id_user_id_key;

ALTER TABLE book_reviews ADD COLUMN student_profile_id BIGINT;

UPDATE book_reviews br
SET student_profile_id = sp.id
FROM student_profiles sp
WHERE sp.user_id = br.user_id;

ALTER TABLE book_reviews ALTER COLUMN student_profile_id SET NOT NULL;

ALTER TABLE book_reviews ADD CONSTRAINT uk_book_reviews_book_student UNIQUE (book_id, student_profile_id);
ALTER TABLE book_reviews ADD CONSTRAINT fk_book_reviews_student_profile
    FOREIGN KEY (student_profile_id) REFERENCES student_profiles(id) ON DELETE CASCADE;

-- ============================================================
-- 8. daily_question_answers: user_id → student_profile_id
-- ============================================================

-- Önce mevcut unique constraint'i düşür (daily_question_id, user_id)
ALTER TABLE daily_question_answers DROP CONSTRAINT IF EXISTS daily_question_answers_daily_question_id_user_id_key;

ALTER TABLE daily_question_answers ADD COLUMN student_profile_id BIGINT;

UPDATE daily_question_answers dqa
SET student_profile_id = sp.id
FROM student_profiles sp
WHERE sp.user_id = dqa.user_id;

ALTER TABLE daily_question_answers ALTER COLUMN student_profile_id SET NOT NULL;

ALTER TABLE daily_question_answers ADD CONSTRAINT uk_daily_question_answers_student UNIQUE (daily_question_id, student_profile_id);
ALTER TABLE daily_question_answers ADD CONSTRAINT fk_daily_question_answers_student_profile
    FOREIGN KEY (student_profile_id) REFERENCES student_profiles(id) ON DELETE CASCADE;

-- ============================================================
-- 9. discount_code_usages user bazlı kalır
-- ============================================================

-- ============================================================
-- 10. Eski user_id FK'larını ve kolonlarını kaldır
-- ============================================================

-- test_results
ALTER TABLE test_results DROP CONSTRAINT IF EXISTS test_results_user_id_fkey;
ALTER TABLE test_results DROP COLUMN IF EXISTS user_id;

-- book_reviews
ALTER TABLE book_reviews DROP CONSTRAINT IF EXISTS book_reviews_user_id_fkey;
ALTER TABLE book_reviews DROP COLUMN IF EXISTS user_id;

-- daily_question_answers
ALTER TABLE daily_question_answers DROP CONSTRAINT IF EXISTS daily_question_answers_user_id_fkey;
ALTER TABLE daily_question_answers DROP COLUMN IF EXISTS user_id;

-- ============================================================
-- 11. Öğrenciye özel alanları users tablosundan kaldır
-- ============================================================

ALTER TABLE users DROP COLUMN IF EXISTS grade_level;
ALTER TABLE users DROP COLUMN IF EXISTS exam_type;
ALTER TABLE users DROP COLUMN IF EXISTS field_type;
ALTER TABLE users DROP COLUMN IF EXISTS school_type;
ALTER TABLE users DROP COLUMN IF EXISTS daily_question_target;
ALTER TABLE users DROP COLUMN IF EXISTS daily_solved_questions;
ALTER TABLE users DROP COLUMN IF EXISTS current_streak;
ALTER TABLE users DROP COLUMN IF EXISTS longest_streak;
ALTER TABLE users DROP COLUMN IF EXISTS last_activity_date;

-- ============================================================
-- 12. Eski flagged questions tablosunu kaldır (user_books kalıyor!)
-- ============================================================

DROP TABLE IF EXISTS user_flagged_questions;

-- ============================================================
-- 13. Index'leri güncelle
-- ============================================================

CREATE INDEX idx_student_profile_user_id ON student_profiles(user_id);
CREATE INDEX idx_test_results_student_profile ON test_results(student_profile_id);
CREATE INDEX idx_purchases_user_id ON purchases(user_id);
CREATE INDEX idx_discount_code_usage_user ON discount_code_usages(user_id);
CREATE INDEX idx_book_review_student_profile ON book_reviews(student_profile_id);
