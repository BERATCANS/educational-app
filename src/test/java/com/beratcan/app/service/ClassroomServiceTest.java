package com.beratcan.app.service;

import com.beratcan.app.dto.request.CreateClassroomRequest;
import com.beratcan.app.dto.request.UpdateClassroomRequest;
import com.beratcan.app.dto.response.ActiveClassroomMembershipDto;
import com.beratcan.app.dto.response.ClassroomDetailDto;
import com.beratcan.app.dto.response.ClassroomDto;
import com.beratcan.app.dto.response.ClassroomSummaryDto;
import com.beratcan.app.entity.Classroom;
import com.beratcan.app.entity.StudentProfile;
import com.beratcan.app.entity.TeacherProfile;
import com.beratcan.app.entity.User;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.ResourceNotFoundException;
import com.beratcan.app.exception.UnauthorizedOperationException;
import com.beratcan.app.repository.ClassroomRepository;
import com.beratcan.app.repository.StudentProfileRepository;
import com.beratcan.app.repository.TeacherProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassroomServiceTest {

    @Mock private ClassroomRepository classroomRepository;
    @Mock private TeacherProfileRepository teacherProfileRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private InviteCodeGenerator inviteCodeGenerator;

    @InjectMocks private ClassroomService service;

    private TeacherProfile teacherA;
    private TeacherProfile teacherB;
    private StudentProfile student;
    private Classroom classroom;

    @BeforeEach
    void setUp() {
        User userA = new User();
        userA.setId(100L);
        userA.setFirstName("Ada");
        userA.setLastName("Lovelace");
        userA.setUsername("ada");
        teacherA = new TeacherProfile();
        teacherA.setId(1L);
        teacherA.setUser(userA);

        User userB = new User();
        userB.setId(200L);
        userB.setFirstName("Bertrand");
        userB.setLastName("Russell");
        userB.setUsername("bert");
        teacherB = new TeacherProfile();
        teacherB.setId(2L);
        teacherB.setUser(userB);

        User userS = new User();
        userS.setId(300L);
        userS.setFirstName("Stu");
        userS.setLastName("Dent");
        userS.setUsername("stu");
        student = new StudentProfile();
        student.setId(3L);
        student.setUser(userS);

        classroom = new Classroom();
        classroom.setId(50L);
        classroom.setName("Math 11-A");
        classroom.setCreator(teacherA);
        classroom.setStudentJoinCode("STU111");
        classroom.setTeacherJoinCode("TCH222");
        classroom.setIsActive(true);
        classroom.setTeachers(new HashSet<>(List.of(teacherA)));
        classroom.setStudents(new HashSet<>());
    }

    // ============================================================
    // CREATE CLASSROOM
    // ============================================================

    @Test
    void createClassroom_creatorIsAutomaticallyMember_andCodesGenerated() {
        when(teacherProfileRepository.findByUserId(100L)).thenReturn(Optional.of(teacherA));
        when(inviteCodeGenerator.generateUnique(any())).thenReturn("CODEAA", "CODEBB");
        when(classroomRepository.save(any(Classroom.class))).thenAnswer(inv -> {
            Classroom c = inv.getArgument(0);
            c.setId(99L);
            return c;
        });

        CreateClassroomRequest req = new CreateClassroomRequest();
        req.setName("Math 12-B");

        ClassroomDto result = service.createClassroom(100L, req);

        assertNotNull(result);
        assertEquals("Math 12-B", result.getName());
        assertEquals("CODEAA", result.getStudentJoinCode());
        assertEquals("CODEBB", result.getTeacherJoinCode());
        assertTrue(result.getIsActive());
        assertEquals(1, result.getTeacherCount()); // creator otomatik üye

        verify(classroomRepository).save(argThat(c ->
                c.getTeachers().contains(teacherA) && c.getCreator().equals(teacherA)));
    }

    @Test
    void createClassroom_teacherProfileMissing_throwsBadRequest() {
        when(teacherProfileRepository.findByUserId(100L)).thenReturn(Optional.empty());
        CreateClassroomRequest req = new CreateClassroomRequest();
        req.setName("X");

        assertThrows(BadRequestException.class,
                () -> service.createClassroom(100L, req));
    }

    // ============================================================
    // ACCESS CONTROL
    // ============================================================

    @Test
    void getClassroomDetail_nonMemberTeacher_throwsUnauthorized() {
        when(teacherProfileRepository.findByUserId(200L)).thenReturn(Optional.of(teacherB));
        when(classroomRepository.findById(50L)).thenReturn(Optional.of(classroom));

        assertThrows(UnauthorizedOperationException.class,
                () -> service.getClassroomDetail(200L, 50L));
    }

    @Test
    void getClassroomDetail_memberTeacher_returnsDetailWithTeachers() {
        when(teacherProfileRepository.findByUserId(100L)).thenReturn(Optional.of(teacherA));
        when(classroomRepository.findById(50L)).thenReturn(Optional.of(classroom));

        ClassroomDetailDto result = service.getClassroomDetail(100L, 50L);

        assertEquals(1, result.getTeachers().size());
        assertEquals(1L, result.getTeachers().get(0).getTeacherProfileId());
        assertTrue(result.getTeachers().get(0).getIsCreator());
        assertEquals("STU111", result.getStudentJoinCode());
    }

    @Test
    void updateClassroom_nonMember_throwsUnauthorized() {
        when(teacherProfileRepository.findByUserId(200L)).thenReturn(Optional.of(teacherB));
        when(classroomRepository.findById(50L)).thenReturn(Optional.of(classroom));

        UpdateClassroomRequest req = new UpdateClassroomRequest();
        req.setName("Hacked");

        assertThrows(UnauthorizedOperationException.class,
                () -> service.updateClassroom(200L, 50L, req));
    }

    @Test
    void updateClassroom_memberOnlyChangesProvidedFields() {
        when(teacherProfileRepository.findByUserId(100L)).thenReturn(Optional.of(teacherA));
        when(classroomRepository.findById(50L)).thenReturn(Optional.of(classroom));

        UpdateClassroomRequest req = new UpdateClassroomRequest();
        req.setDescription("Updated description");
        // name null → değişmemeli

        ClassroomDto result = service.updateClassroom(100L, 50L, req);

        assertEquals("Math 11-A", result.getName());
        assertEquals("Updated description", result.getDescription());
    }

    // ============================================================
    // DEACTIVATE
    // ============================================================

    @Test
    void deactivateClassroom_setsIsActiveFalse() {
        when(teacherProfileRepository.findByUserId(100L)).thenReturn(Optional.of(teacherA));
        when(classroomRepository.findById(50L)).thenReturn(Optional.of(classroom));

        service.deactivateClassroom(100L, 50L);

        assertFalse(classroom.getIsActive());
    }

    // ============================================================
    // STUDENT MANAGEMENT
    // ============================================================

    @Test
    void removeStudent_existingStudent_removesFromClassroom() {
        classroom.getStudents().add(student);
        when(teacherProfileRepository.findByUserId(100L)).thenReturn(Optional.of(teacherA));
        when(classroomRepository.findById(50L)).thenReturn(Optional.of(classroom));

        service.removeStudent(100L, 50L, 3L);

        assertTrue(classroom.getStudents().isEmpty());
    }

    @Test
    void removeStudent_notInClassroom_throwsResourceNotFound() {
        when(teacherProfileRepository.findByUserId(100L)).thenReturn(Optional.of(teacherA));
        when(classroomRepository.findById(50L)).thenReturn(Optional.of(classroom));

        assertThrows(ResourceNotFoundException.class,
                () -> service.removeStudent(100L, 50L, 999L));
    }

    @Test
    void joinAsStudent_validCode_addsStudent() {
        when(studentProfileRepository.findByUserId(300L)).thenReturn(Optional.of(student));
        when(classroomRepository.findByStudentJoinCode("STU111")).thenReturn(Optional.of(classroom));

        ClassroomSummaryDto result = service.joinAsStudent(300L, "STU111");

        assertEquals(50L, result.getId());
        assertTrue(classroom.getStudents().contains(student));
    }

    @Test
    void joinAsStudent_inactiveClassroom_throwsBadRequest() {
        classroom.setIsActive(false);
        when(studentProfileRepository.findByUserId(300L)).thenReturn(Optional.of(student));
        when(classroomRepository.findByStudentJoinCode("STU111")).thenReturn(Optional.of(classroom));

        assertThrows(BadRequestException.class,
                () -> service.joinAsStudent(300L, "STU111"));
    }

    @Test
    void joinAsStudent_isIdempotent() {
        classroom.getStudents().add(student);
        when(studentProfileRepository.findByUserId(300L)).thenReturn(Optional.of(student));
        when(classroomRepository.findByStudentJoinCode("STU111")).thenReturn(Optional.of(classroom));

        service.joinAsStudent(300L, "STU111");

        assertEquals(1, classroom.getStudents().size());
    }

    @Test
    void joinAsStudent_normalizesCode() {
        when(studentProfileRepository.findByUserId(300L)).thenReturn(Optional.of(student));
        when(classroomRepository.findByStudentJoinCode("STU111")).thenReturn(Optional.of(classroom));

        service.joinAsStudent(300L, "  stu111  ");

        verify(classroomRepository).findByStudentJoinCode("STU111");
    }

    @Test
    void joinAsStudent_invalidCode_throwsResourceNotFound() {
        when(studentProfileRepository.findByUserId(300L)).thenReturn(Optional.of(student));
        when(classroomRepository.findByStudentJoinCode("XXXXXX")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.joinAsStudent(300L, "XXXXXX"));
    }

    @Test
    void leaveClassroom_existingStudent_removes() {
        classroom.getStudents().add(student);
        when(studentProfileRepository.findByUserId(300L)).thenReturn(Optional.of(student));
        when(classroomRepository.findById(50L)).thenReturn(Optional.of(classroom));

        service.leaveClassroom(300L, 50L);

        assertTrue(classroom.getStudents().isEmpty());
    }

    @Test
    void leaveClassroom_notMember_throwsResourceNotFound() {
        when(studentProfileRepository.findByUserId(300L)).thenReturn(Optional.of(student));
        when(classroomRepository.findById(50L)).thenReturn(Optional.of(classroom));

        assertThrows(ResourceNotFoundException.class,
                () -> service.leaveClassroom(300L, 50L));
    }

    // ============================================================
    // CO-TEACHER + LAST TEACHER RULE (en kritik kurallar)
    // ============================================================

    @Test
    void joinAsCoTeacher_validCode_addsTeacher() {
        when(teacherProfileRepository.findByUserId(200L)).thenReturn(Optional.of(teacherB));
        when(classroomRepository.findByTeacherJoinCode("TCH222")).thenReturn(Optional.of(classroom));

        ClassroomDto result = service.joinAsCoTeacher(200L, "TCH222");

        assertEquals(50L, result.getId());
        assertTrue(classroom.getTeachers().contains(teacherB));
        assertEquals(2, result.getTeacherCount());
    }

    @Test
    void joinAsCoTeacher_inactiveClassroom_throwsBadRequest() {
        classroom.setIsActive(false);
        when(teacherProfileRepository.findByUserId(200L)).thenReturn(Optional.of(teacherB));
        when(classroomRepository.findByTeacherJoinCode("TCH222")).thenReturn(Optional.of(classroom));

        assertThrows(BadRequestException.class,
                () -> service.joinAsCoTeacher(200L, "TCH222"));
    }

    @Test
    void joinAsCoTeacher_isIdempotent() {
        classroom.getTeachers().add(teacherB);
        when(teacherProfileRepository.findByUserId(200L)).thenReturn(Optional.of(teacherB));
        when(classroomRepository.findByTeacherJoinCode("TCH222")).thenReturn(Optional.of(classroom));

        service.joinAsCoTeacher(200L, "TCH222");

        assertEquals(2, classroom.getTeachers().size());
    }

    @Test
    void removeTeacher_lastTeacherInActiveClassroom_throwsBadRequest() {
        // Sadece teacherA üye, sınıf aktif
        when(teacherProfileRepository.findByUserId(100L)).thenReturn(Optional.of(teacherA));
        when(classroomRepository.findById(50L)).thenReturn(Optional.of(classroom));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.removeTeacher(100L, 50L, 1L));

        assertTrue(ex.getMessage().contains("son öğretmen"));
        assertTrue(classroom.getTeachers().contains(teacherA));
    }

    @Test
    void removeTeacher_lastTeacherInInactiveClassroom_isAllowed() {
        // Sınıf inaktif → son öğretmen kuralı geçerli değil
        classroom.setIsActive(false);
        when(teacherProfileRepository.findByUserId(100L)).thenReturn(Optional.of(teacherA));
        when(classroomRepository.findById(50L)).thenReturn(Optional.of(classroom));

        service.removeTeacher(100L, 50L, 1L);

        assertTrue(classroom.getTeachers().isEmpty());
    }

    @Test
    void removeTeacher_oneOfTwo_isAllowed() {
        classroom.getTeachers().add(teacherB);
        when(teacherProfileRepository.findByUserId(100L)).thenReturn(Optional.of(teacherA));
        when(classroomRepository.findById(50L)).thenReturn(Optional.of(classroom));

        service.removeTeacher(100L, 50L, 2L);

        assertEquals(1, classroom.getTeachers().size());
        assertFalse(classroom.getTeachers().contains(teacherB));
    }

    @Test
    void removeTeacher_targetNotInClassroom_throwsResourceNotFound() {
        when(teacherProfileRepository.findByUserId(100L)).thenReturn(Optional.of(teacherA));
        when(classroomRepository.findById(50L)).thenReturn(Optional.of(classroom));

        assertThrows(ResourceNotFoundException.class,
                () -> service.removeTeacher(100L, 50L, 999L));
    }

    @Test
    void removeTeacher_callerNotMember_throwsUnauthorized() {
        // teacherB üye değil ama A'yı çıkarmaya çalışıyor
        when(teacherProfileRepository.findByUserId(200L)).thenReturn(Optional.of(teacherB));
        when(classroomRepository.findById(50L)).thenReturn(Optional.of(classroom));

        assertThrows(UnauthorizedOperationException.class,
                () -> service.removeTeacher(200L, 50L, 1L));
    }

    // ============================================================
    // REGENERATE CODES
    // ============================================================

    @Test
    void regenerateStudentJoinCode_replacesCode() {
        when(teacherProfileRepository.findByUserId(100L)).thenReturn(Optional.of(teacherA));
        when(classroomRepository.findById(50L)).thenReturn(Optional.of(classroom));
        when(inviteCodeGenerator.generateUnique(any())).thenReturn("NEWSTU");

        ClassroomDto result = service.regenerateStudentJoinCode(100L, 50L);

        assertEquals("NEWSTU", result.getStudentJoinCode());
        assertEquals("TCH222", result.getTeacherJoinCode()); // teacher kodu değişmemeli
    }

    @Test
    void regenerateTeacherJoinCode_replacesCode() {
        when(teacherProfileRepository.findByUserId(100L)).thenReturn(Optional.of(teacherA));
        when(classroomRepository.findById(50L)).thenReturn(Optional.of(classroom));
        when(inviteCodeGenerator.generateUnique(any())).thenReturn("NEWTCH");

        ClassroomDto result = service.regenerateTeacherJoinCode(100L, 50L);

        assertEquals("NEWTCH", result.getTeacherJoinCode());
        assertEquals("STU111", result.getStudentJoinCode()); // student kodu değişmemeli
    }

    // ============================================================
    // ACCOUNT DELETION GUARD
    // ============================================================

    @Test
    void findActiveMembershipsForTeacher_returnsClassroomsWithLastTeacherFlag() {
        Classroom soloClassroom = new Classroom();
        soloClassroom.setId(60L);
        soloClassroom.setName("Solo");
        soloClassroom.setTeachers(new HashSet<>(List.of(teacherA)));

        Classroom sharedClassroom = new Classroom();
        sharedClassroom.setId(61L);
        sharedClassroom.setName("Shared");
        sharedClassroom.setTeachers(new HashSet<>(List.of(teacherA, teacherB)));

        when(teacherProfileRepository.findByUserId(100L)).thenReturn(Optional.of(teacherA));
        when(classroomRepository.findActiveByTeacherProfileId(1L))
                .thenReturn(List.of(soloClassroom, sharedClassroom));

        List<ActiveClassroomMembershipDto> result =
                service.findActiveMembershipsForTeacher(100L);

        assertEquals(2, result.size());

        ActiveClassroomMembershipDto solo = result.stream()
                .filter(m -> m.getId().equals(60L)).findFirst().orElseThrow();
        ActiveClassroomMembershipDto shared = result.stream()
                .filter(m -> m.getId().equals(61L)).findFirst().orElseThrow();

        assertTrue(solo.getIsLastTeacher());
        assertFalse(shared.getIsLastTeacher());
    }

    @Test
    void findActiveMembershipsForTeacher_noTeacherProfile_returnsEmpty() {
        when(teacherProfileRepository.findByUserId(999L)).thenReturn(Optional.empty());

        List<ActiveClassroomMembershipDto> result =
                service.findActiveMembershipsForTeacher(999L);

        assertTrue(result.isEmpty());
    }
}
