package com.beratcan.app.service;

import com.beratcan.app.dto.response.StudentSummaryDto;
import com.beratcan.app.entity.ParentProfile;
import com.beratcan.app.entity.StudentProfile;
import com.beratcan.app.entity.User;
import com.beratcan.app.exception.BadRequestException;
import com.beratcan.app.exception.ResourceNotFoundException;
import com.beratcan.app.repository.ParentProfileRepository;
import com.beratcan.app.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParentLinkService {

    private final ParentProfileRepository parentProfileRepository;
    private final StudentProfileRepository studentProfileRepository;

    @Transactional(readOnly = true)
    public List<StudentSummaryDto> getChildren(Long parentUserId) {
        ParentProfile parent = loadParent(parentUserId);
        return parent.getChildren().stream()
                .map(this::toStudentSummary)
                .toList();
    }

    @Transactional
    public StudentSummaryDto linkChildByCode(Long parentUserId, String code) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Davet kodu boş olamaz");
        }
        String normalized = code.trim().toUpperCase();

        ParentProfile parent = loadParent(parentUserId);
        StudentProfile student = studentProfileRepository.findByParentInviteCode(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Geçersiz davet kodu"));

        if (parent.getChildren().add(student)) {
            log.info("Parent linked to student: parentUserId={}, studentProfileId={}",
                    parentUserId, student.getId());
        } else {
            log.info("Parent already linked to student (idempotent): parentUserId={}, studentProfileId={}",
                    parentUserId, student.getId());
        }

        return toStudentSummary(student);
    }

    @Transactional
    public void unlinkChild(Long parentUserId, Long studentProfileId) {
        ParentProfile parent = loadParent(parentUserId);
        boolean removed = parent.getChildren().removeIf(s -> s.getId().equals(studentProfileId));
        if (!removed) {
            throw new ResourceNotFoundException("Bu öğrenci hesabınıza bağlı değil");
        }
        log.info("Parent unlinked from student: parentUserId={}, studentProfileId={}",
                parentUserId, studentProfileId);
    }

    private ParentProfile loadParent(Long userId) {
        return parentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Veli profili bulunamadı"));
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
