-- V16: Classroom.creator_teacher_profile_id artık nullable + ON DELETE SET NULL.
-- Sebep: Bir TEACHER hesabını silmek isteyince TeacherProfile cascade silinir;
-- daha önceki RESTRICT politikası bunu engelleyip 500 hatası veriyordu.
-- Yeni davranış: creator silinirse classroom yaşar, creator=NULL olur (audit izi kaybolur, sınıf yaşar).

ALTER TABLE classrooms
    DROP CONSTRAINT classrooms_creator_teacher_profile_id_fkey;

ALTER TABLE classrooms
    ALTER COLUMN creator_teacher_profile_id DROP NOT NULL;

ALTER TABLE classrooms
    ADD CONSTRAINT classrooms_creator_teacher_profile_id_fkey
    FOREIGN KEY (creator_teacher_profile_id)
    REFERENCES teacher_profiles(id)
    ON DELETE SET NULL;
