package com.beratcan.app.repository;

import com.beratcan.app.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    @Query("SELECT c FROM Classroom c JOIN c.teachers t WHERE t.id = :teacherProfileId")
    List<Classroom> findAllByTeacherProfileId(@Param("teacherProfileId") Long teacherProfileId);

    @Query("SELECT c FROM Classroom c JOIN c.teachers t WHERE t.id = :teacherProfileId AND c.isActive = true")
    List<Classroom> findActiveByTeacherProfileId(@Param("teacherProfileId") Long teacherProfileId);

    @Query("SELECT c FROM Classroom c JOIN c.students s WHERE s.id = :studentProfileId")
    List<Classroom> findAllByStudentProfileId(@Param("studentProfileId") Long studentProfileId);

    Optional<Classroom> findByStudentJoinCode(String studentJoinCode);

    Optional<Classroom> findByTeacherJoinCode(String teacherJoinCode);

    boolean existsByStudentJoinCode(String studentJoinCode);

    boolean existsByTeacherJoinCode(String teacherJoinCode);
}
