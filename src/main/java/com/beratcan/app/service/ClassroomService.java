package com.beratcan.app.service;

import com.beratcan.app.dto.request.CreateClassroomRequest;
import com.beratcan.app.dto.request.UpdateClassroomRequest;
import com.beratcan.app.dto.response.ActiveClassroomMembershipDto;
import com.beratcan.app.dto.response.ClassroomDetailDto;
import com.beratcan.app.dto.response.ClassroomDto;
import com.beratcan.app.dto.response.ClassroomSummaryDto;
import com.beratcan.app.dto.response.StudentSummaryDto;
import com.beratcan.app.dto.response.TeacherSummaryDto;
import com.beratcan.app.entity.Classroom;
import com.beratcan.app.entity.StudentProfile;
import com.beratcan.app.entity.TeacherProfile;
import com.beratcan.app.entity.User;
import com.beratcan.app.entity.enums.ExamType;
import com.beratcan.app.entity.enums.GradeLevel;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.ResourceNotFoundException;
import com.beratcan.app.exception.UnauthorizedOperationException;
import com.beratcan.app.repository.ClassroomRepository;
import com.beratcan.app.repository.StudentProfileRepository;
import com.beratcan.app.repository.TeacherProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final InviteCodeGenerator inviteCodeGenerator;

    // ============================================================
    // TEACHER ENDPOINTS
    // ============================================================

    @Transactional
    public ClassroomDto createClassroom(Long teacherUserId, CreateClassroomRequest request) {
        TeacherProfile teacher = loadTeacher(teacherUserId);

        Classroom classroom = new Classroom();
        classroom.setCreator(teacher);
        classroom.setName(request.getName());
        classroom.setDescription(request.getDescription());
        classroom.setGradeLevel(parseGradeLevel(request.getGradeLevel()));
        classroom.setExamType(parseExamType(request.getExamType()));
        classroom.setStudentJoinCode(inviteCodeGenerator.generateUnique(classroomRepository::existsByStudentJoinCode));
        classroom.setTeacherJoinCode(inviteCodeGenerator.generateUnique(classroomRepository::existsByTeacherJoinCode));
        classroom.setIsActive(true);
        classroom.getTeachers().add(teacher);

        Classroom saved = classroomRepository.save(classroom);
        log.info("Classroom created: id={}, creatorTeacherId={}, name={}",
                saved.getId(), teacher.getId(), saved.getName());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ClassroomDto> listClassroomsForTeacher(Long teacherUserId) {
        TeacherProfile teacher = loadTeacher(teacherUserId);
        return classroomRepository.findAllByTeacherProfileId(teacher.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClassroomDetailDto getClassroomDetail(Long teacherUserId, Long classroomId) {
        TeacherProfile teacher = loadTeacher(teacherUserId);
        Classroom classroom = loadClassroomAsMember(classroomId, teacher);

        List<TeacherSummaryDto> teacherDtos = classroom.getTeachers().stream()
                .map(t -> TeacherSummaryDto.builder()
                        .teacherProfileId(t.getId())
                        .firstName(t.getUser().getFirstName())
                        .lastName(t.getUser().getLastName())
                        .username(t.getUser().getUsername())
                        .isCreator(classroom.getCreator() != null && classroom.getCreator().getId().equals(t.getId()))
                        .build())
                .toList();

        List<StudentSummaryDto> studentDtos = classroom.getStudents().stream()
                .map(this::toStudentSummary)
                .toList();

        return ClassroomDetailDto.builder()
                .id(classroom.getId())
                .name(classroom.getName())
                .description(classroom.getDescription())
                .gradeLevel(classroom.getGradeLevel() != null ? classroom.getGradeLevel().name() : null)
                .examType(classroom.getExamType() != null ? classroom.getExamType().name() : null)
                .studentJoinCode(classroom.getStudentJoinCode())
                .teacherJoinCode(classroom.getTeacherJoinCode())
                .isActive(classroom.getIsActive())
                .createdAt(classroom.getCreatedAt())
                .teachers(teacherDtos)
                .students(studentDtos)
                .build();
    }

    @Transactional
    public ClassroomDto updateClassroom(Long teacherUserId, Long classroomId, UpdateClassroomRequest request) {
        TeacherProfile teacher = loadTeacher(teacherUserId);
        Classroom classroom = loadClassroomAsMember(classroomId, teacher);

        if (request.getName() != null && !request.getName().isBlank()) {
            classroom.setName(request.getName());
        }
        if (request.getDescription() != null) {
            classroom.setDescription(request.getDescription());
        }
        if (request.getGradeLevel() != null) {
            classroom.setGradeLevel(parseGradeLevel(request.getGradeLevel()));
        }
        if (request.getExamType() != null) {
            classroom.setExamType(parseExamType(request.getExamType()));
        }

        return toDto(classroom);
    }

    @Transactional
    public void deactivateClassroom(Long teacherUserId, Long classroomId) {
        TeacherProfile teacher = loadTeacher(teacherUserId);
        Classroom classroom = loadClassroomAsMember(classroomId, teacher);
        classroom.setIsActive(false);
        log.info("Classroom deactivated: id={}, by teacherId={}", classroomId, teacher.getId());
    }

    @Transactional
    public void removeStudent(Long teacherUserId, Long classroomId, Long studentProfileId) {
        TeacherProfile teacher = loadTeacher(teacherUserId);
        Classroom classroom = loadClassroomAsMember(classroomId, teacher);
        boolean removed = classroom.getStudents().removeIf(s -> s.getId().equals(studentProfileId));
        if (!removed) {
            throw new ResourceNotFoundException("Bu öğrenci sınıfta değil");
        }
        log.info("Student removed from classroom: classroomId={}, studentProfileId={}",
                classroomId, studentProfileId);
    }

    @Transactional
    public void removeTeacher(Long callerUserId, Long classroomId, Long teacherProfileIdToRemove) {
        TeacherProfile caller = loadTeacher(callerUserId);
        Classroom classroom = loadClassroomAsMember(classroomId, caller);

        TeacherProfile target = classroom.getTeachers().stream()
                .filter(t -> t.getId().equals(teacherProfileIdToRemove))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Bu öğretmen sınıfta değil"));

        if (Boolean.TRUE.equals(classroom.getIsActive()) && classroom.getTeachers().size() <= 1) {
            throw new BadRequestException(
                    "Aktif sınıftaki son öğretmen çıkarılamaz. Önce başka öğretmen davet edin veya sınıfı kapatın.");
        }

        classroom.getTeachers().remove(target);
        log.info("Teacher removed from classroom: classroomId={}, teacherProfileId={}, byUserId={}",
                classroomId, teacherProfileIdToRemove, callerUserId);
    }

    @Transactional
    public ClassroomDto joinAsCoTeacher(Long teacherUserId, String teacherJoinCode) {
        if (teacherJoinCode == null || teacherJoinCode.isBlank()) {
            throw new BadRequestException("Öğretmen kodu boş olamaz");
        }
        String normalized = teacherJoinCode.trim().toUpperCase();
        TeacherProfile teacher = loadTeacher(teacherUserId);

        Classroom classroom = classroomRepository.findByTeacherJoinCode(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Geçersiz öğretmen kodu"));

        if (Boolean.FALSE.equals(classroom.getIsActive())) {
            throw new BadRequestException("Bu sınıf kapalı, katılamazsınız");
        }

        if (classroom.getTeachers().add(teacher)) {
            log.info("Co-teacher joined classroom: classroomId={}, teacherProfileId={}",
                    classroom.getId(), teacher.getId());
        }
        return toDto(classroom);
    }

    @Transactional
    public ClassroomDto regenerateStudentJoinCode(Long teacherUserId, Long classroomId) {
        TeacherProfile teacher = loadTeacher(teacherUserId);
        Classroom classroom = loadClassroomAsMember(classroomId, teacher);
        classroom.setStudentJoinCode(inviteCodeGenerator.generateUnique(classroomRepository::existsByStudentJoinCode));
        log.info("Student join code regenerated: classroomId={}", classroomId);
        return toDto(classroom);
    }

    @Transactional
    public ClassroomDto regenerateTeacherJoinCode(Long teacherUserId, Long classroomId) {
        TeacherProfile teacher = loadTeacher(teacherUserId);
        Classroom classroom = loadClassroomAsMember(classroomId, teacher);
        classroom.setTeacherJoinCode(inviteCodeGenerator.generateUnique(classroomRepository::existsByTeacherJoinCode));
        log.info("Teacher join code regenerated: classroomId={}", classroomId);
        return toDto(classroom);
    }

    // ============================================================
    // STUDENT ENDPOINTS
    // ============================================================

    @Transactional(readOnly = true)
    public List<ClassroomSummaryDto> listClassroomsForStudent(Long studentUserId) {
        StudentProfile student = loadStudent(studentUserId);
        return classroomRepository.findAllByStudentProfileId(student.getId()).stream()
                .map(this::toSummaryForStudent)
                .toList();
    }

    @Transactional
    public ClassroomSummaryDto joinAsStudent(Long studentUserId, String studentJoinCode) {
        if (studentJoinCode == null || studentJoinCode.isBlank()) {
            throw new BadRequestException("Sınıf kodu boş olamaz");
        }
        String normalized = studentJoinCode.trim().toUpperCase();
        StudentProfile student = loadStudent(studentUserId);

        Classroom classroom = classroomRepository.findByStudentJoinCode(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Geçersiz sınıf kodu"));

        if (Boolean.FALSE.equals(classroom.getIsActive())) {
            throw new BadRequestException("Bu sınıf kapalı, katılamazsınız");
        }

        if (classroom.getStudents().add(student)) {
            log.info("Student joined classroom: classroomId={}, studentProfileId={}",
                    classroom.getId(), student.getId());
        }
        return toSummaryForStudent(classroom);
    }

    @Transactional
    public void leaveClassroom(Long studentUserId, Long classroomId) {
        StudentProfile student = loadStudent(studentUserId);
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Sınıf bulunamadı"));

        boolean removed = classroom.getStudents().removeIf(s -> s.getId().equals(student.getId()));
        if (!removed) {
            throw new ResourceNotFoundException("Bu sınıfa kayıtlı değilsiniz");
        }
        log.info("Student left classroom: classroomId={}, studentProfileId={}",
                classroomId, student.getId());
    }

    // ============================================================
    // ACCOUNT DELETION GUARD
    // ============================================================

    @Transactional(readOnly = true)
    public List<ActiveClassroomMembershipDto> findActiveMembershipsForTeacher(Long teacherUserId) {
        TeacherProfile teacher = teacherProfileRepository.findByUserId(teacherUserId).orElse(null);
        if (teacher == null) {
            return List.of();
        }
        return classroomRepository.findActiveByTeacherProfileId(teacher.getId()).stream()
                .map(c -> ActiveClassroomMembershipDto.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .isLastTeacher(c.getTeachers().size() <= 1)
                        .build())
                .toList();
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private TeacherProfile loadTeacher(Long userId) {
        return teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Öğretmen profili bulunamadı"));
    }

    private StudentProfile loadStudent(Long userId) {
        return studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Öğrenci profili bulunamadı"));
    }

    private Classroom loadClassroomAsMember(Long classroomId, TeacherProfile teacher) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Sınıf bulunamadı"));
        Set<TeacherProfile> teachers = classroom.getTeachers();
        boolean isMember = teachers.stream().anyMatch(t -> t.getId().equals(teacher.getId()));
        if (!isMember) {
            throw new UnauthorizedOperationException("Bu sınıfa erişim yetkiniz yok");
        }
        return classroom;
    }

    private GradeLevel parseGradeLevel(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return GradeLevel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Geçersiz sınıf seviyesi: " + value);
        }
    }

    private ExamType parseExamType(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return ExamType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Geçersiz sınav türü: " + value);
        }
    }

    private ClassroomDto toDto(Classroom classroom) {
        return ClassroomDto.builder()
                .id(classroom.getId())
                .name(classroom.getName())
                .description(classroom.getDescription())
                .gradeLevel(classroom.getGradeLevel() != null ? classroom.getGradeLevel().name() : null)
                .examType(classroom.getExamType() != null ? classroom.getExamType().name() : null)
                .studentJoinCode(classroom.getStudentJoinCode())
                .teacherJoinCode(classroom.getTeacherJoinCode())
                .isActive(classroom.getIsActive())
                .studentCount(classroom.getStudents().size())
                .teacherCount(classroom.getTeachers().size())
                .createdAt(classroom.getCreatedAt())
                .build();
    }

    private ClassroomSummaryDto toSummaryForStudent(Classroom classroom) {
        List<String> teacherNames = classroom.getTeachers().stream()
                .map(t -> t.getUser().getFirstName() + " " + t.getUser().getLastName())
                .toList();
        return ClassroomSummaryDto.builder()
                .id(classroom.getId())
                .name(classroom.getName())
                .gradeLevel(classroom.getGradeLevel() != null ? classroom.getGradeLevel().name() : null)
                .examType(classroom.getExamType() != null ? classroom.getExamType().name() : null)
                .teacherNames(teacherNames)
                .build();
    }

    private StudentSummaryDto toStudentSummary(StudentProfile student) {
        User user = student.getUser();
        return StudentSummaryDto.builder()
                .studentProfileId(student.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .username(user.getUsername())
                .gradeLevel(student.getGradeLevel() != null ? student.getGradeLevel().name() : null)
                .build();
    }
}
