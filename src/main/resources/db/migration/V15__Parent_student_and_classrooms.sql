-- V15: Parent-Student linki ve Classroom yapısı (çoklu öğretmen destekli)

-- ============================================================
-- 1. student_profiles.parent_invite_code
-- ============================================================

ALTER TABLE student_profiles
    ADD COLUMN parent_invite_code VARCHAR(8) UNIQUE;

-- Mevcut öğrencilere benzersiz davet kodu üret (pgcrypto bağımlılığı olmadan).
-- MD5(random + id + clock) hash'inin ilk 6 karakteri; id ve clock_timestamp sayesinde
-- aynı transaction içinde dahi çakışma riski minimal.
UPDATE student_profiles
SET parent_invite_code = UPPER(SUBSTRING(
    MD5(random()::text || id::text || clock_timestamp()::text),
    1, 6
))
WHERE parent_invite_code IS NULL;

ALTER TABLE student_profiles
    ALTER COLUMN parent_invite_code SET NOT NULL;

-- ============================================================
-- 2. parent_student (ManyToMany: bir veli çoklu çocuk, bir çocuk çoklu veli)
-- ============================================================

CREATE TABLE parent_student (
    parent_profile_id  BIGINT NOT NULL REFERENCES parent_profiles(id)  ON DELETE CASCADE,
    student_profile_id BIGINT NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    linked_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (parent_profile_id, student_profile_id)
);

CREATE INDEX idx_parent_student_student ON parent_student(student_profile_id);

-- ============================================================
-- 3. classrooms (creator audit referansı; üyelik classroom_teacher tablosunda)
-- ============================================================

CREATE TABLE classrooms (
    id                          BIGSERIAL PRIMARY KEY,
    creator_teacher_profile_id  BIGINT NOT NULL REFERENCES teacher_profiles(id) ON DELETE RESTRICT,
    name                        VARCHAR(100) NOT NULL,
    description                 VARCHAR(500),
    grade_level                 VARCHAR(50),
    exam_type                   VARCHAR(50),
    student_join_code           VARCHAR(8) NOT NULL UNIQUE,
    teacher_join_code           VARCHAR(8) NOT NULL UNIQUE,
    is_active                   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_classrooms_creator ON classrooms(creator_teacher_profile_id);

-- ============================================================
-- 4. classroom_teacher (ManyToMany: bir sınıfta çoklu öğretmen)
-- ============================================================

CREATE TABLE classroom_teacher (
    classroom_id       BIGINT NOT NULL REFERENCES classrooms(id)       ON DELETE CASCADE,
    teacher_profile_id BIGINT NOT NULL REFERENCES teacher_profiles(id) ON DELETE CASCADE,
    joined_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (classroom_id, teacher_profile_id)
);

CREATE INDEX idx_classroom_teacher_teacher ON classroom_teacher(teacher_profile_id);

-- ============================================================
-- 5. classroom_student (ManyToMany: bir öğrenci çoklu sınıfta olabilir)
-- ============================================================

CREATE TABLE classroom_student (
    classroom_id       BIGINT NOT NULL REFERENCES classrooms(id)       ON DELETE CASCADE,
    student_profile_id BIGINT NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    joined_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (classroom_id, student_profile_id)
);

CREATE INDEX idx_classroom_student_student ON classroom_student(student_profile_id);
