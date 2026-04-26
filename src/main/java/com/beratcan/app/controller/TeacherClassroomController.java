package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.dto.request.CreateClassroomRequest;
import com.beratcan.app.dto.request.JoinAsCoTeacherRequest;
import com.beratcan.app.dto.request.UpdateClassroomRequest;
import com.beratcan.app.dto.response.ClassroomDetailDto;
import com.beratcan.app.dto.response.ClassroomDto;
import com.beratcan.app.service.ClassroomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/classrooms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class TeacherClassroomController {

    private final ClassroomService classroomService;

    @GetMapping
    public ResponseEntity<List<ClassroomDto>> listMyClassrooms(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(classroomService.listClassroomsForTeacher(userId));
    }

    @PostMapping
    public ResponseEntity<ClassroomDto> createClassroom(
            Authentication authentication,
            @Valid @RequestBody CreateClassroomRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(classroomService.createClassroom(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassroomDetailDto> getClassroomDetail(
            Authentication authentication,
            @PathVariable Long id) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(classroomService.getClassroomDetail(userId, id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ClassroomDto> updateClassroom(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateClassroomRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(classroomService.updateClassroom(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateClassroom(
            Authentication authentication,
            @PathVariable Long id) {
        Long userId = Long.parseLong(authentication.getName());
        classroomService.deactivateClassroom(userId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/students/{studentProfileId}")
    public ResponseEntity<Void> removeStudent(
            Authentication authentication,
            @PathVariable Long id,
            @PathVariable Long studentProfileId) {
        Long userId = Long.parseLong(authentication.getName());
        classroomService.removeStudent(userId, id, studentProfileId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/teachers/{teacherProfileId}")
    public ResponseEntity<Void> removeTeacher(
            Authentication authentication,
            @PathVariable Long id,
            @PathVariable Long teacherProfileId) {
        Long userId = Long.parseLong(authentication.getName());
        classroomService.removeTeacher(userId, id, teacherProfileId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/join")
    @RateLimit(requestsPerMinute = 10, keyType = "USER", message = "Çok fazla deneme. Daha sonra tekrar deneyin.")
    public ResponseEntity<ClassroomDto> joinAsCoTeacher(
            Authentication authentication,
            @Valid @RequestBody JoinAsCoTeacherRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(classroomService.joinAsCoTeacher(userId, request.getTeacherJoinCode()));
    }

    @PostMapping("/{id}/regenerate-student-code")
    public ResponseEntity<ClassroomDto> regenerateStudentCode(
            Authentication authentication,
            @PathVariable Long id) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(classroomService.regenerateStudentJoinCode(userId, id));
    }

    @PostMapping("/{id}/regenerate-teacher-code")
    public ResponseEntity<ClassroomDto> regenerateTeacherCode(
            Authentication authentication,
            @PathVariable Long id) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(classroomService.regenerateTeacherJoinCode(userId, id));
    }
}
