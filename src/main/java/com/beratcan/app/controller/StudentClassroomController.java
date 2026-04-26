package com.beratcan.app.controller;

import com.beratcan.app.annotation.RateLimit;
import com.beratcan.app.dto.request.JoinClassroomRequest;
import com.beratcan.app.dto.response.ClassroomSummaryDto;
import com.beratcan.app.service.ClassroomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/classrooms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentClassroomController {

    private final ClassroomService classroomService;

    @GetMapping
    public ResponseEntity<List<ClassroomSummaryDto>> listMyClassrooms(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(classroomService.listClassroomsForStudent(userId));
    }

    @PostMapping("/join")
    @RateLimit(requestsPerMinute = 10, keyType = "USER", message = "Çok fazla deneme. Daha sonra tekrar deneyin.")
    public ResponseEntity<ClassroomSummaryDto> joinClassroom(
            Authentication authentication,
            @Valid @RequestBody JoinClassroomRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(classroomService.joinAsStudent(userId, request.getStudentJoinCode()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> leaveClassroom(
            Authentication authentication,
            @PathVariable Long id) {
        Long userId = Long.parseLong(authentication.getName());
        classroomService.leaveClassroom(userId, id);
        return ResponseEntity.noContent().build();
    }
}
